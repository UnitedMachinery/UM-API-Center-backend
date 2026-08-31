package com.um.apicenter.admin;

import com.um.apicenter.common.ApiResponse;
import com.um.apicenter.common.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminSessionProperties sessionProperties;

    public AdminAuthController(AdminAuthService adminAuthService, AdminSessionProperties sessionProperties) {
        this.adminAuthService = adminAuthService;
        this.sessionProperties = sessionProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminIdentityResponse>> login(
            @Valid @RequestBody AdminLoginRequest loginRequest, HttpServletRequest request) {
        AuthenticatedAdminSession session = adminAuthService.login(loginRequest.username(), loginRequest.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie(session.token()).toString())
                .body(ApiResponse.success(requestId(request),
                        new AdminIdentityResponse(session.principal().username()), Map.of()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> logout(
            @CookieValue(value = "UM_API_ADMIN_SESSION", required = false) String token,
            HttpServletRequest request) {
        adminAuthService.logout(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredSessionCookie().toString())
                .body(ApiResponse.success(requestId(request), Map.of("logged_out", true), Map.of()));
    }

    @GetMapping("/session")
    public ApiResponse<AdminIdentityResponse> currentSession(
            @CookieValue(value = "UM_API_ADMIN_SESSION", required = false) String token,
            HttpServletRequest request) {
        AdminPrincipal principal = adminAuthService.requireCurrentAdmin(token);
        return ApiResponse.success(requestId(request), new AdminIdentityResponse(principal.username()), Map.of());
    }

    private ResponseCookie sessionCookie(String token) {
        return ResponseCookie.from(sessionProperties.getCookieName(), token)
                .httpOnly(true)
                .secure(sessionProperties.isCookieSecure())
                .sameSite("Lax")
                .path(sessionProperties.getCookiePath())
                .maxAge(sessionProperties.getTtl())
                .build();
    }

    private ResponseCookie expiredSessionCookie() {
        return ResponseCookie.from(sessionProperties.getCookieName(), "")
                .httpOnly(true)
                .secure(sessionProperties.isCookieSecure())
                .sameSite("Lax")
                .path(sessionProperties.getCookiePath())
                .maxAge(0)
                .build();
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
    }

    public record AdminLoginRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(max = 255) String password) {
    }

    public record AdminIdentityResponse(String username) {
    }
}
