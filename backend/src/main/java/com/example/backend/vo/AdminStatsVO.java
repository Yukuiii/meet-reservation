package com.example.backend.vo;

import lombok.Data;

/**
 * 管理员统计概览对象。
 */
@Data
public class AdminStatsVO {

    /**
     * 用户总数。
     */
    private Long totalUsers;

    /**
     * 会议室总数。
     */
    private Long totalRooms;

    /**
     * 正常会议室数。
     */
    private Long normalRooms;

    /**
     * 维护中会议室数。
     */
    private Long maintenanceRooms;

    /**
     * 停用会议室数。
     */
    private Long disabledRooms;

    /**
     * 预约总数。
     */
    private Long totalReservations;

    /**
     * 待审核预约数。
     */
    private Long pendingReservations;

    /**
     * 已通过预约数。
     */
    private Long approvedReservations;

    /**
     * 已拒绝预约数。
     */
    private Long rejectedReservations;

    /**
     * 已取消预约数。
     */
    private Long cancelledReservations;

    /**
     * 今日预约数。
     */
    private Long todayReservations;
}
