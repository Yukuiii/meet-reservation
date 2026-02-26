package com.example.backend.vo;

import lombok.Data;

/**
 * 创建预约响应对象。
 */
@Data
public class CreateReservationResponseVO {

    /**
     * 预约ID。
     */
    private Long id;

    /**
     * 预约编号。
     */
    private String reservationNo;

    /**
     * 预约状态。
     */
    private Integer status;

    /**
     * 状态文案。
     */
    private String statusText;
}
