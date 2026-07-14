package com.whatiread.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.whatiread.identity.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsernameBloomFilterRegistryTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UsernameBloomFilterRegistry registry;

    @Test
    void rebuildLoadsExistingUsernames() {
        when(userRepository.findAllUsernames()).thenReturn(List.of("alice", "bob"));

        registry.rebuild();

        assertThat(registry.mightContain("alice")).isTrue();
        assertThat(registry.mightContain("bob")).isTrue();
        assertThat(registry.mightContain("carol")).isFalse();
    }

    @Test
    void registerAddsUsernameAfterCreation() {
        when(userRepository.findAllUsernames()).thenReturn(List.of());
        registry.rebuild();

        registry.register("new_user");

        assertThat(registry.mightContain("new_user")).isTrue();
        assertThat(registry.mightContain("NEW_USER")).isTrue();
    }
}
