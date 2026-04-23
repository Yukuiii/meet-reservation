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
 * 预约改约推荐实体，对应 reservation_recommendation 表。
 */
@Data
@TableName("reservation_recommendation")
public class ReservationRecommendation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 推荐ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联通知ID。
     */
    @TableField("notification_id")
    private Long notificationId;

    /**
     * 原预约ID。
     */
    @TableField("original_reservation_id")
    private Long originalReservationId;

    /**
     * 接收推荐的用户ID。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 触发推荐的管理员ID。
     */
    @TableField("admin_user_id")
    private Long adminUserId;

    /**
     * 推荐会议室ID。
     */
    @TableField("recommended_room_id")
    private Long recommendedRoomId;

    /**
     * 推荐日期。
     */
    @TableField("reservation_date")
    private LocalDate reservationDate;

    /**
     * 推荐开始时间。
     */
    @TableField("start_time")
    private LocalTime startTime;

    /**
     * 推荐结束时间。
     */
    @TableField("end_time")
    private LocalTime endTime;

    /**
     * 状态：0-待处理，1-已接受，2-已放弃。
     */
    @TableField("status")
    private Integer status;

    /**
     * 接受后生成的新预约ID。
     */
    @TableField("accepted_reservation_id")
    private Long acceptedReservationId;

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
