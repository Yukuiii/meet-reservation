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
 * 会议室图片实体，对应 room_image 表。
 */
@Data
@TableName("room_image")
public class RoomImage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图片ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会议室ID。
     */
    @TableField("room_id")
    private Long roomId;

    /**
     * 图片URL。
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 排序权重。
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
