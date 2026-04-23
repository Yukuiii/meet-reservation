package com.example.backend.dto;

import lombok.Data;

import java.util.List;

/**
 * 管理员批量审核预约请求参数。
 */
@Data
public class AdminBatchReviewReservationRequest {

    /**
     * 管理员用户ID。
     */
    private Long adminUserId;

    /**
     * 预约ID列表。
     */
    private List<Long> reservationIds;

    /**
     * 审核结果：true-通过，false-驳回。
     */
    private Boolean approved;

    /**
     * 驳回原因。
     */
    private String rejectReason;
}
