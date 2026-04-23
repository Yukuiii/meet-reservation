package com.example.backend.vo;

import lombok.Data;

/**
 * 站内通知展示对象。
 */
@Data
public class NotificationVO {

    /**
     * 通知ID。
     */
    private Long id;

    /**
     * 通知标题。
     */
    private String title;

    /**
     * 通知内容。
     */
    private String content;

    /**
     * 通知类型：0-系统通知，1-紧急占用取消，2-审核通过，3-审核驳回。
     */
    private Integer type;

    /**
     * 通知类型文本。
     */
    private String typeText;

    /**
     * 关联预约ID。
     */
    private Long reservationId;

    /**
     * 是否已读。
     */
    private Boolean isRead;

    /**
     * 创建时间。
     */
    private String createdAt;
}
