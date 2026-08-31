package com.um.apicenter.admin;

public record ApiUser(Long id, String username, String passwordHash, boolean active, boolean apiAdmin) {
}
