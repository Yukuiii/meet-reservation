package com.example.backend.dto;

import lombok.Data;

/**
 * 管理员保存设备请求参数。
 */
@Data
public class AdminSaveEquipmentRequest {

    /**
     * 管理员用户ID。
     */
    private Long adminUserId;

    /**
     * 设备名称。
     */
    private String name;

    /**
     * 设备图标URL。
     */
    private String icon;

    /**
     * 设备描述。
     */
    private String description;

    /**
     * 状态：0-停用，1-正常。
     */
    private Integer status;
}
