package com.example.backend.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员批量审核结果对象。
 */
@Data
public class AdminBatchReviewResultVO {

    /**
     * 本次处理总数。
     */
    private Integer totalCount = 0;

    /**
     * 成功处理数量。
     */
    private Integer successCount = 0;

    /**
     * 通过数量。
     */
    private Integer approvedCount = 0;

    /**
     * 驳回数量。
     */
    private Integer rejectedCount = 0;

    /**
     * 失败数量。
     */
    private Integer failedCount = 0;

    /**
     * 失败明细。
     */
    private List<String> failureMessages = new ArrayList<>();
}
