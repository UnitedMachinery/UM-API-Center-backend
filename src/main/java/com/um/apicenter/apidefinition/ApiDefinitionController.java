package com.um.apicenter.apidefinition;

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
@RequestMapping("/api/admin/apis")
public class ApiDefinitionController {

    private final AdminAuthService adminAuthService;
    private final ApiDefinitionService definitionService;

    public ApiDefinitionController(AdminAuthService adminAuthService, ApiDefinitionService definitionService) {
        this.adminAuthService = adminAuthService;
        this.definitionService = definitionService;
    }

    @GetMapping
    public ApiResponse<List<ApiDefinitionSummaryResponse>> list(
            @CookieValue(value = "UM_API_ADMIN_SESSION", required = false) String token, HttpServletRequest request) {
        requireAdmin(token);
        List<ApiDefinitionSummaryResponse> definitions = definitionService.list().stream()
                .map(ApiDefinitionSummaryResponse::from).toList();
        return ApiResponse.success(requestId(request), definitions, Map.of("count", definitions.size()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApiDefinitionResponse> get(
            @PathVariable long id, @CookieValue(value = "UM_API_ADMIN_SESSION", required = false) String token,
            HttpServletRequest request) {
        requireAdmin(token);
        return ApiResponse.success(requestId(request), ApiDefinitionResponse.from(definitionService.get(id), definitionService), Map.of());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ApiDefinitionResponse>> create(
            @CookieValue(value = "UM_API_ADMIN_SESSION", required = false) String token,
            @Valid @RequestBody ApiDefinitionRequest body, HttpServletRequest request) {
        ApiDefinition created = definitionService.create(body.toInput(), requireAdmin(token));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(requestId(request),
                ApiDefinitionResponse.from(created, definitionService), Map.of()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ApiDefinitionResponse> update(
            @PathVariable long id, @CookieValue(value = "UM_API_ADMIN_SESSION", required = false) String token,
            @Valid @RequestBody ApiDefinitionRequest body, HttpServletRequest request) {
        ApiDefinition updated = definitionService.update(id, body.toInput(), requireAdmin(token));
        return ApiResponse.success(requestId(request), ApiDefinitionResponse.from(updated, definitionService), Map.of());
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<ApiDefinitionResponse> setEnabled(
            @PathVariable long id, @CookieValue(value = "UM_API_ADMIN_SESSION", required = false) String token,
            @Valid @RequestBody EnableRequest body, HttpServletRequest request) {
        ApiDefinition updated = definitionService.setEnabled(id, body.enabled(), requireAdmin(token));
        return ApiResponse.success(requestId(request), ApiDefinitionResponse.from(updated, definitionService), Map.of());
    }

    private AdminPrincipal requireAdmin(String token) {
        return adminAuthService.requireCurrentAdmin(token);
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
    }

    public record ApiDefinitionRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 200) String apiPath,
            @Min(1) long datasourceId,
            @NotNull List<ApiParameterDefinition> parameters,
            @NotBlank String sqlText,
            @Min(1) @Max(20) int timeoutSeconds,
            @Min(1) @Max(10000) int maxRows,
            boolean enabled,
            @Size(max = 500) String remark) {
        ApiDefinitionService.ApiDefinitionInput toInput() {
            return new ApiDefinitionService.ApiDefinitionInput(name, apiPath, datasourceId, parameters, sqlText,
                    timeoutSeconds, maxRows, enabled, remark);
        }
    }

    public record EnableRequest(@NotNull Boolean enabled) {
    }

    public record ApiDefinitionSummaryResponse(long id, String name, String apiPath, long datasourceId,
                                               int timeoutSeconds, int maxRows, boolean enabled, String remark,
                                               String createdBy, LocalDateTime createdAt, String updatedBy,
                                               LocalDateTime updatedAt) {
        static ApiDefinitionSummaryResponse from(ApiDefinition definition) {
            return new ApiDefinitionSummaryResponse(definition.getId(), definition.getName(), definition.getApiPath(),
                    definition.getDatasourceId(), definition.getTimeoutSeconds(), definition.getMaxRows(),
                    definition.isEnabled(), definition.getRemark(), definition.getCreatedBy(), definition.getCreatedAt(),
                    definition.getUpdatedBy(), definition.getUpdatedAt());
        }
    }

    public record ApiDefinitionResponse(long id, String name, String apiPath, long datasourceId,
                                        List<ApiParameterDefinition> parameters, String sqlText, int timeoutSeconds,
                                        int maxRows, boolean enabled, String remark, String createdBy,
                                        LocalDateTime createdAt, String updatedBy, LocalDateTime updatedAt) {
        static ApiDefinitionResponse from(ApiDefinition definition, ApiDefinitionService service) {
            return new ApiDefinitionResponse(definition.getId(), definition.getName(), definition.getApiPath(),
                    definition.getDatasourceId(), service.readParameters(definition), definition.getSqlText(),
                    definition.getTimeoutSeconds(), definition.getMaxRows(), definition.isEnabled(), definition.getRemark(),
                    definition.getCreatedBy(), definition.getCreatedAt(), definition.getUpdatedBy(), definition.getUpdatedAt());
        }
    }
}
