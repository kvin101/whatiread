package com.whatiread.identity.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.whatiread.identity.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Probabilistic index of taken usernames. Negative lookups skip the database;
 * positive lookups are confirmed with {@link UserRepository#existsByUsernameIgnoreCase}.
 */
@Component
public class UsernameBloomFilterRegistry {

    private static final Logger log = LoggerFactory.getLogger(UsernameBloomFilterRegistry.class);
    private static final double FALSE_POSITIVE_RATE = 0.01;
    private static final int MIN_EXPECTED_INSERTIONS = 1_000;

    private final UserRepository userRepository;
    private volatile BloomFilter<String> filter = emptyFilter(MIN_EXPECTED_INSERTIONS);

    public UsernameBloomFilterRegistry(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private static BloomFilter<String> emptyFilter(int expectedInsertions) {
        return BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                FALSE_POSITIVE_RATE
        );
    }

    private static String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadFromDatabase() {
        rebuild();
    }

    public synchronized void rebuild() {
        List<String> usernames = userRepository.findAllUsernames();
        int expected = Math.max(usernames.size() * 2, MIN_EXPECTED_INSERTIONS);
        BloomFilter<String> rebuilt = emptyFilter(expected);
        for (String username : usernames) {
            rebuilt.put(key(username));
        }
        filter = rebuilt;
        log.info("Username bloom filter loaded with {} entries (expected capacity {})", usernames.size(), expected);
    }

    public void register(String username) {
        filter.put(key(username));
    }

    public boolean mightContain(String username) {
        return filter.mightContain(key(username));
    }
}
