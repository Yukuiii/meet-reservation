package com.example.backend.vo;

import lombok.Data;

/**
 * 管理员设备管理列表项。
 */
@Data
public class AdminEquipmentManageVO {

    /**
     * 设备ID。
     */
    private Long id;

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
     * 状态码：0-停用，1-正常。
     */
    private Integer status;

    /**
     * 状态文案。
     */
    private String statusText;
}
