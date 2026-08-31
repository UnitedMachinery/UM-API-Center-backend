package com.um.apicenter.admin;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminSessionMapper {

    @Insert("""
            INSERT INTO api_admin_session (user_id, token_hash, expires_at)
            VALUES (#{userId}, #{tokenHash}, #{expiresAt})
            """)
    void insert(AdminSession session);

    @Select("""
            SELECT u.id AS userId, u.username
            FROM api_admin_session s
            INNER JOIN api_user u ON u.id = s.user_id
            WHERE s.token_hash = #{tokenHash}
              AND s.expires_at > UTC_TIMESTAMP(3)
              AND u.is_active = 1
              AND u.is_api_admin = 1
            """)
    AdminPrincipal findValidAdminByTokenHash(@Param("tokenHash") String tokenHash);

    @Delete("DELETE FROM api_admin_session WHERE token_hash = #{tokenHash}")
    void deleteByTokenHash(@Param("tokenHash") String tokenHash);
}
