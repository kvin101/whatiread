package com.whatiread.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whatiread.identity.api.AuthResponse;
import com.whatiread.identity.domain.User;
import com.whatiread.identity.repository.UserRepository;
import com.whatiread.identity.service.TokenIssuer;
import com.whatiread.instance.api.SetupAdminRequest;
import com.whatiread.shared.exception.ConflictException;
import com.whatiread.support.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SetupServiceImplTest {


    private static final String TEST_PASSWORD = TestConstants.TEST_PASSWORD;
    private static final String ENCODED = "encoded";
    private static final String ADMIN_EXAMPLE_COM = "admin@example.com";
    private static final String ADMIN = "Admin";
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private InstanceSettingsService instanceSettingsService;

    @InjectMocks
    private SetupServiceImpl setupService;

    private SetupAdminRequest request;

    @BeforeEach
    void setUp() {
        request = new SetupAdminRequest(
                " Admin@Example.COM ",
                TEST_PASSWORD,
                " Admin ",
                " User ",
                true
        );
    }

    @Test
    void createAdminRejectsWhenSetupAlreadyComplete() {
        when(instanceSettingsService.isSetupRequired()).thenReturn(false);

        assertThatThrownBy(() -> setupService.createAdmin(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Instance is already configured");
    }

    @Test
    void createAdminRejectsDuplicateEmail() {
        when(instanceSettingsService.isSetupRequired()).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThatThrownBy(() -> setupService.createAdmin(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already registered");
    }

    @Test
    void createAdminPersistsUserAndCompletesSetup() {
        AuthResponse tokens = new AuthResponse("access", "refresh", null);
        when(instanceSettingsService.isSetupRequired()).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenIssuer.issueTokens(any(User.class))).thenReturn(tokens);

        AuthResponse response = setupService.createAdmin(request);

        assertThat(response).isSameAs(tokens);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertThat(user.getEmail()).isEqualTo(ADMIN_EXAMPLE_COM);
        assertThat(user.getPasswordHash()).isEqualTo(ENCODED);
        assertThat(user.getFirstName()).isEqualTo(ADMIN);
        assertThat(user.getLastName()).isEqualTo("User");

        verify(instanceSettingsService).setRegistrationEnabled(true);
        verify(instanceSettingsService).markSetupComplete();
        verify(instanceSettingsService).setAdminUserId(user.getId());
    }

    @Test
    void createAdminDefaultsRegistrationToDisabled() {
        SetupAdminRequest noRegistration = new SetupAdminRequest(
                ADMIN_EXAMPLE_COM, TEST_PASSWORD, ADMIN, null, null);
        when(instanceSettingsService.isSetupRequired()).thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase(noRegistration.email())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn(ENCODED);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenIssuer.issueTokens(any(User.class))).thenReturn(
                new AuthResponse("a", "r", null));

        setupService.createAdmin(noRegistration);

        verify(instanceSettingsService).setRegistrationEnabled(false);
        verify(userRepository).save(any(User.class));
    }
}
