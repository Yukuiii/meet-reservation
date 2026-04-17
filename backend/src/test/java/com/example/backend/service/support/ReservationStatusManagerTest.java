package com.example.backend.service.support;

import com.example.backend.entity.Reservation;
import com.example.backend.mapper.ReservationMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * 预约状态管理器测试。
 */
class ReservationStatusManagerTest {

    /**
     * 已结束预约应被识别为不可操作。
     */
    @Test
    void shouldMarkPastReservationAsEnded() {
        ReservationStatusManager manager = new ReservationStatusManager(mock(ReservationMapper.class));
        Reservation reservation = buildReservation(1, LocalDate.now().minusDays(1), LocalTime.of(18, 0));

        assertTrue(manager.isReservationEnded(reservation));
        assertFalse(manager.canCancelReservation(reservation));
        assertFalse(manager.canReviewReservation(reservation));
    }

    /**
     * 未结束预约应按状态允许取消或审核。
     */
    @Test
    void shouldAllowOperationsForFutureReservation() {
        ReservationStatusManager manager = new ReservationStatusManager(mock(ReservationMapper.class));
        Reservation pendingReservation = buildReservation(0, LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        Reservation approvedReservation = buildReservation(1, LocalDate.now().plusDays(1), LocalTime.of(11, 0));

        assertFalse(manager.isReservationEnded(pendingReservation));
        assertTrue(manager.canCancelReservation(pendingReservation));
        assertTrue(manager.canReviewReservation(pendingReservation));

        assertTrue(manager.canCancelReservation(approvedReservation));
        assertFalse(manager.canReviewReservation(approvedReservation));
    }

    /**
     * 今天已开始的时段应被识别为不可新建。
     */
    @Test
    void shouldDetectStartedReservationSlotToday() {
        ReservationStatusManager manager = new ReservationStatusManager(mock(ReservationMapper.class));

        assertTrue(manager.hasReservationStarted(LocalDate.now(), LocalTime.MIN));
        assertFalse(manager.hasReservationStarted(LocalDate.now(), LocalTime.MAX));
    }

    /**
     * 构造预约实体。
     *
     * @param status          状态
     * @param reservationDate 预约日期
     * @param endTime         结束时间
     * @return 预约实体
     */
    private Reservation buildReservation(int status, LocalDate reservationDate, LocalTime endTime) {
        Reservation reservation = new Reservation();
        reservation.setStatus(status);
        reservation.setReservationDate(reservationDate);
        reservation.setEndTime(endTime);
        return reservation;
    }
}
