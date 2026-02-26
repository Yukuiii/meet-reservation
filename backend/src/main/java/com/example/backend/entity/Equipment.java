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
 * 设备实体，对应 equipment 表。
 */
@Data
@TableName("equipment")
public class Equipment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 设备名称。
     */
    @TableField("name")
    private String name;

    /**
     * 设备图标URL。
     */
    @TableField("icon")
    private String icon;

    /**
     * 设备描述。
     */
    @TableField("description")
    private String description;

    /**
     * 状态：0-停用，1-正常。
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
