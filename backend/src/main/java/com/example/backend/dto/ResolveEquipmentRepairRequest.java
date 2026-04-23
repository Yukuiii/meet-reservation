package com.example.backend.dto;

import lombok.Data;

/**
 * 修复设备报修请求参数。
 */
@Data
public class ResolveEquipmentRepairRequest {

    /**
     * 管理员用户ID。
     */
    private Long adminUserId;

    /**
     * 修复备注。
     */
    private String fixRemark;
}
