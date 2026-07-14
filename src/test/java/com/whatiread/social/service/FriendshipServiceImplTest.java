package com.whatiread.social.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.social.repository.FriendshipRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceImplTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @InjectMocks
    private FriendshipServiceImpl friendshipService;

    @Test
    void listFriendIdsDelegatesToRepository() {
        UUID userId = UUID.randomUUID();
        List<UUID> friendIds = List.of(UUID.randomUUID());
        when(friendshipRepository.findFriendIdsByUserId(userId)).thenReturn(friendIds);

        assertThat(friendshipService.listFriendIds(userId)).isEqualTo(friendIds);
        verify(friendshipRepository).findFriendIdsByUserId(userId);
    }

    @Test
    void areFriendsReturnsFalseForSameUser() {
        UUID userId = UUID.randomUUID();
        assertThat(friendshipService.areFriends(userId, userId)).isFalse();
    }
}
