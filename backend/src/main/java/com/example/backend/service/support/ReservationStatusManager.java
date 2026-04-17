package com.example.backend.service.support;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.entity.Reservation;
import com.example.backend.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约状态管理器。
 */
@Service
@RequiredArgsConstructor
public class ReservationStatusManager {

    /**
     * 待审核状态。
     */
    private static final int STATUS_PENDING = 0;

    /**
     * 已通过状态。
     */
    private static final int STATUS_APPROVED = 1;

    /**
     * 已取消状态。
     */
    private static final int STATUS_CANCELLED = 3;

    /**
     * 已完成状态。
     */
    private static final int STATUS_FINISHED = 4;

    /**
     * 过期待审核预约的系统取消原因。
     */
    private static final String AUTO_CANCEL_REASON = "预约时段已结束，系统自动关闭";

    private final ReservationMapper reservationMapper;

    /**
     * 收口已过期的预约状态。
     */
    public void refreshExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        finishExpiredApprovedReservations(today, currentTime, now);
        cancelExpiredPendingReservations(today, currentTime, now);
    }

    /**
     * 判断预约是否已结束。
     *
     * @param reservation 预约实体
     * @return 是否已结束
     */
    public boolean isReservationEnded(Reservation reservation) {
        if (reservation == null
                || reservation.getReservationDate() == null
                || reservation.getEndTime() == null) {
            return false;
        }
        LocalDateTime reservationEndTime = LocalDateTime.of(
                reservation.getReservationDate(),
                reservation.getEndTime()
        );
        return !reservationEndTime.isAfter(LocalDateTime.now());
    }

    /**
     * 判断预约是否已经到达开始时间。
     *
     * @param reservationDate 预约日期
     * @param startTime       开始时间
     * @return 是否已开始
     */
    public boolean hasReservationStarted(LocalDate reservationDate, LocalTime startTime) {
        if (reservationDate == null || startTime == null) {
            return false;
        }
        LocalDateTime reservationStartTime = LocalDateTime.of(reservationDate, startTime);
        return !reservationStartTime.isAfter(LocalDateTime.now());
    }

    /**
     * 判断预约当前是否允许取消。
     *
     * @param reservation 预约实体
     * @return 是否允许取消
     */
    public boolean canCancelReservation(Reservation reservation) {
        if (reservation == null) {
            return false;
        }
        Integer status = reservation.getStatus();
        return (Integer.valueOf(STATUS_PENDING).equals(status)
                || Integer.valueOf(STATUS_APPROVED).equals(status))
                && !isReservationEnded(reservation);
    }

    /**
     * 判断预约当前是否允许审核。
     *
     * @param reservation 预约实体
     * @return 是否允许审核
     */
    public boolean canReviewReservation(Reservation reservation) {
        return reservation != null
                && Integer.valueOf(STATUS_PENDING).equals(reservation.getStatus())
                && !isReservationEnded(reservation);
    }

    /**
     * 将已结束的已通过预约转为已完成。
     *
     * @param today       当前日期
     * @param currentTime 当前时间
     * @param now         当前时间戳
     */
    private void finishExpiredApprovedReservations(LocalDate today, LocalTime currentTime, LocalDateTime now) {
        Reservation updateEntity = new Reservation();
        updateEntity.setStatus(STATUS_FINISHED);
        updateEntity.setUpdatedAt(now);

        reservationMapper.update(
                updateEntity,
                new LambdaUpdateWrapper<Reservation>()
                        .eq(Reservation::getStatus, STATUS_APPROVED)
                        .and(wrapper -> wrapper
                                .lt(Reservation::getReservationDate, today)
                                .or(dateWrapper -> dateWrapper
                                        .eq(Reservation::getReservationDate, today)
                                        .le(Reservation::getEndTime, currentTime)))
        );
    }

    /**
     * 将已结束的待审核预约自动关闭为已取消。
     *
     * @param today       当前日期
     * @param currentTime 当前时间
     * @param now         当前时间戳
     */
    private void cancelExpiredPendingReservations(LocalDate today, LocalTime currentTime, LocalDateTime now) {
        Reservation updateEntity = new Reservation();
        updateEntity.setStatus(STATUS_CANCELLED);
        updateEntity.setCancelReason(AUTO_CANCEL_REASON);
        updateEntity.setCancelledAt(now);
        updateEntity.setUpdatedAt(now);

        reservationMapper.update(
                updateEntity,
                new LambdaUpdateWrapper<Reservation>()
                        .eq(Reservation::getStatus, STATUS_PENDING)
                        .and(wrapper -> wrapper
                                .lt(Reservation::getReservationDate, today)
                                .or(dateWrapper -> dateWrapper
                                        .eq(Reservation::getReservationDate, today)
                                        .le(Reservation::getEndTime, currentTime)))
                        .and(wrapper -> wrapper
                                .isNull(Reservation::getCancelReason)
                                .or()
                                .eq(Reservation::getCancelReason, ""))
        );

        Reservation reasonFixEntity = new Reservation();
        reasonFixEntity.setCancelReason(AUTO_CANCEL_REASON);
        reasonFixEntity.setCancelledAt(now);
        reasonFixEntity.setUpdatedAt(now);

        // 兜底修正历史数据，确保系统自动关闭的记录具备取消原因和取消时间。
        reservationMapper.update(
                reasonFixEntity,
                new LambdaUpdateWrapper<Reservation>()
                        .eq(Reservation::getStatus, STATUS_CANCELLED)
                        .lt(Reservation::getReservationDate, today)
                        .and(wrapper -> wrapper
                                .isNull(Reservation::getCancelReason)
                                .or()
                                .eq(Reservation::getCancelReason, ""))
        );
    }
}
