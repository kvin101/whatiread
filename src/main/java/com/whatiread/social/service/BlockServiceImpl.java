package com.whatiread.social.service;

import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.shared.exception.ResourceNotFoundException;
import com.whatiread.social.api.BlockedUserDto;
import com.whatiread.social.domain.FriendRequestStatus;
import com.whatiread.social.domain.Friendship;
import com.whatiread.social.domain.UserBlock;
import com.whatiread.social.repository.FriendRequestRepository;
import com.whatiread.social.repository.FriendshipRepository;
import com.whatiread.social.repository.UserBlockRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BlockServiceImpl implements BlockService {

    private final UserBlockRepository userBlockRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;

    public BlockServiceImpl(
            UserBlockRepository userBlockRepository,
            FriendshipRepository friendshipRepository,
            FriendRequestRepository friendRequestRepository,
            UserRepository userRepository
    ) {
        this.userBlockRepository = userBlockRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void block(UUID blockerId, UUID blockedUserId) {
        if (blockerId.equals(blockedUserId)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedUserId)) {
            return;
        }
        if (friendshipRepository.existsByUser_IdAndFriend_Id(blockerId, blockedUserId)) {
            friendshipRepository.deleteById(new Friendship.FriendshipId(blockerId, blockedUserId));
            friendshipRepository.deleteById(new Friendship.FriendshipId(blockedUserId, blockerId));
        }
        cancelPendingRequests(blockerId, blockedUserId);
        userBlockRepository.save(new UserBlock(blockerId, blockedUserId));
    }

    private void cancelPendingRequests(UUID userA, UUID userB) {
        cancelPendingRequest(userA, userB);
        cancelPendingRequest(userB, userA);
    }

    private void cancelPendingRequest(UUID requesterId, UUID addresseeId) {
        friendRequestRepository.findByRequester_IdAndAddressee_Id(requesterId, addresseeId)
                .filter(request -> request.getStatus() == FriendRequestStatus.PENDING)
                .ifPresent(request -> {
                    request.setStatus(FriendRequestStatus.CANCELLED);
                    friendRequestRepository.save(request);
                });
    }

    @Override
    public void unblock(UUID blockerId, UUID blockedUserId) {
        if (!userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedUserId)) {
            throw new ResourceNotFoundException("Block not found");
        }
        userBlockRepository.deleteById(new UserBlock.UserBlockId(blockerId, blockedUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedEitherWay(UUID userId, UUID otherUserId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(userId, otherUserId)
                || userBlockRepository.existsByBlockerIdAndBlockedId(otherUserId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasBlocked(UUID blockerId, UUID blockedUserId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockedUserDto> listBlockedUsers(UUID blockerId) {
        List<UserBlock> blocks = userBlockRepository.findByBlockerIdOrderByCreatedAtDesc(blockerId);
        if (blocks.isEmpty()) {
            return List.of();
        }
        List<UUID> blockedIds = blocks.stream().map(UserBlock::getBlockedId).toList();
        Map<UUID, User> usersById = userRepository.findAllById(blockedIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return blocks.stream()
                .map(block -> {
                    User user = usersById.get(block.getBlockedId());
                    if (user == null) {
                        throw new ResourceNotFoundException("User not found");
                    }
                    return new BlockedUserDto(
                            user.getId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getDisplayName(),
                            user.getAvatarUrl(),
                            block.getCreatedAt()
                    );
                })
                .toList();
    }
}
