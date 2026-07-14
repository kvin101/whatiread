package com.whatiread.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.whatiread.identity.api.UpdateProfileRequest;
import com.whatiread.identity.api.UserResponse;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.identity.suggest.UserSearchIndexService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceImplTest {


    private static final String NEW = "New";
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UsernameService usernameService;
    @Mock
    private UserSearchIndexService userSearchIndexService;
    @Mock
    private AvatarStorageService avatarStorageService;

    @InjectMocks
    private UserAccountServiceImpl userAccountService;

    private UUID userId;
    private User user;

    private static void setId(User user, UUID id) {
        Class<?> type = user.getClass();
        while (type != null) {
            try {
                var idField = type.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(user, id);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("No id on User");
    }

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User("reader@example.com", "reader", "hash", "Reader", "User");
        setId(user, userId);
    }

    @Test
    void updateProfileTrimsAndClearsBlankFields() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(new UserResponse(
                userId, user.getEmail(), user.getUsername(), NEW, "Name", null,
                null, null, null, null, null, null, null, null, false, null, true,
                java.time.Instant.parse("2024-01-01T00:00:00Z"), false));

        UserResponse response = userAccountService.updateProfile(
                userId, new UpdateProfileRequest(
                        " New ", " Name ", null, " ", " ", "  ", " ", " ", " ", " ", " ", null, "  ", false));

        assertThat(user.getFirstName()).isEqualTo(NEW);
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getAddressLine1()).isNull();
        assertThat(response.firstName()).isEqualTo(NEW);
        assertThat(user.isAcceptRecommendations()).isFalse();
    }

    @Test
    void updateProfileSetsAcceptRecommendations() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(new UserResponse(
                userId, user.getEmail(), user.getUsername(), "Reader", "User", null,
                null, null, null, null, null, null, null, null, false, null, false,
                java.time.Instant.parse("2024-01-01T00:00:00Z"), false));

        userAccountService.updateProfile(
                userId, new UpdateProfileRequest(
                        null, null, null, null, null, null, null, null, null, null, null, null, null, false));

        assertThat(user.isAcceptRecommendations()).isFalse();
    }
}
