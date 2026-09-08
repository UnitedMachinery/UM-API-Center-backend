package com.um.apicenter.apidefinition;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ApiDefinitionMapper {

    @Select("""
            SELECT id, name, api_path AS apiPath, datasource_id AS datasourceId,
                   params_schema AS paramsSchema, sql_text AS sqlText,
                   timeout_seconds AS timeoutSeconds, max_rows AS maxRows,
                   is_enabled AS enabled, remark, created_by AS createdBy, created_at AS createdAt,
                   updated_by AS updatedBy, updated_at AS updatedAt
            FROM api_definition
            ORDER BY name
            """)
    List<ApiDefinition> findAll();

    @Select("""
            SELECT id, name, api_path AS apiPath, datasource_id AS datasourceId,
                   params_schema AS paramsSchema, sql_text AS sqlText,
                   timeout_seconds AS timeoutSeconds, max_rows AS maxRows,
                   is_enabled AS enabled, remark, created_by AS createdBy, created_at AS createdAt,
                   updated_by AS updatedBy, updated_at AS updatedAt
            FROM api_definition
            WHERE id = #{id}
            """)
    ApiDefinition findById(@Param("id") long id);

    @Select("""
            SELECT id, name, api_path AS apiPath, datasource_id AS datasourceId,
                   params_schema AS paramsSchema, sql_text AS sqlText,
                   timeout_seconds AS timeoutSeconds, max_rows AS maxRows,
                   is_enabled AS enabled, remark, created_by AS createdBy, created_at AS createdAt,
                   updated_by AS updatedBy, updated_at AS updatedAt
            FROM api_definition
            WHERE api_path = #{apiPath}
            """)
    ApiDefinition findByPath(@Param("apiPath") String apiPath);

    @Insert("""
            INSERT INTO api_definition (
                name, api_path, datasource_id, params_schema, sql_text, timeout_seconds,
                max_rows, is_enabled, remark, created_by, updated_by
            ) VALUES (
                #{name}, #{apiPath}, #{datasourceId}, #{paramsSchema}, #{sqlText}, #{timeoutSeconds},
                #{maxRows}, #{enabled}, #{remark}, #{createdBy}, #{updatedBy}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ApiDefinition definition);

    @Update("""
            UPDATE api_definition
            SET name = #{name}, api_path = #{apiPath}, datasource_id = #{datasourceId},
                params_schema = #{paramsSchema}, sql_text = #{sqlText}, timeout_seconds = #{timeoutSeconds},
                max_rows = #{maxRows}, is_enabled = #{enabled}, remark = #{remark}, updated_by = #{updatedBy}
            WHERE id = #{id}
            """)
    int update(ApiDefinition definition);

    @Update("""
            UPDATE api_definition
            SET is_enabled = #{enabled}, updated_by = #{updatedBy}
            WHERE id = #{id}
            """)
    int updateEnabled(@Param("id") long id, @Param("enabled") boolean enabled, @Param("updatedBy") String updatedBy);
}
