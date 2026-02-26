package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会议室设备关联实体，对应 room_equipment 表。
 */
@Data
@TableName("room_equipment")
public class RoomEquipment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会议室ID。
     */
    @TableField("room_id")
    private Long roomId;

    /**
     * 设备ID。
     */
    @TableField("equipment_id")
    private Long equipmentId;

    /**
     * 设备数量。
     */
    @TableField("quantity")
    private Integer quantity;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
