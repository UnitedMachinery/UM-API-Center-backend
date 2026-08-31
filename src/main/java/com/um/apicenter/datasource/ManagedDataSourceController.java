package com.um.apicenter.datasource;

import com.um.apicenter.admin.AdminAuthService;
import com.um.apicenter.admin.AdminPrincipal;
import com.um.apicenter.common.ApiResponse;
import com.um.apicenter.common.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/datasources")
public class ManagedDataSourceController {

    private final AdminAuthService adminAuthService;
    private final ManagedDataSourceService dataSourceService;

    public ManagedDataSourceController(AdminAuthService adminAuthService, ManagedDataSourceService dataSourceService) {
        this.adminAuthService = adminAuthService;
        this.dataSourceService = dataSourceService;
    }

    @GetMapping
    public ApiResponse<List<DataSourceResponse>> list(
            @CookieValue(value = "UM_API_ADMIN_SESSION", required = false) String token, HttpServletRequest request) {
        requireAdmin(token);
        List<DataSourceResponse> result = dataSourceService.list().stream().map(DataSourceResponse::from).toList();
        return ApiResponse.success(requestId(request), result, Map.of("count", result.size()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DataSourceResponse>> create(
            @CookieValue(value = "UM_API_ADMIN_SESSION", required = false) String token,
            @Valid @RequestBody CreateDataSourceRequest body, HttpServletRequest request) {
        ManagedDataSource created = dataSourceService.create(body.toInput(), requireAdmin(token));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(requestId(request), DataSourceResponse.from(created), Map.of()));
    }

    @PutMapping("/{id}")
    public ApiResponse<DataSourceResponse> update(
            @PathVariable long id, @CookieValue(value = "UM_API_ADMIN_SESSION", required = false) String token,
            @Valid @RequestBody UpdateDataSourceRequest body, HttpServletRequest request) {
        ManagedDataSource updated = dataSourceService.update(id, body.toInput(), requireAdmin(token));
        return ApiResponse.success(requestId(request), DataSourceResponse.from(updated), Map.of());
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<DataSourceResponse> updateEnabled(
            @PathVariable long id, @CookieValue(value = "UM_API_ADMIN_SESSION", required = false) String token,
            @Valid @RequestBody EnableRequest body, HttpServletRequest request) {
        ManagedDataSource updated = dataSourceService.setEnabled(id, body.enabled(), requireAdmin(token));
        return ApiResponse.success(requestId(request), DataSourceResponse.from(updated), Map.of());
    }

    @PostMapping("/{id}/test")
    public ApiResponse<ConnectionTestResponse> test(
            @PathVariable long id, @CookieValue(value = "UM_API_ADMIN_SESSION", required = false) String token,
            HttpServletRequest request) {
        requireAdmin(token);
        JdbcConnectionTester.ConnectionTestResult result = dataSourceService.testConnection(id);
        return ApiResponse.success(requestId(request), new ConnectionTestResponse(result.success(), result.message()), Map.of());
    }

    private AdminPrincipal requireAdmin(String token) {
        return adminAuthService.requireCurrentAdmin(token);
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
    }

    public record CreateDataSourceRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull DataSourceType dbType,
            @NotBlank @Size(max = 255) String host,
            @Min(1) @Max(65535) int port,
            @NotBlank @Size(max = 100) String databaseName,
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(max = 255) String password,
            boolean enabled,
            @Size(max = 500) String remark) {
        ManagedDataSourceService.DataSourceInput toInput() {
            return new ManagedDataSourceService.DataSourceInput(
                    name, dbType, host, port, databaseName, username, password, enabled, remark);
        }
    }

    public record UpdateDataSourceRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull DataSourceType dbType,
            @NotBlank @Size(max = 255) String host,
            @Min(1) @Max(65535) int port,
            @NotBlank @Size(max = 100) String databaseName,
            @NotBlank @Size(max = 100) String username,
            @Size(max = 255) String password,
            boolean enabled,
            @Size(max = 500) String remark) {
        ManagedDataSourceService.DataSourceInput toInput() {
            return new ManagedDataSourceService.DataSourceInput(
                    name, dbType, host, port, databaseName, username, password, enabled, remark);
        }
    }

    public record EnableRequest(@NotNull Boolean enabled) {
    }

    public record ConnectionTestResponse(boolean success, String message) {
    }

    public record DataSourceResponse(long id, String name, DataSourceType dbType, String host, int port,
                                     String databaseName, String username, boolean passwordConfigured, boolean enabled,
                                     String remark, String createdBy, LocalDateTime createdAt, String updatedBy,
                                     LocalDateTime updatedAt) {
        static DataSourceResponse from(ManagedDataSource dataSource) {
            return new DataSourceResponse(dataSource.getId(), dataSource.getName(), dataSource.getDbType(),
                    dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName(), dataSource.getUsername(),
                    dataSource.getPasswordEncrypted() != null && !dataSource.getPasswordEncrypted().isBlank(),
                    dataSource.isEnabled(), dataSource.getRemark(), dataSource.getCreatedBy(), dataSource.getCreatedAt(),
                    dataSource.getUpdatedBy(), dataSource.getUpdatedAt());
        }
    }
}
