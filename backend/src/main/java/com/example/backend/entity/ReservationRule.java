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
 * 预约规则实体，对应 reservation_rule 表。
 */
@Data
@TableName("reservation_rule")
public class ReservationRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 单次最大预约时长，单位分钟。
     */
    @TableField("max_duration_minutes")
    private Integer maxDurationMinutes;

    /**
     * 最少提前预约时间，单位分钟。
     */
    @TableField("min_advance_minutes")
    private Integer minAdvanceMinutes;

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
