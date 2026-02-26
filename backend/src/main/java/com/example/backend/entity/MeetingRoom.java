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
 * 会议室实体，对应 meeting_room 表。
 */
@Data
@TableName("meeting_room")
public class MeetingRoom implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会议室ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会议室名称。
     */
    @TableField("name")
    private String name;

    /**
     * 容纳人数。
     */
    @TableField("capacity")
    private Integer capacity;

    /**
     * 位置描述。
     */
    @TableField("location")
    private String location;

    /**
     * 所属楼栋。
     */
    @TableField("building")
    private String building;

    /**
     * 所在楼层。
     */
    @TableField("floor")
    private String floor;

    /**
     * 会议室描述。
     */
    @TableField("description")
    private String description;

    /**
     * 封面图片URL。
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 状态：0-停用，1-正常，2-维护中。
     */
    @TableField("status")
    private Integer status;

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

    /**
     * 更新时间。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
