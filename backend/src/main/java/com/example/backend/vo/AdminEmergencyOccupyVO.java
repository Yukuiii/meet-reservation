package com.example.backend.vo;

import lombok.Data;

/**
 * 管理员紧急占用结果对象。
 */
@Data
public class AdminEmergencyOccupyVO {

    /**
     * 新占用预约ID。
     */
    private Long reservationId;

    /**
     * 新占用预约编号。
     */
    private String reservationNo;

    /**
     * 冲突预约数量。
     */
    private Integer conflictCount;

    /**
     * 被协调取消的预约数量。
     */
    private Integer cancelledCount;

    /**
     * 占用状态文案。
     */
    private String statusText;
}
