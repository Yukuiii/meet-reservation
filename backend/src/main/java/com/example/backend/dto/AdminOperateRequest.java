package com.example.backend.dto;

import lombok.Data;

/**
 * 管理员通用操作请求参数。
 */
@Data
public class AdminOperateRequest {

    /**
     * 管理员用户ID。
     */
    private Long adminUserId;
}
