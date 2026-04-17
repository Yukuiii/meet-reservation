package com.example.backend.vo;

import lombok.Data;

/**
 * 管理员视角预约记录对象。
 */
@Data
public class AdminReservationVO {

    /**
     * 预约ID。
     */
    private Long id;

    /**
     * 预约编号。
     */
    private String reservationNo;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 用户昵称。
     */
    private String nickname;

    /**
     * 会议室ID。
     */
    private Long roomId;

    /**
     * 会议室名称。
     */
    private String roomName;

    /**
     * 日期，格式：yyyy-MM-dd。
     */
    private String date;

    /**
     * 开始时间，格式：HH:mm。
     */
    private String startTime;

    /**
     * 结束时间，格式：HH:mm。
     */
    private String endTime;

    /**
     * 时段文本，格式：HH:mm-HH:mm。
     */
    private String timeSlot;

    /**
     * 会议主题。
     */
    private String title;

    /**
     * 预约事由。
     */
    private String purpose;

    /**
     * 参与人数。
     */
    private Integer attendeeCount;

    /**
     * 状态码。
     */
    private Integer status;

    /**
     * 状态键。
     */
    private String statusKey;

    /**
     * 状态文案。
     */
    private String statusText;

    /**
     * 是否允许审核。
     */
    private Boolean canReview;
}
