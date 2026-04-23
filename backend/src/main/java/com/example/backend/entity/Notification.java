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
 * 站内通知实体，对应 notification 表。
 */
@Data
@TableName("notification")
public class Notification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 通知ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 接收通知的用户ID。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 通知标题。
     */
    @TableField("title")
    private String title;

    /**
     * 通知内容。
     */
    @TableField("content")
    private String content;

    /**
     * 通知类型：0-系统通知，1-紧急占用取消，2-审核通过，3-审核驳回。
     */
    @TableField("type")
    private Integer type;

    /**
     * 关联预约ID。
     */
    @TableField("reservation_id")
    private Long reservationId;

    /**
     * 是否已读：0-未读，1-已读。
     */
    @TableField("is_read")
    private Integer isRead;

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
