package com.whatiread.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.instance.service.InstanceSettingsService;
import com.whatiread.shared.security.SecurityConstants;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String HASH = "hash";

    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InstanceSettingsService instanceSettingsService;
    @Mock
    private FilterChain filterChain;

    private AuthPrincipalCache authPrincipalCache;
    private AuthenticatedUserFactory authenticatedUserFactory;
    private JwtAuthenticationFilter filter;

    private UUID userId;
    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        authPrincipalCache = new AuthPrincipalCache();
        authenticatedUserFactory = new AuthenticatedUserFactory(authPrincipalCache, instanceSettingsService);
        filter = new JwtAuthenticationFilter(
                jwtService,
                userRepository,
                authenticatedUserFactory,
                authPrincipalCache
        );
        userId = UUID.randomUUID();
        user = new User("user@example.com", "user", HASH, "User", "One");
        setId(user, userId);
        token = "access-token";
    }

    @Test
    void secondRequestWithinCacheTtlSkipsUserRepositoryLookup() throws Exception {
        when(instanceSettingsService.resolveEffectiveAdminUserId()).thenReturn(null);
        when(jwtService.parse(token)).thenReturn(new JwtService.JwtClaims(
                userId, user.getEmail(), SecurityConstants.JWT_TYPE_ACCESS, 0L));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MockHttpServletRequest request = bearerRequest(token);
        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);
        SecurityContextHolder.clearContext();
        filter.doFilterInternal(bearerRequest(token), new MockHttpServletResponse(), filterChain);

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void cachedPrincipalWithStaleTokenVersionReturnsUnauthorized() throws Exception {
        authPrincipalCache.put(user);
        when(jwtService.parse(token)).thenReturn(new JwtService.JwtClaims(
                userId, user.getEmail(), SecurityConstants.JWT_TYPE_ACCESS, 1L));

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(bearerRequest(token), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(userRepository, times(0)).findById(any());
        verify(filterChain, times(0)).doFilter(any(), any());
    }

    @Test
    void invalidJwtTokenReturnsUnauthorized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtService.parse("bad")).thenThrow(new RuntimeException("expired"));

        filter.doFilterInternal(bearerRequest("bad"), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, times(0)).doFilter(any(), any());
    }

    private MockHttpServletRequest bearerRequest(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", SecurityConstants.BEARER_PREFIX + value);
        return request;
    }

    private static void setId(Object entity, UUID id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                var field = type.getDeclaredField("id");
                field.setAccessible(true);
                field.set(entity, id);
                return;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("No id on " + entity.getClass());
    }
}
