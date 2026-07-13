package com.whatiread.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.whatiread.identity.api.UserResponse;
import com.whatiread.identity.domain.User;
import com.whatiread.instance.service.InstanceSettingsService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Mock
    private InstanceSettingsService instanceSettingsService;

    @InjectMocks
    private UserMapper userMapper;

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
        user = new User("admin@example.com", "hash", "Admin", "User");
        setId(user, userId);
    }

    @Test
    void toUserResponseMarksInstanceAdmin() {
        when(instanceSettingsService.isInstanceAdmin(userId)).thenReturn(true);

        UserResponse response = userMapper.toUserResponse(user);

        assertThat(response.admin()).isTrue();
    }

    @Test
    void toUserResponseMarksNonAdmin() {
        when(instanceSettingsService.isInstanceAdmin(userId)).thenReturn(false);

        UserResponse response = userMapper.toUserResponse(user);

        assertThat(response.admin()).isFalse();
    }
}
