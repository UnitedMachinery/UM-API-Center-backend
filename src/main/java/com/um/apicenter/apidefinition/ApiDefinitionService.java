package com.um.apicenter.apidefinition;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.um.apicenter.admin.AdminPrincipal;
import com.um.apicenter.datasource.ManagedDataSource;
import com.um.apicenter.datasource.ManagedDataSourceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApiDefinitionService {

    private static final TypeReference<List<ApiParameterDefinition>> PARAMETER_LIST = new TypeReference<>() { };

    private final ApiDefinitionMapper definitionMapper;
    private final ManagedDataSourceMapper dataSourceMapper;
    private final ApiDefinitionValidator validator;
    private final ObjectMapper objectMapper;

    public ApiDefinitionService(ApiDefinitionMapper definitionMapper, ManagedDataSourceMapper dataSourceMapper,
                                ApiDefinitionValidator validator, ObjectMapper objectMapper) {
        this.definitionMapper = definitionMapper;
        this.dataSourceMapper = dataSourceMapper;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public List<ApiDefinition> list() {
        return definitionMapper.findAll();
    }

    public ApiDefinition get(long id) {
        return require(id);
    }

    @Transactional
    public ApiDefinition create(ApiDefinitionInput input, AdminPrincipal administrator) {
        validateInput(input);
        ApiDefinition definition = fromInput(input, administrator.username());
        definitionMapper.insert(definition);
        return require(definition.getId());
    }

    @Transactional
    public ApiDefinition update(long id, ApiDefinitionInput input, AdminPrincipal administrator) {
        require(id);
        validateInput(input);
        ApiDefinition definition = fromInput(input, administrator.username());
        definition.setId(id);
        if (definitionMapper.update(definition) == 0) {
            throw new ApiDefinitionNotFoundException();
        }
        return require(id);
    }

    @Transactional
    public ApiDefinition setEnabled(long id, boolean enabled, AdminPrincipal administrator) {
        if (enabled) {
            ApiDefinition existing = require(id);
            requireEnabledDataSource(existing.getDatasourceId());
        }
        if (definitionMapper.updateEnabled(id, enabled, administrator.username()) == 0) {
            throw new ApiDefinitionNotFoundException();
        }
        return require(id);
    }

    public List<ApiParameterDefinition> readParameters(ApiDefinition definition) {
        try {
            return objectMapper.readValue(definition.getParamsSchema(), PARAMETER_LIST);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored API parameter schema is invalid", exception);
        }
    }

    private void validateInput(ApiDefinitionInput input) {
        requireEnabledDataSource(input.datasourceId());
        validator.validate(input.apiPath(), input.parameters(), input.sqlText());
    }

    private void requireEnabledDataSource(long datasourceId) {
        ManagedDataSource dataSource = dataSourceMapper.findById(datasourceId);
        if (dataSource == null || !dataSource.isEnabled()) {
            throw new ApiDefinitionValidationException("请选择已启用的数据源");
        }
    }

    private ApiDefinition require(long id) {
        ApiDefinition definition = definitionMapper.findById(id);
        if (definition == null) {
            throw new ApiDefinitionNotFoundException();
        }
        return definition;
    }

    private ApiDefinition fromInput(ApiDefinitionInput input, String administrator) {
        ApiDefinition definition = new ApiDefinition();
        definition.setName(input.name().trim());
        definition.setApiPath(input.apiPath().trim());
        definition.setDatasourceId(input.datasourceId());
        definition.setParamsSchema(writeParameters(input.parameters()));
        definition.setSqlText(input.sqlText().trim());
        definition.setTimeoutSeconds(input.timeoutSeconds());
        definition.setMaxRows(input.maxRows());
        definition.setEnabled(input.enabled());
        definition.setRemark(blankToNull(input.remark()));
        definition.setCreatedBy(administrator);
        definition.setUpdatedBy(administrator);
        return definition;
    }

    private String writeParameters(List<ApiParameterDefinition> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (JacksonException exception) {
            throw new IllegalStateException("API parameter schema cannot be serialized", exception);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record ApiDefinitionInput(String name, String apiPath, long datasourceId,
                                     List<ApiParameterDefinition> parameters, String sqlText,
                                     int timeoutSeconds, int maxRows, boolean enabled, String remark) {
    }
}
