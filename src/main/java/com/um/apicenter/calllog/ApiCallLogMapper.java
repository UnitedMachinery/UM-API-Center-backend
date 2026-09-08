package com.um.apicenter.calllog;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApiCallLogMapper {

    @Insert("""
            INSERT INTO api_call_log (
                request_id, api_id, api_path, username, client_ip, params_summary,
                result, error_code, record_count, duration_ms
            ) VALUES (
                #{requestId}, #{apiId}, #{apiPath}, #{username}, #{clientIp}, #{paramsSummary},
                #{result}, #{errorCode}, #{recordCount}, #{durationMs}
            )
            """)
    void insert(ApiCallLog log);

    @Select("""
            <script>
            SELECT id, request_id AS requestId, api_id AS apiId, api_path AS apiPath,
                   username, client_ip AS clientIp, params_summary AS paramsSummary,
                   result, error_code AS errorCode, record_count AS recordCount,
                   duration_ms AS durationMs, created_at AS createdAt
            FROM api_call_log
            <where>
              <if test="filter.fromTime != null"> AND created_at <![CDATA[>=]]> #{filter.fromTime}</if>
              <if test="filter.toTime != null"> AND created_at <![CDATA[<=]]> #{filter.toTime}</if>
              <if test="filter.username != null and filter.username != ''"> AND username = #{filter.username}</if>
              <if test="filter.apiId != null"> AND api_id = #{filter.apiId}</if>
              <if test="filter.result != null and filter.result != ''"> AND result = #{filter.result}</if>
            </where>
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ApiCallLog> findPage(@Param("filter") ApiCallLogService.LogFilter filter,
                              @Param("limit") int limit, @Param("offset") int offset);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM api_call_log
            <where>
              <if test="filter.fromTime != null"> AND created_at <![CDATA[>=]]> #{filter.fromTime}</if>
              <if test="filter.toTime != null"> AND created_at <![CDATA[<=]]> #{filter.toTime}</if>
              <if test="filter.username != null and filter.username != ''"> AND username = #{filter.username}</if>
              <if test="filter.apiId != null"> AND api_id = #{filter.apiId}</if>
              <if test="filter.result != null and filter.result != ''"> AND result = #{filter.result}</if>
            </where>
            </script>
            """)
    long count(@Param("filter") ApiCallLogService.LogFilter filter);
}
