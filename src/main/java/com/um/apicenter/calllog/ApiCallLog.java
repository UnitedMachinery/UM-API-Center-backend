package com.um.apicenter.calllog;

import java.time.LocalDateTime;

public class ApiCallLog {
    private Long id;
    private String requestId;
    private Long apiId;
    private String apiPath;
    private String username;
    private String clientIp;
    private String paramsSummary;
    private String requestDiagnostics;
    private String result;
    private String errorCode;
    private Integer recordCount;
    private int durationMs;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getApiId() { return apiId; }
    public void setApiId(Long apiId) { this.apiId = apiId; }
    public String getApiPath() { return apiPath; }
    public void setApiPath(String apiPath) { this.apiPath = apiPath; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getParamsSummary() { return paramsSummary; }
    public void setParamsSummary(String paramsSummary) { this.paramsSummary = paramsSummary; }
    public String getRequestDiagnostics() { return requestDiagnostics; }
    public void setRequestDiagnostics(String requestDiagnostics) { this.requestDiagnostics = requestDiagnostics; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
    public int getDurationMs() { return durationMs; }
    public void setDurationMs(int durationMs) { this.durationMs = durationMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
