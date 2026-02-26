package com.example.backend.vo;

import lombok.Data;

/**
 * 管理员设备选项对象。
 */
@Data
public class AdminEquipmentVO {

    /**
     * 设备ID。
     */
    private Long id;

    /**
     * 设备名称。
     */
    private String name;
}
