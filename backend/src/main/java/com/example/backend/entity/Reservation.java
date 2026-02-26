package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约实体，对应 reservation 表。
 */
@Data
@TableName("reservation")
public class Reservation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 预约ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 预约编号。
     */
    @TableField("reservation_no")
    private String reservationNo;

    /**
     * 预约用户ID。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会议室ID。
     */
    @TableField("room_id")
    private Long roomId;

    /**
     * 会议主题。
     */
    @TableField("title")
    private String title;

    /**
     * 预约事由/会议内容。
     */
    @TableField("purpose")
    private String purpose;

    /**
     * 参与人数。
     */
    @TableField("attendee_count")
    private Integer attendeeCount;

    /**
     * 预约日期。
     */
    @TableField("reservation_date")
    private LocalDate reservationDate;

    /**
     * 开始时间。
     */
    @TableField("start_time")
    private LocalTime startTime;

    /**
     * 结束时间。
     */
    @TableField("end_time")
    private LocalTime endTime;

    /**
     * 状态：0-待审核，1-已通过，2-已拒绝，3-已取消，4-已完成。
     */
    @TableField("status")
    private Integer status;

    /**
     * 拒绝原因。
     */
    @TableField("reject_reason")
    private String rejectReason;

    /**
     * 审核人ID。
     */
    @TableField("reviewer_id")
    private Long reviewerId;

    /**
     * 审核时间。
     */
    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * 取消原因。
     */
    @TableField("cancel_reason")
    private String cancelReason;

    /**
     * 取消时间。
     */
    @TableField("cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * 备注。
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
