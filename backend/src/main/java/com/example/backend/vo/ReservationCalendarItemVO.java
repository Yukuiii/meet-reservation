package com.example.backend.vo;

import lombok.Data;

/**
 * 日历视图预约项。
 */
@Data
public class ReservationCalendarItemVO {

    /**
     * 预约ID。
     */
    private Long id;

    /**
     * 预约编号。
     */
    private String reservationNo;

    /**
     * 会议室ID。
     */
    private Long roomId;

    /**
     * 会议室名称。
     */
    private String roomName;

    /**
     * 预约日期，格式：yyyy-MM-dd。
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
}
