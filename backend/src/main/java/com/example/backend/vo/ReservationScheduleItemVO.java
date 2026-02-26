package com.example.backend.vo;

import lombok.Data;

/**
 * 会议室占用时段视图对象。
 */
@Data
public class ReservationScheduleItemVO {

    /**
     * 预约ID。
     */
    private Long id;

    /**
     * 预约编号。
     */
    private String reservationNo;

    /**
     * 开始时间，格式：HH:mm。
     */
    private String startTime;

    /**
     * 结束时间，格式：HH:mm。
     */
    private String endTime;

    /**
     * 预约状态。
     */
    private Integer status;

    /**
     * 状态文案。
     */
    private String statusText;

    /**
     * 会议主题。
     */
    private String title;
}
