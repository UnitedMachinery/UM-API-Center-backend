package com.um.apicenter.apidefinition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessParameterValidatorTest {

    private final BusinessParameterValidator validator = new BusinessParameterValidator();
    private final List<ApiParameterDefinition> definitions = List.of(
            new ApiParameterDefinition("vendor_code", ApiParameterType.STRING, true, 10, false, null),
            new ApiParameterDefinition("phone", ApiParameterType.STRING, false, 30, true, null),
            new ApiParameterDefinition("page", ApiParameterType.INTEGER, false, null, false, null));

    @Test
    void acceptsDefinedAndCorrectlyTypedParameters() {
        Map<String, Object> result = validator.validate(Map.of("vendor_code", "V001", "page", 2), definitions);

        assertEquals("V001", result.get("vendor_code"));
        assertEquals(2L, result.get("page"));
    }

    @Test
    void rejectsUnknownMissingAndInvalidParameters() {
        assertThrows(BusinessApiException.class, () -> validator.validate(Map.of("unknown", "x"), definitions));
        assertThrows(BusinessApiException.class, () -> validator.validate(Map.of(), definitions));
        assertThrows(BusinessApiException.class, () -> validator.validate(Map.of("vendor_code", "V001", "page", "2"), definitions));
    }

    @Test
    void masksConfiguredLogParameters() {
        Map<String, Object> summary = validator.maskedSummary(
                Map.of("vendor_code", "V001", "phone", "13800000000"), definitions);

        assertEquals("V001", summary.get("vendor_code"));
        assertEquals("***", summary.get("phone"));
    }

    @Test
    void rawInvalidParametersCannotLeakNestedCredentialsOrGrowSummaryWithoutBound() {
        Map<String, Object> summary = validator.maskedSummary(
                Map.of("vendor_code", Map.of("password", "secret"), "phone", "private-phone", "page", "x".repeat(1000)), definitions);
        assertEquals("[非标量值，内容未记录]", summary.get("vendor_code"));
        assertEquals("***", summary.get("phone"));
        assertEquals(512 + "…（已截断）".length(), ((String) summary.get("page")).length());
    }
}
