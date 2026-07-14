package com.whatiread.instance.service;

import com.whatiread.identity.api.AuthResponse;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.identity.service.TokenIssuer;
import com.whatiread.identity.service.UsernameService;
import com.whatiread.identity.suggest.UserSearchIndexService;
import com.whatiread.instance.api.SetupAdminRequest;
import com.whatiread.shared.exception.ConflictException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SetupServiceImpl implements SetupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    private final InstanceSettingsService instanceSettingsService;
    private final UsernameService usernameService;
    private final UserSearchIndexService userSearchIndexService;

    public SetupServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenIssuer tokenIssuer,
            InstanceSettingsService instanceSettingsService,
            UsernameService usernameService,
            UserSearchIndexService userSearchIndexService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.instanceSettingsService = instanceSettingsService;
        this.usernameService = usernameService;
        this.userSearchIndexService = userSearchIndexService;
    }

    @Override
    public AuthResponse createAdmin(SetupAdminRequest request) {
        if (!instanceSettingsService.isSetupRequired()) {
            throw new ConflictException("Instance is already configured");
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
        userRepository.save(user);
        usernameService.indexUsername(user.getUsername());
        userSearchIndexService.syncUser(user);

        boolean registration = request.registrationEnabled() != null
                ? request.registrationEnabled()
                : false;
        instanceSettingsService.setRegistrationEnabled(registration);
        instanceSettingsService.markSetupComplete();
        instanceSettingsService.setAdminUserId(user.getId());

        return tokenIssuer.issueTokens(user);
    }
}
