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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
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
    private final ObjectMapper objectMapper;

    public BusinessApiController(BusinessApiAuthenticationService authenticationService,
                                 ApiDefinitionRuntimeService definitionService,
                                 BusinessParameterValidator parameterValidator,
                                 DynamicQueryExecutor queryExecutor, ApiCallLogService callLogService,
                                 ObjectMapper objectMapper) {
        this.authenticationService = authenticationService;
        this.definitionService = definitionService;
        this.parameterValidator = parameterValidator;
        this.queryExecutor = queryExecutor;
        this.callLogService = callLogService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/**")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> execute(
            @RequestBody(required = false) String rawBody, HttpServletRequest request) {
        long startedAt = System.nanoTime();
        String requestId = requestId(request);
        String apiPath = apiPath(request);
        String username = null;
        Long apiId = null;
        Map<String, Object> parameterSummary = null;
        String result = "failed";
        String errorCode = "INTERNAL_ERROR";
        Integer recordCount = null;
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("stage", "INTERFACE_LOOKUP");

        try {
            ApiDefinitionRuntimeService.RuntimeDefinition runtimeDefinition = definitionService.findByPath(apiPath);
            apiId = runtimeDefinition.definition().getId();
            diagnostics.put("stage", "REQUEST_BODY");
            BusinessApiRequest body = rawBody == null || rawBody.isBlank() ? null
                    : objectMapper.readValue(rawBody, BusinessApiRequest.class);
            String receivedUsername = body == null || body.auth() == null ? null : body.auth().username();
            String receivedPassword = body == null || body.auth() == null ? null : body.auth().password();
            username = receivedUsername == null ? null : receivedUsername.substring(0, Math.min(100, receivedUsername.length()));
            diagnostics.put("auth_present", body != null && body.auth() != null);
            diagnostics.put("username_present", receivedUsername != null && !receivedUsername.isBlank());
            diagnostics.put("password_present", receivedPassword != null && !receivedPassword.isBlank());
            diagnostics.put("params_present", body != null && body.params() != null);
            Map<String, Object> receivedParameters = body == null || body.params() == null ? Map.of() : body.params();
            parameterSummary = parameterValidator.maskedSummary(receivedParameters, runtimeDefinition.parameters());
            long unknownCount = receivedParameters.keySet().stream().filter(name -> runtimeDefinition.parameters()
                    .stream().noneMatch(parameter -> parameter.name().equals(name))).count();
            diagnostics.put("unknown_parameter_count", unknownCount);
            diagnostics.put("stage", "AUTHENTICATION");
            if (body == null || body.auth() == null) {
                throw BusinessApiException.authenticationFailed("AUTH_OBJECT_MISSING");
            }
            username = authenticationService.authenticate(receivedUsername, receivedPassword);
            diagnostics.put("stage", "INTERFACE_STATE");
            if (!runtimeDefinition.definition().isEnabled()) {
                throw BusinessApiException.disabled();
            }
            diagnostics.put("stage", "PARAMETER_VALIDATION");
            Map<String, Object> parameters = parameterValidator.validate(body == null ? null : body.params(),
                    runtimeDefinition.parameters());
            parameterSummary = parameterValidator.maskedSummary(parameters, runtimeDefinition.parameters());
            diagnostics.put("stage", "QUERY_EXECUTION");
            DynamicQueryExecutor.QueryResult queryResult = queryExecutor.execute(runtimeDefinition.definition(), parameters);
            result = "success";
            errorCode = null;
            recordCount = queryResult.rows().size();
            diagnostics.put("stage", "COMPLETED");
            return ResponseEntity.ok(ApiResponse.success(requestId, queryResult.rows(),
                    Map.of("count", recordCount, "truncated", queryResult.truncated())));
        } catch (BusinessApiException exception) {
            errorCode = exception.getCode();
            diagnostics.put("reason_code", exception.getDiagnosticCode());
            // Authentication details are represented only by a stable internal code.
            diagnostics.put("message", exception.getMessage());
            return ResponseEntity.status(exception.getStatus())
                    .body(ApiResponse.failure(requestId, exception.getCode(), exception.getMessage()));
        } catch (JacksonException exception) {
            errorCode = "VALIDATION_FAILED";
            diagnostics.put("reason_code", "REQUEST_BODY_INVALID");
            diagnostics.put("message", "请求 JSON 或 auth/params 结构不合法；未记录原始请求体");
            return ResponseEntity.badRequest().body(ApiResponse.failure(requestId, errorCode, "请求参数不合法"));
        } catch (Exception exception) {
            LOGGER.error("Configured query execution failed, requestId={}, apiId={}", requestId, apiId, exception);
            errorCode = "QUERY_FAILED";
            diagnostics.put("reason_code", errorCode);
            diagnostics.put("message", "系统处理或查询执行失败，请根据请求编号排查服务器状态");
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.failure(requestId, "QUERY_FAILED", "查询执行失败"));
        } finally {
            int durationMs = (int) Math.min(Integer.MAX_VALUE, (System.nanoTime() - startedAt) / 1_000_000);
            if (apiId != null) {
                callLogService.record(requestId, apiId, apiPath, username, request.getRemoteAddr(), parameterSummary,
                        result, errorCode, recordCount, durationMs, diagnostics);
            }
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
