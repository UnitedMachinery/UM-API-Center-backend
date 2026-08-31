package com.um.apicenter.datasource;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ManagedDataSourceMapper {

    @Select("""
            SELECT id, name, db_type AS dbType, host, port, database_name AS databaseName,
                   username, password_encrypted AS passwordEncrypted,
                   connection_options AS connectionOptions, is_enabled AS enabled, remark,
                   created_by AS createdBy, created_at AS createdAt,
                   updated_by AS updatedBy, updated_at AS updatedAt
            FROM api_datasource
            ORDER BY name
            """)
    List<ManagedDataSource> findAll();

    @Select("""
            SELECT id, name, db_type AS dbType, host, port, database_name AS databaseName,
                   username, password_encrypted AS passwordEncrypted,
                   connection_options AS connectionOptions, is_enabled AS enabled, remark,
                   created_by AS createdBy, created_at AS createdAt,
                   updated_by AS updatedBy, updated_at AS updatedAt
            FROM api_datasource
            WHERE id = #{id}
            """)
    ManagedDataSource findById(@Param("id") long id);

    @Insert("""
            INSERT INTO api_datasource (
                name, db_type, host, port, database_name, username, password_encrypted,
                connection_options, is_enabled, remark, created_by, updated_by
            ) VALUES (
                #{name}, #{dbType}, #{host}, #{port}, #{databaseName}, #{username}, #{passwordEncrypted},
                #{connectionOptions}, #{enabled}, #{remark}, #{createdBy}, #{updatedBy}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ManagedDataSource dataSource);

    @Update("""
            UPDATE api_datasource
            SET name = #{name}, db_type = #{dbType}, host = #{host}, port = #{port},
                database_name = #{databaseName}, username = #{username},
                password_encrypted = #{passwordEncrypted}, connection_options = #{connectionOptions},
                is_enabled = #{enabled}, remark = #{remark}, updated_by = #{updatedBy}
            WHERE id = #{id}
            """)
    int update(ManagedDataSource dataSource);

    @Update("""
            UPDATE api_datasource
            SET is_enabled = #{enabled}, updated_by = #{updatedBy}
            WHERE id = #{id}
            """)
    int updateEnabled(@Param("id") long id, @Param("enabled") boolean enabled, @Param("updatedBy") String updatedBy);
}
