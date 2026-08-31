package com.um.apicenter.admin;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AdminAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiUserMapper apiUserMapper;
    private final AdminSessionMapper adminSessionMapper;
    private final AdminSessionProperties sessionProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAuthService(ApiUserMapper apiUserMapper, AdminSessionMapper adminSessionMapper,
                            AdminSessionProperties sessionProperties) {
        this.apiUserMapper = apiUserMapper;
        this.adminSessionMapper = adminSessionMapper;
        this.sessionProperties = sessionProperties;
    }

    @Transactional
    public AuthenticatedAdminSession login(String username, String password) {
        ApiUser user = apiUserMapper.findByUsername(username);
        if (user == null || !user.active() || !user.apiAdmin() || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new AdminAuthenticationException();
        }

        String token = createToken();
        adminSessionMapper.insert(new AdminSession(
                user.id(), hashToken(token), LocalDateTime.now(ZoneOffset.UTC).plus(sessionProperties.getTtl())));
        return new AuthenticatedAdminSession(new AdminPrincipal(user.id(), user.username()), token);
    }

    public AdminPrincipal requireCurrentAdmin(String token) {
        if (token == null || token.isBlank()) {
            throw new AdminAuthenticationException();
        }
        AdminPrincipal principal = adminSessionMapper.findValidAdminByTokenHash(hashToken(token));
        if (principal == null) {
            throw new AdminAuthenticationException();
        }
        return principal;
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            adminSessionMapper.deleteByTokenHash(hashToken(token));
        }
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
