package com.whatiread.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.whatiread.identity.api.UserSummaryDto;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.shared.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceImplTest {


    private static final String ADA_EXAMPLE_COM = "ada@example.com";
    private static final String LOVELACE = "Lovelace";
    private static final String ADA = "Ada";
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserLookupServiceImpl userLookupService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User(ADA_EXAMPLE_COM, "ada", "hash", ADA, LOVELACE);
    }

    @Test
    void requireExistsThrowsWhenMissing() {
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> userLookupService.requireExists(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void getSummaryMapsUserFields() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserSummaryDto summary = userLookupService.getSummary(userId);

        assertThat(summary.id()).isEqualTo(user.getId());
        assertThat(summary.username()).isEqualTo("ada");
        assertThat(summary.displayName()).isEqualTo("Ada Lovelace");
        assertThat(summary.firstName()).isEqualTo(ADA);
        assertThat(summary.lastName()).isEqualTo(LOVELACE);
    }

    @Test
    void getByEmailTrimsAndLooksUpCaseInsensitively() {
        when(userRepository.findByEmailIgnoreCase(ADA_EXAMPLE_COM)).thenReturn(Optional.of(user));

        assertThat(userLookupService.getByEmail("  ada@example.com  ")).isSameAs(user);
    }

    @Test
    void countUsersDelegatesToRepository() {
        when(userRepository.count()).thenReturn(42L);

        assertThat(userLookupService.countUsers()).isEqualTo(42L);
    }

    @Test
    void existsByIdDelegatesToRepository() {
        when(userRepository.existsById(userId)).thenReturn(true);

        assertThat(userLookupService.existsById(userId)).isTrue();
    }
}
