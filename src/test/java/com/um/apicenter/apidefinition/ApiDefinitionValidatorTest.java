package com.um.apicenter.apidefinition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiDefinitionValidatorTest {

    private final ApiDefinitionValidator validator = new ApiDefinitionValidator();
    private final List<ApiParameterDefinition> vendorCode = List.of(
            new ApiParameterDefinition("vendor_code", ApiParameterType.STRING, false, 30, false, "供应商编码"));

    @Test
    void acceptsSingleSelectWithMatchingNamedParameter() {
        assertDoesNotThrow(() -> validator.validate("/qad/vendors/search", vendorCode,
                "SELECT vendor_code FROM qad_vendor WHERE vendor_code = :vendor_code"));
    }

    @Test
    void rejectsWriteOrMultipleStatements() {
        assertThrows(ApiDefinitionValidationException.class, () -> validator.validate("/qad/vendors/search", vendorCode,
                "SELECT vendor_code FROM qad_vendor; DELETE FROM qad_vendor"));
        assertThrows(ApiDefinitionValidationException.class, () -> validator.validate("/qad/vendors/search", vendorCode,
                "UPDATE qad_vendor SET vendor_code = :vendor_code"));
    }

    @Test
    void rejectsUndefinedOrUnusedParameter() {
        assertThrows(ApiDefinitionValidationException.class, () -> validator.validate("/qad/vendors/search", vendorCode,
                "SELECT vendor_code FROM qad_vendor WHERE vendor_name = :vendor_name"));
        assertThrows(ApiDefinitionValidationException.class, () -> validator.validate("/qad/vendors/search", vendorCode,
                "SELECT vendor_code FROM qad_vendor"));
    }

    @Test
    void rejectsReservedPathAndInvalidStringLength() {
        assertThrows(ApiDefinitionValidationException.class, () -> validator.validate("/admin/users", vendorCode,
                "SELECT vendor_code FROM qad_vendor WHERE vendor_code = :vendor_code"));
        assertThrows(ApiDefinitionValidationException.class, () -> validator.validate("/qad/vendors/search",
                List.of(new ApiParameterDefinition("vendor_code", ApiParameterType.STRING, false, null, false, null)),
                "SELECT vendor_code FROM qad_vendor WHERE vendor_code = :vendor_code"));
    }
}
