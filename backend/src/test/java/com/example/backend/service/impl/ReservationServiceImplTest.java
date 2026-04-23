package com.example.backend.service.impl;

import com.example.backend.dto.CreateReservationRequest;
import com.example.backend.entity.MeetingRoom;
import com.example.backend.entity.Reservation;
import com.example.backend.entity.User;
import com.example.backend.mapper.MeetingRoomMapper;
import com.example.backend.mapper.ReservationMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.ReservationRuleService;
import com.example.backend.service.support.ReservationStatusManager;
import com.example.backend.vo.ReservationCalendarVO;
import com.example.backend.vo.ReservationRuleVO;
import com.example.backend.vo.UserReservationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预约业务实现测试。
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private MeetingRoomMapper meetingRoomMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ReservationStatusManager reservationStatusManager;

    @Mock
    private ReservationRuleService reservationRuleService;

    private ReservationServiceImpl reservationService;

    /**
     * 初始化被测对象。
     */
    @BeforeEach
    void setUp() {
        reservationService = new ReservationServiceImpl(
                reservationMapper,
                meetingRoomMapper,
                userMapper,
                reservationStatusManager,
                reservationRuleService
        );
    }

    /**
     * 已结束预约不允许取消。
     */
    @Test
    void shouldRejectCancelForEndedReservation() {
        Reservation reservation = buildReservation(10L, 1L, 1, LocalDate.now().minusDays(1));
        when(userMapper.selectById(1L)).thenReturn(buildActiveUser());
        when(reservationMapper.selectOne(any())).thenReturn(reservation);
        when(reservationStatusManager.isReservationEnded(reservation)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.cancelReservation(1L, 10L, "用户主动取消")
        );

        assertEquals("预约时段已结束，无法取消", exception.getMessage());
        verify(reservationStatusManager).refreshExpiredReservations();
        verify(reservationMapper, never()).update(any(), any());
    }

    /**
     * 已完成预约应返回完成状态且不可取消。
     */
    @Test
    void shouldReturnFinishedReservationWithoutCancelPermission() {
        Reservation reservation = buildReservation(11L, 1L, 4, LocalDate.now().minusDays(1));
        MeetingRoom room = new MeetingRoom();
        room.setId(1L);
        room.setName("创新会议室");

        when(userMapper.selectById(1L)).thenReturn(buildActiveUser());
        when(reservationMapper.selectList(any())).thenReturn(List.of(reservation));
        when(meetingRoomMapper.selectBatchIds(anyCollection())).thenReturn(List.of(room));
        when(reservationStatusManager.canCancelReservation(reservation)).thenReturn(false);

        List<UserReservationVO> result = reservationService.listUserReservations(1L);

        assertEquals(1, result.size());
        assertEquals("finished", result.get(0).getStatusKey());
        assertFalse(Boolean.TRUE.equals(result.get(0).getCanCancel()));
    }

    /**
     * 今天已开始的时段不允许新建预约。
     */
    @Test
    void shouldRejectCreateReservationForStartedTimeSlotToday() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setUserId(1L);
        request.setRoomId(1L);
        request.setTitle("临时会议");
        request.setPurpose("临时会议");
        request.setAttendeeCount(3);
        request.setReservationDate(LocalDate.now());
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));

        when(reservationStatusManager.hasReservationStarted(request.getReservationDate(), request.getStartTime()))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.createReservation(request)
        );

        assertEquals("今天已开始的时段不可预约", exception.getMessage());
        verify(reservationStatusManager).refreshExpiredReservations();
    }

    /**
     * 超过最大预约时长时不允许新建预约。
     */
    @Test
    void shouldRejectCreateReservationWhenDurationExceedsRule() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setUserId(1L);
        request.setRoomId(1L);
        request.setTitle("长时间会议");
        request.setPurpose("长时间会议");
        request.setAttendeeCount(3);
        request.setReservationDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(12, 0));

        when(reservationRuleService.getRule()).thenReturn(buildDefaultRule());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.createReservation(request)
        );

        assertEquals("单次预约时长不能超过120分钟", exception.getMessage());
        verify(reservationStatusManager).refreshExpiredReservations();
    }

    /**
     * 月视图应返回整月日期并统计有预约日期。
     */
    @Test
    void shouldReturnWholeMonthCalendar() {
        LocalDate targetDate = LocalDate.of(2026, 4, 23);
        Reservation reservation = buildReservation(12L, 1L, 1, targetDate);
        MeetingRoom room = new MeetingRoom();
        room.setId(1L);
        room.setName("创新会议室");

        when(userMapper.selectById(1L)).thenReturn(buildActiveUser());
        when(reservationMapper.selectList(any())).thenReturn(List.of(reservation));
        when(meetingRoomMapper.selectBatchIds(anyCollection())).thenReturn(List.of(room));

        ReservationCalendarVO result = reservationService.getCalendar(1L, "month", targetDate);

        assertEquals("month", result.getViewType());
        assertEquals("2026-04-01", result.getStartDate());
        assertEquals("2026-04-30", result.getEndDate());
        assertEquals(30, result.getDays().size());
        assertEquals(Integer.valueOf(1), result.getTotalCount());
        assertEquals(Integer.valueOf(1), result.getDays().get(22).getTotalCount());
    }

    /**
     * 构造预约实体。
     *
     * @param id              预约ID
     * @param roomId          会议室ID
     * @param status          状态
     * @param reservationDate 预约日期
     * @return 预约实体
     */
    private Reservation buildReservation(Long id, Long roomId, int status, LocalDate reservationDate) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setUserId(1L);
        reservation.setRoomId(roomId);
        reservation.setReservationNo("RTEST001");
        reservation.setTitle("项目复盘");
        reservation.setPurpose("迭代总结");
        reservation.setAttendeeCount(6);
        reservation.setReservationDate(reservationDate);
        reservation.setStartTime(LocalTime.of(9, 0));
        reservation.setEndTime(LocalTime.of(10, 0));
        reservation.setStatus(status);
        return reservation;
    }

    /**
     * 构造可用用户。
     *
     * @return 用户实体
     */
    private User buildActiveUser() {
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        return user;
    }

    /**
     * 构造默认预约规则。
     *
     * @return 预约规则
     */
    private ReservationRuleVO buildDefaultRule() {
        ReservationRuleVO rule = new ReservationRuleVO();
        rule.setMaxDurationMinutes(120);
        rule.setMinAdvanceMinutes(0);
        return rule;
    }
}
