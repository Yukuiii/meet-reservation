package com.example.backend.vo;

import lombok.Data;

/**
 * 报修设备选项对象。
 */
@Data
public class RepairEquipmentOptionVO {

    /**
     * 设备ID。
     */
    private Long id;

    /**
     * 设备名称。
     */
    private String name;

    /**
     * 设备描述。
     */
    private String description;

    /**
     * 会议室内设备数量。
     */
    private Integer quantity;
}
