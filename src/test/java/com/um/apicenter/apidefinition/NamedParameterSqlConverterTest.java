package com.um.apicenter.apidefinition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NamedParameterSqlConverterTest {

    private final NamedParameterSqlConverter converter = new NamedParameterSqlConverter();

    @Test
    void convertsOnlyNamedParametersOutsideStringsAndComments() {
        String sql = "SELECT ':ignore' AS label FROM vendor -- :comment\nWHERE code = :vendor_code";

        assertEquals("SELECT ':ignore' AS label FROM vendor -- :comment\nWHERE code = #{vendor_code}",
                converter.toMyBatisSql(sql));
    }
}
