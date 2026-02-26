package com.example.backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 创建预约请求参数。
 */
@Data
public class CreateReservationRequest {

    /**
     * 预约用户ID。
     */
    private Long userId;

    /**
     * 会议室ID。
     */
    private Long roomId;

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
     * 预约日期。
     */
    private LocalDate reservationDate;

    /**
     * 开始时间。
     */
    private LocalTime startTime;

    /**
     * 结束时间。
     */
    private LocalTime endTime;
}
