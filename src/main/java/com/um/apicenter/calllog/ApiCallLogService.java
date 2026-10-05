package com.um.apicenter.calllog;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApiCallLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiCallLogService.class);

    private final ApiCallLogMapper logMapper;
    private final ObjectMapper objectMapper;
    private static final TypeReference<Map<String, Object>> PARAMETER_SUMMARY = new TypeReference<>() { };

    public ApiCallLogService(ApiCallLogMapper logMapper, ObjectMapper objectMapper) {
        this.logMapper = logMapper;
        this.objectMapper = objectMapper;
    }

    public void record(String requestId, Long apiId, String apiPath, String username, String clientIp,
                       Map<String, Object> parameterSummary, String result, String errorCode,
                       Integer recordCount, int durationMs, Map<String, Object> diagnostics) {
        if (apiId == null) {
            return;
        }
        try {
            ApiCallLog log = new ApiCallLog();
            log.setRequestId(requestId);
            log.setApiId(apiId);
            log.setApiPath(apiPath);
            log.setUsername(username);
            log.setClientIp(clientIp);
            log.setParamsSummary(parameterSummary == null ? null : objectMapper.writeValueAsString(parameterSummary));
            log.setRequestDiagnostics(diagnostics == null ? null : objectMapper.writeValueAsString(diagnostics));
            log.setResult(result);
            log.setErrorCode(errorCode);
            log.setRecordCount(recordCount);
            log.setDurationMs(durationMs);
            logMapper.insert(log);
        } catch (JacksonException exception) {
            LOGGER.error("Unable to serialize API call log summary, requestId={}", requestId, exception);
        } catch (RuntimeException exception) {
            LOGGER.error("Unable to write API call log, requestId={}", requestId, exception);
        }
    }

    public LogPage search(LogFilter filter, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        long total = logMapper.count(filter);
        List<LogResponse> records = logMapper.findPage(filter, pageSize, offset).stream()
                .map(this::toResponse).toList();
        return new LogPage(records, total, page, pageSize);
    }

    private LogResponse toResponse(ApiCallLog log) {
        return new LogResponse(log.getId(), log.getRequestId(), log.getApiId(), log.getApiPath(), log.getUsername(),
                log.getClientIp(), readParameterSummary(log.getParamsSummary()), log.getResult(), log.getErrorCode(),
                log.getRecordCount(), log.getDurationMs(), log.getCreatedAt(), readParameterSummary(log.getRequestDiagnostics()));
    }

    private Map<String, Object> readParameterSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(summary, PARAMETER_SUMMARY);
        } catch (JacksonException exception) {
            LOGGER.warn("Stored API call log parameter summary is invalid");
            return null;
        }
    }

    public record LogFilter(LocalDateTime fromTime, LocalDateTime toTime, String username, Long apiId, String result) { }
    public record LogPage(List<LogResponse> records, long total, int page, int pageSize) { }
    public record LogResponse(Long id, String requestId, Long apiId, String apiPath, String username, String clientIp,
                              Map<String, Object> paramsSummary, String result, String errorCode, Integer recordCount,
                              int durationMs, LocalDateTime createdAt, Map<String, Object> requestDiagnostics) { }
}
