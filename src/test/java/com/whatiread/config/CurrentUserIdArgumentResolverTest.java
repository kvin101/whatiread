package com.whatiread.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.whatiread.identity.domain.User;
import com.whatiread.identity.security.AuthenticatedUser;
import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.shared.exception.UnauthorizedException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.NativeWebRequest;

class CurrentUserIdArgumentResolverTest {

    private final CurrentUserIdArgumentResolver resolver = new CurrentUserIdArgumentResolver();

    private static MethodParameter currentUserIdParameter() throws NoSuchMethodException {
        Method method = Controller.class.getDeclaredMethod("handle", UUID.class);
        return new MethodParameter(method, 0);
    }

    private static MethodParameter otherParameter() throws NoSuchMethodException {
        Method method = Controller.class.getDeclaredMethod("other", String.class);
        return new MethodParameter(method, 0);
    }

    private static void setUserId(User user, UUID id) throws Exception {
        Field idField = User.class.getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void supportsCurrentUserIdUuidParameters() throws Exception {
        assertThat(resolver.supportsParameter(currentUserIdParameter())).isTrue();
        assertThat(resolver.supportsParameter(otherParameter())).isFalse();
    }

    @Test
    void resolvesAuthenticatedUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User("reader@example.com", "hash", "Jane", "Doe");
        setUserId(user, userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthenticatedUser(user, false), null));

        Object resolved = resolver.resolveArgument(
                currentUserIdParameter(),
                null,
                mock(NativeWebRequest.class),
                null);

        assertThat(resolved).isEqualTo(userId);
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        assertThatThrownBy(() -> resolver.resolveArgument(
                currentUserIdParameter(),
                null,
                mock(NativeWebRequest.class),
                null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication required");
    }

    @SuppressWarnings("unused")
    private static class Controller {

        void handle(@CurrentUserId UUID userId) {
        }

        void other(String name) {
        }
    }
}
