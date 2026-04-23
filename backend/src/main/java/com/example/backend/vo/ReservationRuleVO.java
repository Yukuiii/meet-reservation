package com.example.backend.vo;

import lombok.Data;

/**
 * 预约规则视图对象。
 */
@Data
public class ReservationRuleVO {

    /**
     * 单次最大预约时长，单位分钟。
     */
    private Integer maxDurationMinutes;

    /**
     * 最少提前预约时间，单位分钟。
     */
    private Integer minAdvanceMinutes;
}
