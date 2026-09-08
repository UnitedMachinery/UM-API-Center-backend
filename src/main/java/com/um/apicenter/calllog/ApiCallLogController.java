package com.um.apicenter.calllog;

import com.um.apicenter.admin.AdminAuthService;
import com.um.apicenter.common.ApiResponse;
import com.um.apicenter.common.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/call-logs")
public class ApiCallLogController {

    private final AdminAuthService adminAuthService;
    private final ApiCallLogService callLogService;

    public ApiCallLogController(AdminAuthService adminAuthService, ApiCallLogService callLogService) {
        this.adminAuthService = adminAuthService;
        this.callLogService = callLogService;
    }

    @GetMapping
    public ApiResponse<ApiCallLogService.LogPage> list(
            @CookieValue(value = "UM_API_ADMIN_SESSION", required = false) String token,
            @RequestParam(name = "from_time", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam(name = "to_time", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime,
            @RequestParam(required = false) String username,
            @RequestParam(name = "api_id", required = false) @Min(1) Long apiId,
            @RequestParam(required = false) @Pattern(regexp = "success|failed") String result,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "page_size", defaultValue = "20") @Min(1) @Max(100) int pageSize,
            HttpServletRequest request) {
        adminAuthService.requireCurrentAdmin(token);
        ApiCallLogService.LogFilter filter = new ApiCallLogService.LogFilter(fromTime, toTime, username, apiId, result);
        ApiCallLogService.LogPage logs = callLogService.search(filter, page, pageSize);
        return ApiResponse.success(requestId(request), logs, Map.of());
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
    }
}
