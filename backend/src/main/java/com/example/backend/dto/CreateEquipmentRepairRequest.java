package com.example.backend.dto;

import lombok.Data;

/**
 * 创建设备报修请求参数。
 */
@Data
public class CreateEquipmentRepairRequest {

    /**
     * 报修用户ID。
     */
    private Long userId;

    /**
     * 关联预约ID。
     */
    private Long reservationId;

    /**
     * 故障设备ID。
     */
    private Long equipmentId;

    /**
     * 故障描述。
     */
    private String description;
}
