package com.example.backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 管理员紧急占用请求参数。
 */
@Data
public class AdminEmergencyOccupyRequest {

    /**
     * 管理员用户ID。
     */
    private Long adminUserId;

    /**
     * 会议室ID。
     */
    private Long roomId;

    /**
     * 占用日期。
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

    /**
     * 占用主题。
     */
    private String title;

    /**
     * 占用说明。
     */
    private String purpose;

    /**
     * 是否强制协调冲突。
     */
    private Boolean forceOverride;

    /**
     * 被取消预约的原因文案。
     */
    private String cancelReason;
}
