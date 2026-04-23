package com.example.backend.dto;

import lombok.Data;

/**
 * 保存预约规则请求参数。
 */
@Data
public class SaveReservationRuleRequest {

    /**
     * 管理员用户ID。
     */
    private Long adminUserId;

    /**
     * 单次最大预约时长，单位分钟。
     */
    private Integer maxDurationMinutes;

    /**
     * 最少提前预约时间，单位分钟。
     */
    private Integer minAdvanceMinutes;
}
