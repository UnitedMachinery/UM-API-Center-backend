package com.um.apicenter.apidefinition;

import com.um.apicenter.admin.ApiUser;
import com.um.apicenter.admin.ApiUserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class BusinessApiAuthenticationService {

    private final ApiUserMapper apiUserMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public BusinessApiAuthenticationService(ApiUserMapper apiUserMapper) {
        this.apiUserMapper = apiUserMapper;
    }

    public String authenticate(String username, String password) {
        if (username == null || username.isBlank()) {
            throw BusinessApiException.authenticationFailed("AUTH_USERNAME_MISSING");
        }
        if (password == null || password.isBlank()) {
            throw BusinessApiException.authenticationFailed("AUTH_PASSWORD_MISSING");
        }
        ApiUser user = apiUserMapper.findByUsername(username);
        if (user == null) {
            throw BusinessApiException.authenticationFailed("AUTH_USER_NOT_FOUND");
        }
        if (!user.active()) {
            throw BusinessApiException.authenticationFailed("AUTH_USER_DISABLED");
        }
        if (user.passwordHash() == null || !user.passwordHash().matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
            throw BusinessApiException.authenticationFailed("AUTH_HASH_INVALID");
        }
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw BusinessApiException.authenticationFailed("AUTH_PASSWORD_MISMATCH");
        }
        return user.username();
    }
}
