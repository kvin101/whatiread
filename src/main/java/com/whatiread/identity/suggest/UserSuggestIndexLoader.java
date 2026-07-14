package com.whatiread.identity.suggest;

import com.whatiread.config.MeilisearchProperties;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserSuggestIndexLoader {

    private static final Logger log = LoggerFactory.getLogger(UserSuggestIndexLoader.class);
    private static final int BATCH_SIZE = 250;

    private final MeilisearchProperties properties;
    private final MeilisearchUserIndexClient indexClient;
    private final UserRepository userRepository;

    public UserSuggestIndexLoader(
            MeilisearchProperties properties,
            MeilisearchUserIndexClient indexClient,
            UserRepository userRepository
    ) {
        this.properties = properties;
        this.indexClient = indexClient;
        this.userRepository = userRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadUsersIfNeeded() {
        if (!properties.enabled() || !properties.loadUsersOnStartup()) {
            return;
        }
        if (indexClient.documentCount() > 0) {
            log.info("Meilisearch index '{}' already has documents; skipping user load", properties.userIndex());
            return;
        }

        List<User> users = userRepository.findAll().stream().filter(User::isEnabled).toList();
        if (users.isEmpty()) {
            log.info("No enabled users to index for '{}'", properties.userIndex());
            return;
        }

        indexClient.ensureIndex();
        for (int start = 0; start < users.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, users.size());
            indexClient.upsertUsers(users.subList(start, end));
        }
        log.info("Loaded {} users into Meilisearch index '{}'", users.size(), properties.userIndex());
    }
}
