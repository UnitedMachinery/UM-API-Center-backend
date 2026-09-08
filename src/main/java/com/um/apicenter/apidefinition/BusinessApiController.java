package com.um.apicenter.apidefinition;

import com.um.apicenter.calllog.ApiCallLogService;
import com.um.apicenter.common.ApiResponse;
import com.um.apicenter.common.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class BusinessApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessApiController.class);

    private final BusinessApiAuthenticationService authenticationService;
    private final ApiDefinitionRuntimeService definitionService;
    private final BusinessParameterValidator parameterValidator;
    private final DynamicQueryExecutor queryExecutor;
    private final ApiCallLogService callLogService;

    public BusinessApiController(BusinessApiAuthenticationService authenticationService,
                                 ApiDefinitionRuntimeService definitionService,
                                 BusinessParameterValidator parameterValidator,
                                 DynamicQueryExecutor queryExecutor, ApiCallLogService callLogService) {
        this.authenticationService = authenticationService;
        this.definitionService = definitionService;
        this.parameterValidator = parameterValidator;
        this.queryExecutor = queryExecutor;
        this.callLogService = callLogService;
    }

    @PostMapping("/api/**")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> execute(
            @RequestBody(required = false) BusinessApiRequest body, HttpServletRequest request) {
        long startedAt = System.nanoTime();
        String requestId = requestId(request);
        String apiPath = apiPath(request);
        String username = null;
        Long apiId = null;
        Map<String, Object> parameterSummary = null;
        String result = "failed";
        String errorCode = "INTERNAL_ERROR";
        Integer recordCount = null;

        try {
            username = authenticationService.authenticate(body == null || body.auth() == null ? null : body.auth().username(),
                    body == null || body.auth() == null ? null : body.auth().password());
            ApiDefinitionRuntimeService.RuntimeDefinition runtimeDefinition = definitionService.findByPath(apiPath);
            apiId = runtimeDefinition.definition().getId();
            Map<String, Object> parameters = parameterValidator.validate(body == null ? null : body.params(),
                    runtimeDefinition.parameters());
            parameterSummary = parameterValidator.maskedSummary(parameters, runtimeDefinition.parameters());
            DynamicQueryExecutor.QueryResult queryResult = queryExecutor.execute(runtimeDefinition.definition(), parameters);
            result = "success";
            errorCode = null;
            recordCount = queryResult.rows().size();
            return ResponseEntity.ok(ApiResponse.success(requestId, queryResult.rows(),
                    Map.of("count", recordCount, "truncated", queryResult.truncated())));
        } catch (BusinessApiException exception) {
            errorCode = exception.getCode();
            return ResponseEntity.status(exception.getStatus())
                    .body(ApiResponse.failure(requestId, exception.getCode(), exception.getMessage()));
        } catch (Exception exception) {
            LOGGER.error("Configured query execution failed, requestId={}, apiId={}", requestId, apiId, exception);
            errorCode = "QUERY_FAILED";
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.failure(requestId, "QUERY_FAILED", "查询执行失败"));
        } finally {
            int durationMs = (int) Math.min(Integer.MAX_VALUE, (System.nanoTime() - startedAt) / 1_000_000);
            callLogService.record(requestId, apiId, apiPath, username, request.getRemoteAddr(), parameterSummary,
                    result, errorCode, recordCount, durationMs);
        }
    }

    private String apiPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String path = uri.substring("/api".length());
        return path.isBlank() ? "/" : path;
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
    }

    public record BusinessApiRequest(Auth auth, Map<String, Object> params) { }
    public record Auth(String username, String password) { }
}
