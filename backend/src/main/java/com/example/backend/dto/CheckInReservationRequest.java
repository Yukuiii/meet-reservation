package com.example.backend.dto;

import lombok.Data;

/**
 * 预约签到请求参数。
 */
@Data
public class CheckInReservationRequest {

    /**
     * 当前用户ID。
     */
    private Long userId;
}
