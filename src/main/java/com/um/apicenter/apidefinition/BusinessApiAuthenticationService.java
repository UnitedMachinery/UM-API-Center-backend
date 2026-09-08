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
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw BusinessApiException.authenticationFailed();
        }
        ApiUser user = apiUserMapper.findByUsername(username);
        if (user == null || !user.active() || !passwordEncoder.matches(password, user.passwordHash())) {
            throw BusinessApiException.authenticationFailed();
        }
        return user.username();
    }
}
