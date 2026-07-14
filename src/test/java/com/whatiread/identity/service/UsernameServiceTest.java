package com.whatiread.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.whatiread.identity.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsernameServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UsernameBloomFilterRegistry bloomFilterRegistry;

    @InjectMocks
    private UsernameService usernameService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void checkAvailabilityReturnsInvalidForShortUsername() {
        var response = usernameService.checkAvailability("ab", null);

        assertThat(response.valid()).isFalse();
        assertThat(response.available()).isFalse();
        assertThat(response.message()).contains("3");
    }

    @Test
    void checkAvailabilitySkipsDatabaseWhenBloomFilterNegative() {
        when(bloomFilterRegistry.mightContain("fresh_user")).thenReturn(false);

        var response = usernameService.checkAvailability("Fresh_User", null);

        assertThat(response.username()).isEqualTo("fresh_user");
        assertThat(response.valid()).isTrue();
        assertThat(response.available()).isTrue();
        assertThat(response.message()).isNull();
    }

    @Test
    void checkAvailabilityConfirmsBloomFilterPositiveWithDatabase() {
        when(bloomFilterRegistry.mightContain("taken_user")).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCase("taken_user")).thenReturn(true);

        var response = usernameService.checkAvailability("taken_user", null);

        assertThat(response.valid()).isTrue();
        assertThat(response.available()).isFalse();
        assertThat(response.message()).isEqualTo("Username already taken");
    }

    @Test
    void checkAvailabilityTreatsBloomFalsePositiveAsAvailable() {
        when(bloomFilterRegistry.mightContain("almost_taken")).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCase("almost_taken")).thenReturn(false);

        var response = usernameService.checkAvailability("almost_taken", null);

        assertThat(response.valid()).isTrue();
        assertThat(response.available()).isTrue();
    }

    @Test
    void checkAvailabilityAllowsKeepingCurrentUsername() {
        var user = new com.whatiread.identity.domain.User(
                "reader@example.com", "reader", "hash", "Reader", "User");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        var response = usernameService.checkAvailability("reader", userId);

        assertThat(response.valid()).isTrue();
        assertThat(response.available()).isTrue();
    }
}
