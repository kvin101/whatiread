package com.whatiread.social.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.shared.exception.ResourceNotFoundException;
import com.whatiread.social.api.BlockedUserDto;
import com.whatiread.social.domain.FriendRequest;
import com.whatiread.social.domain.FriendRequestStatus;
import com.whatiread.social.domain.UserBlock;
import com.whatiread.social.repository.FriendRequestRepository;
import com.whatiread.social.repository.UserBlockRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockServiceImplTest {


    private static final String V_2024_01_15T10_00_00Z = "2024-01-15T10:00:00Z";
    private static final String ADA = "Ada";
    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BlockServiceImpl blockService;

    private UUID blockerId;
    private UUID blockedId;

    private static User user(UUID id) {
        User user = new User();
        setEntityId(user, id);
        return user;
    }

    private static void setEntityId(User user, UUID id) {
        try {
            Field idField = user.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void setCreatedAt(UserBlock block, Instant createdAt) throws Exception {
        Field createdAtField = UserBlock.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(block, createdAt);
    }

    @BeforeEach
    void setUp() {
        blockerId = UUID.randomUUID();
        blockedId = UUID.randomUUID();
    }

    @Test
    void blockRejectsSelfBlock() {
        assertThatThrownBy(() -> blockService.block(blockerId, blockerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot block yourself");
    }

    @Test
    void blockIsIdempotentWhenAlreadyBlocked() {
        when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(true);

        blockService.block(blockerId, blockedId);

        verify(userBlockRepository, never()).save(any());
    }

    @Test
    void blockCancelsPendingRequestsWithoutRemovingFriendship() {
        User requester = user(blockerId);
        User addressee = user(blockedId);
        FriendRequest outgoing = new FriendRequest(requester, addressee, FriendRequestStatus.PENDING);
        FriendRequest incoming = new FriendRequest(addressee, requester, FriendRequestStatus.PENDING);

        when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(false);
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(blockerId, blockedId))
                .thenReturn(Optional.of(outgoing));
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(blockedId, blockerId))
                .thenReturn(Optional.of(incoming));

        blockService.block(blockerId, blockedId);

        assertThat(outgoing.getStatus()).isEqualTo(FriendRequestStatus.CANCELLED);
        assertThat(incoming.getStatus()).isEqualTo(FriendRequestStatus.CANCELLED);
        verify(friendRequestRepository).save(outgoing);
        verify(friendRequestRepository).save(incoming);

        ArgumentCaptor<UserBlock> saved = ArgumentCaptor.forClass(UserBlock.class);
        verify(userBlockRepository).save(saved.capture());
        assertThat(saved.getValue().getBlockerId()).isEqualTo(blockerId);
        assertThat(saved.getValue().getBlockedId()).isEqualTo(blockedId);
    }

    @Test
    void unblockRequiresExistingBlock() {
        when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(false);

        assertThatThrownBy(() -> blockService.unblock(blockerId, blockedId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Block not found");
    }

    @Test
    void unblockDeletesBlockRecord() {
        when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(true);

        blockService.unblock(blockerId, blockedId);

        verify(userBlockRepository).deleteById(new UserBlock.UserBlockId(blockerId, blockedId));
    }

    @Test
    void isBlockedEitherWayChecksBothDirections() {
        when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(false);
        when(userBlockRepository.existsByBlockerIdAndBlockedId(blockedId, blockerId)).thenReturn(true);

        assertThat(blockService.isBlockedEitherWay(blockerId, blockedId)).isTrue();
    }

    @Test
    void listBlockedUsersMapsUserDetails() throws Exception {
        User blockedUser = user(blockedId);
        blockedUser.setFirstName(ADA);
        blockedUser.setLastName("Lovelace");

        UserBlock block = new UserBlock(blockerId, blockedId);
        setCreatedAt(block, Instant.parse(V_2024_01_15T10_00_00Z));

        when(userBlockRepository.findByBlockerIdOrderByCreatedAtDesc(blockerId)).thenReturn(List.of(block));
        when(userRepository.findAllById(List.of(blockedId))).thenReturn(List.of(blockedUser));

        List<BlockedUserDto> blockedUsers = blockService.listBlockedUsers(blockerId);

        assertThat(blockedUsers).hasSize(1);
        assertThat(blockedUsers.getFirst().id()).isEqualTo(blockedId);
        assertThat(blockedUsers.getFirst().firstName()).isEqualTo(ADA);
        assertThat(blockedUsers.getFirst().blockedAt()).isEqualTo(Instant.parse(V_2024_01_15T10_00_00Z));
    }

    @Test
    void blockWithoutFriendshipOrRequestsOnlyPersistsBlock() {
        when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(false);
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(blockerId, blockedId))
                .thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequester_IdAndAddressee_Id(blockedId, blockerId))
                .thenReturn(Optional.empty());

        blockService.block(blockerId, blockedId);

        verify(userBlockRepository).save(any(UserBlock.class));
        verify(friendRequestRepository, never()).save(any());
    }

    @Test
    void isBlockedEitherWayReturnsFalseWhenNotBlocked() {
        when(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)).thenReturn(false);
        when(userBlockRepository.existsByBlockerIdAndBlockedId(blockedId, blockerId)).thenReturn(false);

        assertThat(blockService.isBlockedEitherWay(blockerId, blockedId)).isFalse();
    }
}
