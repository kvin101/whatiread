package com.whatiread.social.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.config.BusinessMetrics;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.service.UserLookupService;
import com.whatiread.shared.exception.ConflictException;
import com.whatiread.social.api.CreateFriendRequestRequest;
import com.whatiread.social.domain.FriendRequest;
import com.whatiread.social.domain.FriendRequestStatus;
import com.whatiread.social.repository.FriendRequestRepository;
import com.whatiread.social.repository.FriendshipRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class FriendServiceImplTest {


    private static final String BOB_EXAMPLE_COM = "bob@example.com";
    private static final String HASH = "hash";
    @Mock
    private FriendRequestRepository friendRequestRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private BlockService blockService;
    @Mock
    private BusinessMetrics businessMetrics;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private FriendServiceImpl friendService;

    private UUID requesterId;
    private UUID addresseeId;
    private User requester;
    private User addressee;

    private static void setId(Object entity, UUID id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                Field idField = type.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, id);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("No id on " + entity.getClass());
    }

    @BeforeEach
    void setUp() {
        requesterId = UUID.randomUUID();
        addresseeId = UUID.randomUUID();
        requester = new User("a@example.com", "alice", HASH, "Alice", "A");
        addressee = new User("b@example.com", "bob", HASH, "Bob", "B");
        setId(requester, requesterId);
        setId(addressee, addresseeId);
    }

    @Test
    void sendRequestRejectsSelfRequest() {
        when(userLookupService.getPersistenceReference(requesterId)).thenReturn(requester);

        assertThatThrownBy(() -> friendService.sendRequest(
                requesterId, new CreateFriendRequestRequest(requesterId, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendRequestRejectsWhenBlocked() {
        when(userLookupService.getPersistenceReference(requesterId)).thenReturn(requester);
        when(userLookupService.getPersistenceReference(addresseeId)).thenReturn(addressee);
        when(blockService.isBlockedEitherWay(requesterId, addresseeId)).thenReturn(true);

        assertThatThrownBy(() -> friendService.sendRequest(
                requesterId, new CreateFriendRequestRequest(addresseeId, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void sendRequestCreatesPendingRequest() {
        when(userLookupService.getPersistenceReference(requesterId)).thenReturn(requester);
        when(userLookupService.getPersistenceReference(addresseeId)).thenReturn(addressee);
        when(blockService.isBlockedEitherWay(requesterId, addresseeId)).thenReturn(false);
        when(friendshipRepository.existsByUser_IdAndFriend_Id(requesterId, addresseeId)).thenReturn(false);
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(addresseeId, requesterId))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(requesterId, addresseeId))
                .thenReturn(Optional.empty());
        FriendRequest saved = new FriendRequest(requester, addressee, FriendRequestStatus.PENDING);
        setId(saved, UUID.randomUUID());
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(saved);

        var dto = friendService.sendRequest(requesterId, new CreateFriendRequestRequest(addresseeId, null));

        assertThat(dto.status()).isEqualTo(FriendRequestStatus.PENDING);
        verify(messagingTemplate).convertAndSendToUser(
                eq(addresseeId.toString()),
                eq("/queue/friends"),
                any()
        );
        verify(businessMetrics).recordFriendRequestSent();
    }

    @Test
    void acceptCreatesBidirectionalFriendship() {
        UUID requestId = UUID.randomUUID();
        FriendRequest request = new FriendRequest(requester, addressee, FriendRequestStatus.PENDING);
        setId(request, requestId);
        when(friendRequestRepository.findByIdAndAddressee_Id(requestId, addresseeId))
                .thenReturn(Optional.of(request));
        when(blockService.isBlockedEitherWay(addresseeId, requesterId)).thenReturn(false);
        when(friendshipRepository.existsByUser_IdAndFriend_Id(requesterId, addresseeId)).thenReturn(false);

        friendService.accept(addresseeId, requestId);

        assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);
        verify(friendshipRepository, times(2)).save(any());
        verify(businessMetrics).recordFriendRequestAccepted();
    }

    @Test
    void listIncomingMapsPendingRequests() {
        FriendRequest request = new FriendRequest(requester, addressee, FriendRequestStatus.PENDING);
        setId(request, UUID.randomUUID());
        when(friendRequestRepository.findByAddressee_IdAndStatus(addresseeId, FriendRequestStatus.PENDING))
                .thenReturn(List.of(request));

        assertThat(friendService.listIncoming(addresseeId)).hasSize(1);
    }

    @Test
    void declineMarksRequestRejected() {
        UUID requestId = UUID.randomUUID();
        FriendRequest request = new FriendRequest(requester, addressee, FriendRequestStatus.PENDING);
        setId(request, requestId);
        when(friendRequestRepository.findByIdAndAddressee_Id(requestId, addresseeId))
                .thenReturn(Optional.of(request));
        when(friendRequestRepository.save(request)).thenReturn(request);

        var dto = friendService.decline(addresseeId, requestId);

        assertThat(dto.status()).isEqualTo(FriendRequestStatus.REJECTED);
        verify(businessMetrics).recordFriendRequestDeclined();
    }

    @Test
    void cancelRequestMarksCancelled() {
        UUID requestId = UUID.randomUUID();
        FriendRequest request = new FriendRequest(requester, addressee, FriendRequestStatus.PENDING);
        setId(request, requestId);
        when(friendRequestRepository.findByIdAndRequester_Id(requestId, requesterId))
                .thenReturn(Optional.of(request));

        friendService.cancelRequest(requesterId, requestId);

        assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.CANCELLED);
    }

    @Test
    void unfriendRemovesBidirectionalLinks() {
        when(friendshipRepository.existsByUser_IdAndFriend_Id(requesterId, addresseeId)).thenReturn(true);

        friendService.unfriend(requesterId, addresseeId);

        verify(friendshipRepository, times(2)).deleteById(any());
    }

    @Test
    void listFriendsMapsSummaries() {
        com.whatiread.social.domain.Friendship friendship =
                new com.whatiread.social.domain.Friendship(requester, addressee);
        when(friendshipRepository.findByUser_Id(requesterId)).thenReturn(List.of(friendship));

        assertThat(friendService.listFriends(requesterId)).hasSize(1);
    }

    @Test
    void listOutgoingReturnsPendingRequests() {
        FriendRequest request = new FriendRequest(requester, addressee, FriendRequestStatus.PENDING);
        setId(request, UUID.randomUUID());
        when(friendRequestRepository.findByRequester_IdAndStatus(requesterId, FriendRequestStatus.PENDING))
                .thenReturn(List.of(request));

        assertThat(friendService.listOutgoing(requesterId)).hasSize(1);
    }

    @Test
    void sendRequestRejectsAlreadyFriends() {
        when(userLookupService.getPersistenceReference(requesterId)).thenReturn(requester);
        when(userLookupService.getPersistenceReference(addresseeId)).thenReturn(addressee);
        when(blockService.isBlockedEitherWay(requesterId, addresseeId)).thenReturn(false);
        when(friendshipRepository.existsByUser_IdAndFriend_Id(requesterId, addresseeId)).thenReturn(true);

        assertThatThrownBy(() -> friendService.sendRequest(
                requesterId, new CreateFriendRequestRequest(addresseeId, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void sendRequestReopensCancelledRequest() {
        FriendRequest existing = new FriendRequest(requester, addressee, FriendRequestStatus.CANCELLED);
        setId(existing, UUID.randomUUID());
        when(userLookupService.getPersistenceReference(requesterId)).thenReturn(requester);
        when(userLookupService.getPersistenceReference(addresseeId)).thenReturn(addressee);
        when(blockService.isBlockedEitherWay(requesterId, addresseeId)).thenReturn(false);
        when(friendshipRepository.existsByUser_IdAndFriend_Id(requesterId, addresseeId)).thenReturn(false);
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(addresseeId, requesterId))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(requesterId, addresseeId))
                .thenReturn(Optional.of(existing));
        when(friendRequestRepository.save(existing)).thenReturn(existing);

        var dto = friendService.sendRequest(requesterId, new CreateFriendRequestRequest(addresseeId, null));

        assertThat(dto.status()).isEqualTo(FriendRequestStatus.PENDING);
    }

    @Test
    void blockDelegatesToBlockService() {
        friendService.block(requesterId, addresseeId);
        verify(blockService).block(requesterId, addresseeId);
    }

    @Test
    void acceptRejectsNonPendingRequest() {
        UUID requestId = UUID.randomUUID();
        FriendRequest request = new FriendRequest(requester, addressee, FriendRequestStatus.ACCEPTED);
        setId(request, requestId);
        when(friendRequestRepository.findByIdAndAddressee_Id(requestId, addresseeId))
                .thenReturn(Optional.of(request));

        assertThatThrownBy(() -> friendService.accept(addresseeId, requestId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unfriendThrowsWhenNotFriends() {
        when(friendshipRepository.existsByUser_IdAndFriend_Id(requesterId, addresseeId)).thenReturn(false);

        assertThatThrownBy(() -> friendService.unfriend(requesterId, addresseeId))
                .isInstanceOf(com.whatiread.shared.exception.ResourceNotFoundException.class);
    }

    @Test
    void unfriendRejectsSelf() {
        assertThatThrownBy(() -> friendService.unfriend(requesterId, requesterId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sendRequestByEmailResolvesTargetUser() {
        when(userLookupService.getPersistenceReference(requesterId)).thenReturn(requester);
        when(userLookupService.getByEmail(BOB_EXAMPLE_COM)).thenReturn(addressee);
        when(blockService.isBlockedEitherWay(requesterId, addresseeId)).thenReturn(false);
        when(friendshipRepository.existsByUser_IdAndFriend_Id(requesterId, addresseeId)).thenReturn(false);
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(addresseeId, requesterId))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(requesterId, addresseeId))
                .thenReturn(Optional.empty());
        FriendRequest saved = new FriendRequest(requester, addressee, FriendRequestStatus.PENDING);
        setId(saved, UUID.randomUUID());
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(saved);

        var dto = friendService.sendRequest(requesterId, new CreateFriendRequestRequest(null, BOB_EXAMPLE_COM));

        assertThat(dto.status()).isEqualTo(FriendRequestStatus.PENDING);
    }

    @Test
    void sendRequestRejectsWhenReversePendingExists() {
        FriendRequest incoming = new FriendRequest(addressee, requester, FriendRequestStatus.PENDING);
        when(userLookupService.getPersistenceReference(requesterId)).thenReturn(requester);
        when(userLookupService.getPersistenceReference(addresseeId)).thenReturn(addressee);
        when(blockService.isBlockedEitherWay(requesterId, addresseeId)).thenReturn(false);
        when(friendshipRepository.existsByUser_IdAndFriend_Id(requesterId, addresseeId)).thenReturn(false);
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(addresseeId, requesterId))
                .thenReturn(Optional.of(incoming));

        assertThatThrownBy(() -> friendService.sendRequest(
                requesterId, new CreateFriendRequestRequest(addresseeId, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void sendRequestReopensRejectedRequest() {
        FriendRequest rejected = new FriendRequest(requester, addressee, FriendRequestStatus.REJECTED);
        setId(rejected, UUID.randomUUID());
        when(userLookupService.getPersistenceReference(requesterId)).thenReturn(requester);
        when(userLookupService.getPersistenceReference(addresseeId)).thenReturn(addressee);
        when(blockService.isBlockedEitherWay(requesterId, addresseeId)).thenReturn(false);
        when(friendshipRepository.existsByUser_IdAndFriend_Id(requesterId, addresseeId)).thenReturn(false);
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(addresseeId, requesterId))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(requesterId, addresseeId))
                .thenReturn(Optional.of(rejected));
        when(friendRequestRepository.save(rejected)).thenReturn(rejected);

        var dto = friendService.sendRequest(requesterId, new CreateFriendRequestRequest(addresseeId, null));

        assertThat(dto.status()).isEqualTo(FriendRequestStatus.PENDING);
    }

    @Test
    void sendRequestRejectsDuplicatePendingRequest() {
        FriendRequest pending = new FriendRequest(requester, addressee, FriendRequestStatus.PENDING);
        when(userLookupService.getPersistenceReference(requesterId)).thenReturn(requester);
        when(userLookupService.getPersistenceReference(addresseeId)).thenReturn(addressee);
        when(blockService.isBlockedEitherWay(requesterId, addresseeId)).thenReturn(false);
        when(friendshipRepository.existsByUser_IdAndFriend_Id(requesterId, addresseeId)).thenReturn(false);
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(addresseeId, requesterId))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(requesterId, addresseeId))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> friendService.sendRequest(
                requesterId, new CreateFriendRequestRequest(addresseeId, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void acceptRejectsWhenBlocked() {
        UUID requestId = UUID.randomUUID();
        FriendRequest request = new FriendRequest(requester, addressee, FriendRequestStatus.PENDING);
        setId(request, requestId);
        when(friendRequestRepository.findByIdAndAddressee_Id(requestId, addresseeId))
                .thenReturn(Optional.of(request));
        when(blockService.isBlockedEitherWay(addresseeId, requesterId)).thenReturn(true);

        assertThatThrownBy(() -> friendService.accept(addresseeId, requestId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancelRequestRejectsNonPending() {
        UUID requestId = UUID.randomUUID();
        FriendRequest request = new FriendRequest(requester, addressee, FriendRequestStatus.CANCELLED);
        setId(request, requestId);
        when(friendRequestRepository.findByIdAndRequester_Id(requestId, requesterId))
                .thenReturn(Optional.of(request));

        assertThatThrownBy(() -> friendService.cancelRequest(requesterId, requestId))
                .isInstanceOf(ConflictException.class);
    }
}
