package com.whatiread.identity.security;

import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.shared.security.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthenticatedUserFactory authenticatedUserFactory;
    private final AuthPrincipalCache authPrincipalCache;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            AuthenticatedUserFactory authenticatedUserFactory,
            AuthPrincipalCache authPrincipalCache
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authenticatedUserFactory = authenticatedUserFactory;
        this.authPrincipalCache = authPrincipalCache;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            String token = header.substring(SecurityConstants.BEARER_PREFIX_LENGTH);
            try {
                JwtService.JwtClaims claims = jwtService.parse(token);
                if (SecurityConstants.JWT_TYPE_ACCESS.equals(claims.type())
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Optional<AuthPrincipalCache.Snapshot> cached = authPrincipalCache.get(claims.userId());
                    if (cached.isPresent()) {
                        if (!isValidPrincipal(cached.get(), claims.tokenVersion())) {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            return;
                        }
                        setAuthentication(authenticatedUserFactory.create(cached.get()), request);
                    } else {
                        Optional<User> user = userRepository.findById(claims.userId());
                        if (user.isEmpty() || !isValidPrincipal(authPrincipalCache.fromUser(user.get()), claims.tokenVersion())) {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            return;
                        }
                        setAuthentication(authenticatedUserFactory.create(user.get()), request);
                    }
                }
            } catch (Exception ex) {
                log.debug("Invalid JWT token: {}", ex.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isValidPrincipal(AuthPrincipalCache.Snapshot snapshot, Long tokenVersion) {
        return snapshot.enabled() && tokenVersion != null && snapshot.tokenVersion() == tokenVersion;
    }

    private void setAuthentication(AuthenticatedUser principal, HttpServletRequest request) {
        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
