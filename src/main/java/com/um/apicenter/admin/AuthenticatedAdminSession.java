package com.um.apicenter.admin;

public record AuthenticatedAdminSession(AdminPrincipal principal, String token) {
}
