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
 * 设备报修实体，对应 equipment_repair 表。
 */
@Data
@TableName("equipment_repair")
public class EquipmentRepair implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 报修ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 报修编号。
     */
    @TableField("repair_no")
    private String repairNo;

    /**
     * 报修用户ID。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 关联预约ID。
     */
    @TableField("reservation_id")
    private Long reservationId;

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
     * 故障描述。
     */
    @TableField("description")
    private String description;

    /**
     * 状态：0-待处理，1-已修复。
     */
    @TableField("status")
    private Integer status;

    /**
     * 修复管理员ID。
     */
    @TableField("fixed_by")
    private Long fixedBy;

    /**
     * 修复时间。
     */
    @TableField("fixed_at")
    private LocalDateTime fixedAt;

    /**
     * 修复备注。
     */
    @TableField("fix_remark")
    private String fixRemark;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
