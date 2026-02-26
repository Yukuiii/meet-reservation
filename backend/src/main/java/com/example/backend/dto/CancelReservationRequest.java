package com.example.backend.dto;

import lombok.Data;

/**
 * 取消预约请求参数。
 */
@Data
public class CancelReservationRequest {

    /**
     * 当前用户ID。
     */
    private Long userId;

    /**
     * 取消原因。
     */
    private String cancelReason;
}
