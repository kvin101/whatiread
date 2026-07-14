package com.whatiread.social.service;

import com.whatiread.config.CacheConfig;
import com.whatiread.config.BusinessMetrics;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.shared.exception.ConflictException;
import com.whatiread.shared.exception.ResourceNotFoundException;
import com.whatiread.social.api.BlockedUserDto;
import com.whatiread.social.api.CreateFriendRequestRequest;
import com.whatiread.social.api.FriendRequestDto;
import com.whatiread.social.api.FriendSummaryDto;
import com.whatiread.social.api.FriendUserDto;
import com.whatiread.social.domain.FriendRequest;
import com.whatiread.social.domain.FriendRequestStatus;
import com.whatiread.social.domain.Friendship;
import com.whatiread.social.repository.FriendRequestRepository;
import com.whatiread.social.repository.FriendshipRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FriendServiceImpl implements FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserLookupService userLookupService;
    private final BlockService blockService;
    private final BusinessMetrics businessMetrics;
    private final SimpMessagingTemplate messagingTemplate;
    private final CacheManager cacheManager;

    public FriendServiceImpl(
            FriendRequestRepository friendRequestRepository,
            FriendshipRepository friendshipRepository,
            UserLookupService userLookupService,
            BlockService blockService,
            BusinessMetrics businessMetrics,
            SimpMessagingTemplate messagingTemplate,
            CacheManager cacheManager
    ) {
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.userLookupService = userLookupService;
        this.blockService = blockService;
        this.businessMetrics = businessMetrics;
        this.messagingTemplate = messagingTemplate;
        this.cacheManager = cacheManager;
    }

    @Override
    public FriendRequestDto sendRequest(UUID requesterId, CreateFriendRequestRequest request) {
        User requester = userLookupService.getPersistenceReference(requesterId);
        User addressee = resolveTargetUser(request);

        if (requester.getId().equals(addressee.getId())) {
            throw new IllegalArgumentException("Cannot send a friend request to yourself");
        }
        if (blockService.isBlockedEitherWay(requesterId, addressee.getId())) {
            throw new ConflictException("Cannot send a friend request to this user");
        }
        if (friendshipRepository.existsByUser_IdAndFriend_Id(requesterId, addressee.getId())) {
            throw new ConflictException("You are already friends");
        }

        friendRequestRepository.findByRequester_IdAndAddressee_Id(addressee.getId(), requesterId)
                .filter(existing -> existing.getStatus() == FriendRequestStatus.PENDING)
                .ifPresent(existing -> {
                    throw new ConflictException("This user already sent you a friend request");
                });

        FriendRequest friendRequest = friendRequestRepository
                .findByRequester_IdAndAddressee_Id(requesterId, addressee.getId())
                .map(existing -> reopenOrReject(existing))
                .orElseGet(() -> new FriendRequest(requester, addressee, FriendRequestStatus.PENDING));

        FriendRequestDto saved = toRequestDto(friendRequestRepository.save(friendRequest));
        messagingTemplate.convertAndSendToUser(
                addressee.getId().toString(),
                "/queue/friends",
                saved
        );
        businessMetrics.recordFriendRequestSent();
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendRequestDto> listIncoming(UUID userId) {
        return friendRequestRepository.findByAddressee_IdAndStatus(userId, FriendRequestStatus.PENDING).stream()
                .map(this::toRequestDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendRequestDto> listOutgoing(UUID userId) {
        return friendRequestRepository.findByRequester_IdAndStatus(userId, FriendRequestStatus.PENDING).stream()
                .map(this::toRequestDto)
                .toList();
    }

    @Override
    public FriendRequestDto accept(UUID userId, UUID requestId) {
        FriendRequest request = friendRequestRepository.findByIdAndAddressee_Id(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ConflictException("Friend request is not pending");
        }
        if (blockService.isBlockedEitherWay(userId, request.getRequester().getId())) {
            throw new ConflictException("Cannot accept this friend request");
        }

        request.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequestRepository.save(request);

        User requester = request.getRequester();
        User addressee = request.getAddressee();
        if (!friendshipRepository.existsByUser_IdAndFriend_Id(requester.getId(), addressee.getId())) {
            friendshipRepository.save(new Friendship(requester, addressee));
            friendshipRepository.save(new Friendship(addressee, requester));
        }
        evictFriendIdsCache(requester.getId(), addressee.getId());

        businessMetrics.recordFriendRequestAccepted();
        return toRequestDto(request);
    }

    @Override
    public FriendRequestDto decline(UUID userId, UUID requestId) {
        FriendRequest request = friendRequestRepository.findByIdAndAddressee_Id(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ConflictException("Friend request is not pending");
        }
        request.setStatus(FriendRequestStatus.REJECTED);
        FriendRequestDto saved = toRequestDto(friendRequestRepository.save(request));
        businessMetrics.recordFriendRequestDeclined();
        return saved;
    }

    @Override
    public void cancelRequest(UUID userId, UUID requestId) {
        FriendRequest request = friendRequestRepository.findByIdAndRequester_Id(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ConflictException("Only pending requests can be cancelled");
        }
        request.setStatus(FriendRequestStatus.CANCELLED);
        friendRequestRepository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendSummaryDto> listFriends(UUID userId) {
        return friendshipRepository.findByUser_Id(userId).stream()
                .map(friendship -> toFriendSummary(friendship.getFriend(), friendship.getCreatedAt()))
                .toList();
    }

    @Override
    public void unfriend(UUID userId, UUID friendUserId) {
        if (userId.equals(friendUserId)) {
            throw new IllegalArgumentException("Cannot unfriend yourself");
        }
        if (!friendshipRepository.existsByUser_IdAndFriend_Id(userId, friendUserId)) {
            throw new ResourceNotFoundException("Friendship not found");
        }
        friendshipRepository.deleteById(new Friendship.FriendshipId(userId, friendUserId));
        friendshipRepository.deleteById(new Friendship.FriendshipId(friendUserId, userId));
        evictFriendIdsCache(userId, friendUserId);
    }

    @Override
    public void block(UUID userId, UUID blockedUserId) {
        blockService.block(userId, blockedUserId);
        evictFriendIdsCache(userId, blockedUserId);
    }

    @Override
    public void unblock(UUID userId, UUID blockedUserId) {
        blockService.unblock(userId, blockedUserId);
        evictFriendIdsCache(userId, blockedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockedUserDto> listBlocked(UUID userId) {
        return blockService.listBlockedUsers(userId);
    }

    private void evictFriendIdsCache(UUID... userIds) {
        var cache = cacheManager.getCache(CacheConfig.FRIEND_IDS);
        if (cache == null) {
            return;
        }
        for (UUID userId : userIds) {
            cache.evict(userId);
        }
    }

    private User resolveTargetUser(CreateFriendRequestRequest request) {
        if (request.userId() != null) {
            return userLookupService.getPersistenceReference(request.userId());
        }
        return userLookupService.getByEmail(request.email());
    }

    private FriendRequest reopenOrReject(FriendRequest existing) {
        return switch (existing.getStatus()) {
            case PENDING -> throw new ConflictException("Friend request already sent");
            case ACCEPTED -> throw new ConflictException("You are already friends");
            case REJECTED, CANCELLED -> {
                existing.setStatus(FriendRequestStatus.PENDING);
                yield existing;
            }
        };
    }

    private FriendRequestDto toRequestDto(FriendRequest request) {
        return new FriendRequestDto(
                request.getId(),
                toFriendUserDto(request.getRequester()),
                toFriendUserDto(request.getAddressee()),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private FriendUserDto toFriendUserDto(User user) {
        return new FriendUserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getDisplayName(),
                user.getAvatarUrl()
        );
    }

    private FriendSummaryDto toFriendSummary(User user, java.time.Instant friendsSince) {
        return new FriendSummaryDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                friendsSince
        );
    }
}
