package com.um.apicenter.apidefinition;

import com.um.apicenter.admin.ApiUser;
import com.um.apicenter.admin.ApiUserMapper;
import com.um.apicenter.calllog.ApiCallLog;
import com.um.apicenter.calllog.ApiCallLogMapper;
import com.um.apicenter.calllog.ApiCallLogService;
import com.um.apicenter.common.RequestIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class BusinessApiControllerTest {
    private static final String HASH = new BCryptPasswordEncoder().encode("correct-password");
    private final ApiUserMapper users = mock(ApiUserMapper.class);
    private final ApiDefinitionRuntimeService definitions = mock(ApiDefinitionRuntimeService.class);
    private final DynamicQueryExecutor executor = mock(DynamicQueryExecutor.class);
    private final ApiCallLogMapper logs = mock(ApiCallLogMapper.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ApiDefinition definition = new ApiDefinition();
    private BusinessApiController controller;

    @BeforeEach
    void setup() {
        definition.setId(7L);
        definition.setEnabled(true);
        controller = new BusinessApiController(new BusinessApiAuthenticationService(users), definitions,
                new BusinessParameterValidator(), executor, new ApiCallLogService(logs, mapper), mapper);
    }

    private void register() {
        when(definitions.findByPath("/work/search")).thenReturn(new ApiDefinitionRuntimeService.RuntimeDefinition(
                definition, List.of(
                new ApiParameterDefinition("vendor", ApiParameterType.STRING, true, 20, false, null),
                new ApiParameterDefinition("phone", ApiParameterType.STRING, false, 30, true, null),
                new ApiParameterDefinition("password", ApiParameterType.STRING, false, 30, false, null))));
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api" + path);
        request.setRemoteAddr("192.168.10.5");
        request.setAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE, "test-request-id");
        return request;
    }

    private ApiCallLog storedLog() {
        ArgumentCaptor<ApiCallLog> captor = ArgumentCaptor.forClass(ApiCallLog.class);
        verify(logs).insert(captor.capture());
        return captor.getValue();
    }

    @Test
    void unknownApplicationPathNeverAuthenticatesOrWritesLog() {
        when(definitions.findByPath("/other-app/save")).thenThrow(BusinessApiException.notFound());
        var response = controller.execute("{\"auth\":{\"password\":\"secret\"}}", request("/other-app/save"));
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        verifyNoInteractions(users, executor, logs);
    }

    @Test
    void failedAuthenticationRecordsDefinedParametersAndNotSecrets() {
        register();
        when(users.findByUsername("operator")).thenReturn(new ApiUser(1L, "operator", HASH, true, false));
        var response = controller.execute("""
                {"auth":{"username":"operator","password":"wrong-credential"},
                 "params":{"vendor":"V001","phone":"13800000000","password":"nested-secret",
                           "unknown":"unconfigured-secret"}}
                """, request("/work/search"));
        assertThat(response.getBody().error().code()).isEqualTo("AUTH_FAILED");
        assertThat(response.getBody().error().message()).isEqualTo("认证失败");
        ApiCallLog log = storedLog();
        assertThat(log.getApiId()).isEqualTo(7L);
        assertThat(log.getUsername()).isEqualTo("operator");
        assertThat(log.getParamsSummary()).contains("V001", "***")
                .doesNotContain("13800000000", "nested-secret", "unconfigured-secret", "wrong-credential");
        assertThat(log.getRequestDiagnostics()).contains("AUTH_PASSWORD_MISMATCH", "AUTHENTICATION", "password_present", "unknown_parameter_count")
                .doesNotContain("wrong-credential", HASH, "13800000000");
        assertThat(mapper.writeValueAsString(response.getBody())).doesNotContain("AUTH_PASSWORD_MISMATCH");
        verifyNoInteractions(executor);
    }

    @Test
    void actualJsonHttpRequestUsesControllerParsingAndDoesNotExposeDiagnostics() throws Exception {
        register();
        var result = MockMvcBuilders.standaloneSetup(controller).build().perform(
                post("/api/work/search").contentType(MediaType.APPLICATION_JSON).content("""
                    {"auth":{"username":"missing","password":"http-secret"},"params":{"vendor":"中文编码"}}
                    """)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getContentAsString()).contains("AUTH_FAILED")
                .doesNotContain("AUTH_USER_NOT_FOUND", "http-secret");
        ApiCallLog log = storedLog();
        assertThat(log.getParamsSummary()).contains("中文编码");
        assertThat(log.getRequestDiagnostics()).contains("AUTH_USER_NOT_FOUND").doesNotContain("http-secret");
    }

    @ParameterizedTest
    @CsvSource({"missing,true,AUTH_USER_NOT_FOUND", "valid,false,AUTH_USER_DISABLED", "invalid,true,AUTH_HASH_INVALID"})
    void internalReasonsArePreciseButExternalErrorIsIdentical(String hashType, boolean active, String reason) {
        register();
        if (!hashType.equals("missing")) {
            when(users.findByUsername("operator")).thenReturn(new ApiUser(1L, "operator",
                    hashType.equals("valid") ? HASH : "not-a-hash", active, false));
        }
        var response = controller.execute("""
                {"auth":{"username":"operator","password":"correct-password"},"params":{"vendor":"V001"}}
                """, request("/work/search"));
        assertThat(response.getBody().error().code()).isEqualTo("AUTH_FAILED");
        assertThat(response.getBody().error().message()).isEqualTo("认证失败");
        assertThat(storedLog().getRequestDiagnostics()).contains(reason).doesNotContain("correct-password", HASH);
        verifyNoInteractions(executor);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "{}|AUTH_OBJECT_MISSING",
            "{\"auth\":{\"password\":\"secret\"}}|AUTH_USERNAME_MISSING",
            "{\"auth\":{\"username\":\"operator\"}}|AUTH_PASSWORD_MISSING"
    }, delimiter = '|')
    void missingAuthFieldsHaveUsefulDiagnosticWithoutAccountLookup(String body, String reason) {
        register();
        var response = controller.execute(body, request("/work/search"));
        assertThat(response.getBody().error().code()).isEqualTo("AUTH_FAILED");
        assertThat(storedLog().getRequestDiagnostics()).contains(reason).doesNotContain("secret");
        verifyNoInteractions(users, executor);
    }

    @Test
    void malformedBodyIsLoggedOnlyForConfiguredInterfaceWithoutRawContent() {
        register();
        var response = controller.execute("{\"password\":\"raw-secret", request("/work/search"));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        ApiCallLog log = storedLog();
        assertThat(log.getParamsSummary()).isNull();
        assertThat(log.getRequestDiagnostics()).contains("REQUEST_BODY_INVALID").doesNotContain("raw-secret");
        verifyNoInteractions(users, executor);
    }

    private String validBody() {
        when(users.findByUsername("operator")).thenReturn(new ApiUser(1L, "operator", HASH, true, false));
        return """
                {"auth":{"username":"operator","password":"correct-password"},"params":{"vendor":"V001"}}
                """;
    }

    @Test
    void disabledConfiguredInterfaceStillProducesLogAndDoesNotQuery() {
        register();
        definition.setEnabled(false);
        var response = controller.execute(validBody(), request("/work/search"));
        assertThat(response.getBody().error().code()).isEqualTo("API_DISABLED");
        assertThat(storedLog().getRequestDiagnostics()).contains("INTERFACE_STATE");
        verifyNoInteractions(executor);
    }

    @Test
    void invalidParametersStillHaveSummaryAndValidationMessage() {
        register();
        String body = validBody().replace("\"V001\"", "12");
        var response = controller.execute(body, request("/work/search"));
        assertThat(response.getBody().error().code()).isEqualTo("PARAMETER_INVALID");
        ApiCallLog log = storedLog();
        assertThat(log.getParamsSummary()).contains("12");
        assertThat(log.getRequestDiagnostics()).contains("PARAMETER_VALIDATION", "vendor");
        verifyNoInteractions(executor);
    }

    @Test
    void validNonAdminUserCanQueryAndSuccessIsLogged() {
        register();
        when(executor.execute(any(), any())).thenReturn(new DynamicQueryExecutor.QueryResult(List.of(Map.of("vendor", "V001")), false));
        var response = controller.execute(validBody(), request("/work/search"));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        ApiCallLog log = storedLog();
        assertThat(log.getResult()).isEqualTo("success");
        assertThat(log.getRecordCount()).isEqualTo(1);
        assertThat(log.getRequestDiagnostics()).contains("COMPLETED").doesNotContain("correct-password", HASH);
    }

    @Test
    void timeoutHasStableCodeAndRetainsParameters() {
        register();
        when(executor.execute(any(), any())).thenThrow(BusinessApiException.timeout());
        var response = controller.execute(validBody(), request("/work/search"));
        assertThat(response.getBody().error().code()).isEqualTo("QUERY_TIMEOUT");
        assertThat(storedLog().getRequestDiagnostics()).contains("QUERY_EXECUTION", "QUERY_TIMEOUT");
    }
}
