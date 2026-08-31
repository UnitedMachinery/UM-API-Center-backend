package com.um.apicenter.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private ApiUserMapper apiUserMapper;

    @Mock
    private AdminSessionMapper adminSessionMapper;

    @Captor
    private ArgumentCaptor<AdminSession> sessionCaptor;

    private AdminAuthService adminAuthService;

    @BeforeEach
    void configureSession() {
        AdminSessionProperties properties = new AdminSessionProperties();
        properties.setTtl(Duration.ofHours(8));
        adminAuthService = new AdminAuthService(apiUserMapper, adminSessionMapper, properties);
    }

    @Test
    void loginCreatesHashedServerSideSessionForActiveAdministrator() {
        String password = "correct-password";
        String passwordHash = new BCryptPasswordEncoder().encode(password);
        when(apiUserMapper.findByUsername("admin")).thenReturn(
                new ApiUser(1L, "admin", passwordHash, true, true));

        LocalDateTime beforeLogin = LocalDateTime.now(ZoneOffset.UTC);
        AuthenticatedAdminSession result = adminAuthService.login("admin", password);

        verify(adminSessionMapper).insert(sessionCaptor.capture());
        AdminSession persistedSession = sessionCaptor.getValue();
        assertThat(result.principal().username()).isEqualTo("admin");
        assertThat(result.token()).hasSizeGreaterThan(32);
        assertThat(persistedSession.userId()).isEqualTo(1L);
        assertThat(persistedSession.tokenHash()).hasSize(64).isNotEqualTo(result.token());
        assertThat(persistedSession.expiresAt()).isAfter(beforeLogin.plusHours(7).plusMinutes(59));
    }

    @Test
    void loginRejectsWrongPasswordWithOneStableAuthenticationError() {
        when(apiUserMapper.findByUsername("admin")).thenReturn(
                new ApiUser(1L, "admin", new BCryptPasswordEncoder().encode("correct-password"), true, true));

        assertThatThrownBy(() -> adminAuthService.login("admin", "wrong-password"))
                .isInstanceOf(AdminAuthenticationException.class);
    }

    @Test
    void disabledOrNonAdminAccountIsRejected() {
        when(apiUserMapper.findByUsername(anyString())).thenReturn(
                new ApiUser(1L, "operator", new BCryptPasswordEncoder().encode("password"), false, false));

        assertThatThrownBy(() -> adminAuthService.login("operator", "password"))
                .isInstanceOf(AdminAuthenticationException.class);
    }

    @Test
    void currentSessionRequiresActiveAdministrator() {
        when(adminSessionMapper.findValidAdminByTokenHash(anyString()))
                .thenReturn(new AdminPrincipal(1L, "admin"));

        AdminPrincipal principal = adminAuthService.requireCurrentAdmin("opaque-session-token");

        assertThat(principal.username()).isEqualTo("admin");
    }
}
