package com.um.apicenter.admin;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ApiUserMapper {

    @Select("""
            SELECT id,
                   username,
                   password_hash AS passwordHash,
                   is_active AS active,
                   is_api_admin AS apiAdmin
            FROM api_user
            WHERE username = #{username}
            """)
    ApiUser findByUsername(@Param("username") String username);
}
