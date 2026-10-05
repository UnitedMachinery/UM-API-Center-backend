package com.um.apicenter.calllog;

import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiCallLogMapperTest {
    @Test
    void listAndCountUseSameScopeAndParameterizedFilters() {
        Configuration configuration = new Configuration();
        configuration.addMapper(ApiCallLogMapper.class);
        Map<String, Object> parameters = Map.of("filter",
                new ApiCallLogService.LogFilter(null, null, "operator", 7L, "failed"), "limit", 20, "offset", 0);
        for (String method : new String[]{"findPage", "count"}) {
            var sql = configuration.getMappedStatement(ApiCallLogMapper.class.getName() + "." + method)
                    .getBoundSql(parameters);
            assertThat(sql.getSql()).contains("api_id IS NOT NULL OR EXISTS", "d.api_path = api_call_log.api_path",
                    "username = ?", "api_id = ?", "result = ?").doesNotContain("operator");
        }
    }

    @Test
    void insertIncludesJsonDiagnostics() {
        Configuration configuration = new Configuration();
        configuration.addMapper(ApiCallLogMapper.class);
        var sql = configuration.getMappedStatement(ApiCallLogMapper.class.getName() + ".insert").getBoundSql(new ApiCallLog());
        assertThat(sql.getSql()).contains("request_diagnostics");
        assertThat(sql.getParameterMappings()).anyMatch(mapping -> mapping.getProperty().equals("requestDiagnostics"));
    }
}
