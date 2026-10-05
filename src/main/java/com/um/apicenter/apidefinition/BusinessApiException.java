package com.um.apicenter.apidefinition;

import org.springframework.http.HttpStatus;

public class BusinessApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private String diagnosticCode;

    private BusinessApiException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
    public String getDiagnosticCode() { return diagnosticCode == null ? code : diagnosticCode; }

    public static BusinessApiException authenticationFailed() {
        return authenticationFailed("AUTH_FAILED");
    }

    // Only persisted in the administrator-visible log; never sent to callers.
    public static BusinessApiException authenticationFailed(String diagnosticCode) {
        BusinessApiException exception = new BusinessApiException("AUTH_FAILED", "认证失败", HttpStatus.UNAUTHORIZED);
        exception.diagnosticCode = diagnosticCode;
        return exception;
    }

    public static BusinessApiException notFound() {
        return new BusinessApiException("API_NOT_FOUND", "接口不存在", HttpStatus.NOT_FOUND);
    }

    public static BusinessApiException disabled() {
        return new BusinessApiException("API_DISABLED", "接口已停用", HttpStatus.FORBIDDEN);
    }

    public static BusinessApiException invalidParameter(String message) {
        return new BusinessApiException("PARAMETER_INVALID", message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessApiException dataSourceUnavailable() {
        return new BusinessApiException("DATASOURCE_UNAVAILABLE", "数据源不可用", HttpStatus.SERVICE_UNAVAILABLE);
    }

    public static BusinessApiException timeout() {
        return new BusinessApiException("QUERY_TIMEOUT", "查询超时", HttpStatus.GATEWAY_TIMEOUT);
    }

    public static BusinessApiException queryFailed() {
        return new BusinessApiException("QUERY_FAILED", "查询执行失败", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
