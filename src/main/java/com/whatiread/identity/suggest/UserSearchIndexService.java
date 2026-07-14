package com.whatiread.identity.suggest;

import com.whatiread.identity.domain.User;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserSearchIndexService {

    private final MeilisearchUserIndexClient indexClient;

    public UserSearchIndexService(MeilisearchUserIndexClient indexClient) {
        this.indexClient = indexClient;
    }

    public void syncUser(User user) {
        if (!indexClient.isEnabled() || user == null || !user.isEnabled()) {
            if (user != null) {
                removeUser(user.getId());
            }
            return;
        }
        indexClient.upsertUsers(List.of(user));
    }

    public void syncUsers(List<User> users) {
        if (!indexClient.isEnabled() || users.isEmpty()) {
            return;
        }
        List<User> enabledUsers = users.stream().filter(User::isEnabled).toList();
        if (!enabledUsers.isEmpty()) {
            indexClient.upsertUsers(enabledUsers);
        }
    }

    public void removeUser(UUID userId) {
        if (!indexClient.isEnabled() || userId == null) {
            return;
        }
        indexClient.removeUsers(List.of(userId.toString()));
    }
}
