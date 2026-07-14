package com.whatiread.identity.suggest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.whatiread.social.domain.FriendRequestStatus;
import com.whatiread.social.repository.FriendRequestRepository;
import com.whatiread.social.repository.FriendshipRepository;
import com.whatiread.social.repository.UserBlockRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSuggestServiceTest {

    @Mock
    private MeilisearchUserIndexClient indexClient;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @InjectMocks
    private UserSuggestService userSuggestService;

    @Test
    void suggestExcludesViewer() {
        UUID viewerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        when(indexClient.search("vams", 120)).thenReturn(List.of(
                new MeilisearchUserIndexClient.IndexedUserHit(
                        viewerId.toString(), "viewer", "Viewer", "viewer@example.com"),
                new MeilisearchUserIndexClient.IndexedUserHit(
                        otherId.toString(), "vamsi", "vamsi k", "vamsi@example.com")
        ));
        when(friendshipRepository.findFriendIdsByUserId(viewerId)).thenReturn(List.of());
        when(friendRequestRepository.findByAddressee_IdAndStatus(viewerId, FriendRequestStatus.PENDING))
                .thenReturn(List.of());
        when(friendRequestRepository.findByRequester_IdAndStatus(viewerId, FriendRequestStatus.PENDING))
                .thenReturn(List.of());
        when(userBlockRepository.findByBlockerIdOrderByCreatedAtDesc(viewerId)).thenReturn(List.of());
        when(userBlockRepository.findBlockerIdsByBlockedId(viewerId)).thenReturn(List.of());

        List<UserSuggestDto> results = userSuggestService.suggest(viewerId, "vams", UserSuggestScope.INVITE, 5);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().username()).isEqualTo("vamsi");
    }
}
