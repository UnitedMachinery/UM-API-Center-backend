package com.um.apicenter.apidefinition;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class BusinessParameterValidator {

    public Map<String, Object> validate(Map<String, Object> requestParameters,
                                        List<ApiParameterDefinition> definitions) {
        Map<String, Object> received = requestParameters == null ? Map.of() : requestParameters;
        Set<String> definedNames = definitions.stream().map(ApiParameterDefinition::name).collect(Collectors.toSet());
        if (!definedNames.containsAll(received.keySet())) {
            throw BusinessApiException.invalidParameter("存在未定义的请求参数");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (ApiParameterDefinition definition : definitions) {
            Object value = received.get(definition.name());
            if (value == null) {
                if (definition.required()) {
                    throw BusinessApiException.invalidParameter("缺少必填参数：" + definition.name());
                }
                result.put(definition.name(), null);
                continue;
            }
            result.put(definition.name(), validateValue(definition, value));
        }
        return result;
    }

    private Object validateValue(ApiParameterDefinition definition, Object value) {
        return switch (definition.type()) {
            case STRING -> validateString(definition, value);
            case INTEGER -> validateInteger(definition, value);
            case DECIMAL -> validateDecimal(definition, value);
            case BOOLEAN -> validateBoolean(definition, value);
            case DATE -> validateDate(definition, value);
        };
    }

    private String validateString(ApiParameterDefinition definition, Object value) {
        if (!(value instanceof String string) || string.length() > definition.maxLength()) {
            throw invalidType(definition.name());
        }
        return string;
    }

    private Number validateInteger(ApiParameterDefinition definition, Object value) {
        if (!(value instanceof Number number) || value instanceof Float || value instanceof Double) {
            throw invalidType(definition.name());
        }
        try {
            BigDecimal decimal = new BigDecimal(number.toString());
            return decimal.longValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            throw invalidType(definition.name());
        }
    }

    private BigDecimal validateDecimal(ApiParameterDefinition definition, Object value) {
        if (!(value instanceof Number number)) {
            throw invalidType(definition.name());
        }
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException exception) {
            throw invalidType(definition.name());
        }
    }

    private Boolean validateBoolean(ApiParameterDefinition definition, Object value) {
        if (!(value instanceof Boolean bool)) {
            throw invalidType(definition.name());
        }
        return bool;
    }

    private LocalDate validateDate(ApiParameterDefinition definition, Object value) {
        if (!(value instanceof String text)) {
            throw invalidType(definition.name());
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException exception) {
            throw invalidType(definition.name());
        }
    }

    private BusinessApiException invalidType(String name) {
        return BusinessApiException.invalidParameter("参数格式不合法：" + name);
    }

    public Map<String, Object> maskedSummary(Map<String, Object> parameters,
                                             List<ApiParameterDefinition> definitions) {
        Map<String, Object> summary = new LinkedHashMap<>();
        for (ApiParameterDefinition definition : definitions) {
            Object value = parameters.get(definition.name());
            String name = definition.name().toLowerCase(Locale.ROOT);
            boolean sensitive = definition.maskedInLog() || name.contains("password") || name.contains("passwd")
                    || name.contains("secret") || name.contains("token");
            Object safeValue = value;
            if (value != null && sensitive) {
                safeValue = "***";
            } else if (value instanceof String text && text.length() > 512) {
                safeValue = text.substring(0, 512) + "…（已截断）";
            } else if (value != null && !(value instanceof String || value instanceof Number
                    || value instanceof Boolean || value instanceof LocalDate)) {
                safeValue = "[非标量值，内容未记录]";
            }
            summary.put(definition.name(), safeValue);
        }
        return summary;
    }
}
