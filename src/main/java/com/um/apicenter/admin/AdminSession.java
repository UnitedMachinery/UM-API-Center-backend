package com.um.apicenter.admin;

import java.time.LocalDateTime;

public record AdminSession(Long userId, String tokenHash, LocalDateTime expiresAt) {
}
