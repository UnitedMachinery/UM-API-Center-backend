package com.um.apicenter.apidefinition;

public record ApiParameterDefinition(
        String name,
        ApiParameterType type,
        boolean required,
        Integer maxLength,
        boolean maskedInLog,
        String description) {
}
