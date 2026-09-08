package com.um.apicenter.apidefinition;

import org.springframework.stereotype.Service;

@Service
public class ApiDefinitionRuntimeService {

    private final ApiDefinitionMapper definitionMapper;
    private final ApiDefinitionService definitionService;

    public ApiDefinitionRuntimeService(ApiDefinitionMapper definitionMapper, ApiDefinitionService definitionService) {
        this.definitionMapper = definitionMapper;
        this.definitionService = definitionService;
    }

    public RuntimeDefinition findByPath(String apiPath) {
        ApiDefinition definition = definitionMapper.findByPath(apiPath);
        if (definition == null) {
            throw BusinessApiException.notFound();
        }
        if (!definition.isEnabled()) {
            throw BusinessApiException.disabled();
        }
        return new RuntimeDefinition(definition, definitionService.readParameters(definition));
    }

    public record RuntimeDefinition(ApiDefinition definition, java.util.List<ApiParameterDefinition> parameters) {
    }
}
