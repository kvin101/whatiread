package com.whatiread.identity.service;

import com.whatiread.config.BusinessMetrics;
import com.whatiread.identity.api.AuthResponse;
import com.whatiread.identity.api.LoginRequest;
import com.whatiread.identity.api.RefreshRequest;
import com.whatiread.identity.api.RegisterRequest;
import com.whatiread.identity.api.UserResponse;
import com.whatiread.identity.domain.RefreshToken;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.RefreshTokenRepository;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.identity.security.AuthenticatedUser;
import com.whatiread.identity.security.AuthPrincipalCache;
import com.whatiread.identity.security.SecurityUtils;
import com.whatiread.identity.security.TokenHasher;
import com.whatiread.instance.service.InstanceSettingsService;
import com.whatiread.identity.suggest.UserSearchIndexService;
import com.whatiread.shared.event.UserRegisteredEvent;
import com.whatiread.shared.exception.ConflictException;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shared.exception.UnauthorizedException;
import com.whatiread.shared.outbox.OutboxEventPublisher;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final InstanceSettingsService instanceSettingsService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final TokenIssuer tokenIssuer;
    private final UserMapper userMapper;
    private final BusinessMetrics businessMetrics;
    private final UsernameService usernameService;
    private final UserSearchIndexService userSearchIndexService;
    private final AuthPrincipalCache authPrincipalCache;

    public AuthServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            InstanceSettingsService instanceSettingsService,
            OutboxEventPublisher outboxEventPublisher,
            TokenIssuer tokenIssuer,
            UserMapper userMapper,
            BusinessMetrics businessMetrics,
            UsernameService usernameService,
            UserSearchIndexService userSearchIndexService,
            AuthPrincipalCache authPrincipalCache
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.instanceSettingsService = instanceSettingsService;
        this.outboxEventPublisher = outboxEventPublisher;
        this.tokenIssuer = tokenIssuer;
        this.userMapper = userMapper;
        this.businessMetrics = businessMetrics;
        this.usernameService = usernameService;
        this.userSearchIndexService = userSearchIndexService;
        this.authPrincipalCache = authPrincipalCache;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (!instanceSettingsService.isRegistrationEnabled()) {
            throw new ForbiddenException("Registration is disabled on this instance");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email already registered");
        }
        String username = usernameService.normalizeAndValidate(request.username());
        usernameService.requireAvailable(username);
        User user = new User(
                request.email().trim().toLowerCase(),
                username,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName() != null ? request.lastName().trim() : null
        );
        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            user.setPhoneNumber(request.phoneNumber().trim());
        }
        boolean firstUser = instanceSettingsService.isSetupRequired();
        userRepository.save(user);
        usernameService.indexUsername(user.getUsername());
        userSearchIndexService.syncUser(user);
        if (firstUser) {
            instanceSettingsService.markSetupComplete();
            instanceSettingsService.setAdminUserId(user.getId());
        }
        outboxEventPublisher.publish("UserRegisteredEvent", new UserRegisteredEvent(user.getId(), firstUser));
        businessMetrics.recordUserRegistered();
        return tokenIssuer.issueTokens(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String identifier = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    identifier,
                    request.password()
            ));
        } catch (BadCredentialsException ex) {
            businessMetrics.recordLoginFailure("invalid_credentials");
            throw new UnauthorizedException("Invalid credentials");
        } catch (DisabledException ex) {
            businessMetrics.recordLoginFailure("disabled");
            throw new UnauthorizedException("Account disabled");
        }
        User user = userRepository.findByEmailIgnoreCase(identifier)
                .or(() -> userRepository.findByUsernameIgnoreCase(identifier))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        businessMetrics.recordLoginSuccess();
        return tokenIssuer.issueTokens(user);
    }

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        String hash = TokenHasher.hash(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new UnauthorizedException("Refresh token expired");
        }
        User user = stored.getUser();
        if (!user.isEnabled()) {
            refreshTokenRepository.delete(stored);
            throw new UnauthorizedException("Account disabled");
        }
        refreshTokenRepository.delete(stored);
        return tokenIssuer.issueTokens(user);
    }

    @Override
    public void logout(RefreshRequest request) {
        String hash = TokenHasher.hash(request.refreshToken());
        refreshTokenRepository.findByTokenHash(hash).ifPresent(stored -> {
            authPrincipalCache.invalidate(stored.getUser().getId());
            refreshTokenRepository.delete(stored);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        AuthenticatedUser principal = SecurityUtils.currentUser();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return userMapper.toUserResponse(user);
    }
}
