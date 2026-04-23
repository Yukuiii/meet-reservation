package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.backend.dto.AdminBatchReviewReservationRequest;
import com.example.backend.dto.AdminEmergencyOccupyRequest;
import com.example.backend.dto.AdminReviewReservationRequest;
import com.example.backend.dto.AdminSaveAdminRequest;
import com.example.backend.entity.MeetingRoom;
import com.example.backend.entity.Reservation;
import com.example.backend.entity.User;
import com.example.backend.mapper.EquipmentMapper;
import com.example.backend.mapper.MeetingRoomMapper;
import com.example.backend.mapper.ReservationMapper;
import com.example.backend.mapper.ReservationRecommendationMapper;
import com.example.backend.mapper.RoomEquipmentMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.NotificationService;
import com.example.backend.service.support.ReservationStatusManager;
import com.example.backend.service.support.UserAccountSupport;
import com.example.backend.vo.AdminBatchReviewResultVO;
import com.example.backend.vo.AdminReservationVO;
import com.example.backend.vo.AdminUserVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员业务实现测试。
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private MeetingRoomMapper meetingRoomMapper;

    @Mock
    private RoomEquipmentMapper roomEquipmentMapper;

    @Mock
    private EquipmentMapper equipmentMapper;

    @Mock
    private ReservationRecommendationMapper recommendationMapper;

    @Mock
    private ReservationStatusManager reservationStatusManager;

    @Mock
    private NotificationService notificationService;

    private AdminServiceImpl adminService;

    /**
     * 初始化被测对象。
     */
    @BeforeEach
    void setUp() {
        initMybatisLambdaCache(User.class);
        initMybatisLambdaCache(Reservation.class);
        adminService = new AdminServiceImpl(
                userMapper,
                reservationMapper,
                meetingRoomMapper,
                roomEquipmentMapper,
                equipmentMapper,
                recommendationMapper,
                reservationStatusManager,
                notificationService
        );
    }

    /**
     * 初始化MyBatis-Plus lambda字段缓存。
     *
     * @param entityClass 实体类型
     */
    private void initMybatisLambdaCache(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                entityClass
        );
    }

    /**
     * 已结束待审核预约不允许继续审核。
     */
    @Test
    void shouldRejectReviewForEndedReservation() {
        Reservation reservation = buildPendingReservation(20L, 2L, 3L, LocalDate.now().minusDays(1));
        AdminReviewReservationRequest request = new AdminReviewReservationRequest();
        request.setAdminUserId(99L);
        request.setApproved(true);

        when(userMapper.selectById(99L)).thenReturn(buildAdminUser());
        when(reservationMapper.selectById(20L)).thenReturn(reservation);
        when(reservationStatusManager.isReservationEnded(reservation)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.reviewReservation(20L, request)
        );

        assertEquals("预约时段已结束，无法继续审核", exception.getMessage());
        verify(reservationStatusManager).refreshExpiredReservations();
        verify(reservationMapper, never()).update(any(), any());
    }

    /**
     * 待审核列表应过滤不可审核的过期记录。
     */
    @Test
    void shouldFilterExpiredReservationFromPendingList() {
        Reservation expiredReservation = buildPendingReservation(21L, 2L, 3L, LocalDate.now().minusDays(1));
        Reservation activeReservation = buildPendingReservation(22L, 2L, 4L, LocalDate.now().plusDays(1));

        when(userMapper.selectById(99L)).thenReturn(buildAdminUser());
        when(reservationMapper.selectList(any())).thenReturn(List.of(expiredReservation, activeReservation));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(buildApplicantUser()));
        when(meetingRoomMapper.selectBatchIds(any())).thenReturn(List.of(
                buildMeetingRoom(3L, "A101"),
                buildMeetingRoom(4L, "B201")
        ));
        when(reservationStatusManager.canReviewReservation(expiredReservation)).thenReturn(false);
        when(reservationStatusManager.canReviewReservation(activeReservation)).thenReturn(true);

        List<AdminReservationVO> result = adminService.listPendingReservations(99L);

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(22L), result.get(0).getId());
        assertEquals(Boolean.TRUE, result.get(0).getCanReview());
    }

    /**
     * 批量通过预约应返回处理统计。
     */
    @Test
    void shouldBatchApproveReservations() {
        Reservation firstReservation = buildPendingReservation(20L, 2L, 3L, LocalDate.now().plusDays(1));
        Reservation secondReservation = buildPendingReservation(21L, 2L, 4L, LocalDate.now().plusDays(1));
        AdminBatchReviewReservationRequest request = new AdminBatchReviewReservationRequest();
        request.setAdminUserId(99L);
        request.setReservationIds(List.of(20L, 21L));
        request.setApproved(true);

        when(userMapper.selectById(99L)).thenReturn(buildAdminUser());
        when(userMapper.selectById(2L)).thenReturn(buildApplicantUser());
        when(reservationMapper.selectById(20L)).thenReturn(firstReservation);
        when(reservationMapper.selectById(21L)).thenReturn(secondReservation);
        when(meetingRoomMapper.selectById(3L)).thenReturn(buildMeetingRoom(3L, "A101"));
        when(meetingRoomMapper.selectById(4L)).thenReturn(buildMeetingRoom(4L, "B201"));
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(reservationMapper.update(any(), any())).thenReturn(1);

        AdminBatchReviewResultVO result = adminService.batchReviewReservations(request);

        assertEquals(Integer.valueOf(2), result.getTotalCount());
        assertEquals(Integer.valueOf(2), result.getSuccessCount());
        assertEquals(Integer.valueOf(2), result.getApprovedCount());
        assertEquals(Integer.valueOf(0), result.getFailedCount());
        verify(reservationMapper, times(2)).update(any(), any());
        verify(notificationService, times(2)).createNotification(any(), any(), any(), anyInt(), any());
    }

    /**
     * 自动审核应通过可用预约并驳回冲突预约。
     */
    @Test
    void shouldAutoApproveAvailableAndRejectConflictReservations() {
        Reservation availableReservation = buildPendingReservation(20L, 2L, 3L, LocalDate.now().plusDays(1));
        Reservation conflictReservation = buildPendingReservation(21L, 2L, 3L, LocalDate.now().plusDays(1));

        when(userMapper.selectById(99L)).thenReturn(buildAdminUser());
        when(userMapper.selectById(2L)).thenReturn(buildApplicantUser());
        when(reservationMapper.selectList(any())).thenReturn(List.of(availableReservation, conflictReservation));
        when(reservationMapper.selectById(20L)).thenReturn(availableReservation);
        when(reservationMapper.selectById(21L)).thenReturn(conflictReservation);
        when(meetingRoomMapper.selectById(3L)).thenReturn(buildMeetingRoom(3L, "A101"));
        when(reservationMapper.selectCount(any())).thenReturn(0L, 0L, 1L);
        when(reservationMapper.update(any(), any())).thenReturn(1);

        AdminBatchReviewResultVO result = adminService.autoReviewPendingReservations(99L);
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationMapper, times(2)).update(captor.capture(), any());

        assertEquals(Integer.valueOf(2), result.getTotalCount());
        assertEquals(Integer.valueOf(2), result.getSuccessCount());
        assertEquals(Integer.valueOf(1), result.getApprovedCount());
        assertEquals(Integer.valueOf(1), result.getRejectedCount());
        assertEquals(Integer.valueOf(0), result.getFailedCount());
        assertEquals(Integer.valueOf(1), captor.getAllValues().get(0).getStatus());
        assertEquals(Integer.valueOf(2), captor.getAllValues().get(1).getStatus());
        assertTrue(captor.getAllValues().get(1).getRejectReason().contains("与已通过预约冲突"));
    }

    /**
     * 今天已开始的时段不允许紧急占用。
     */
    @Test
    void shouldRejectEmergencyOccupyForStartedTimeSlotToday() {
        AdminEmergencyOccupyRequest request = new AdminEmergencyOccupyRequest();
        request.setAdminUserId(99L);
        request.setRoomId(3L);
        request.setReservationDate(LocalDate.now());
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setTitle("紧急占用");
        request.setPurpose("故障处理");

        when(reservationStatusManager.hasReservationStarted(request.getReservationDate(), request.getStartTime()))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.emergencyOccupy(request)
        );

        assertEquals("今天已开始的时段不可紧急占用", exception.getMessage());
        verify(reservationStatusManager).refreshExpiredReservations();
        verify(reservationMapper, never()).selectList(any());
    }

    /**
     * 新增管理员应写入管理员角色并加密密码。
     */
    @Test
    void shouldCreateAdminWithEncryptedPassword() {
        AdminSaveAdminRequest request = new AdminSaveAdminRequest();
        request.setAdminUserId(99L);
        request.setUsername("new-admin");
        request.setNickname("新管理员");
        request.setPhone("13800138000");
        request.setEmail("admin@example.com");
        request.setPassword("secret88");
        request.setStatus(1);

        when(userMapper.selectById(99L)).thenReturn(buildAdminUser());
        when(userMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(120L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        Long adminId = adminService.createAdmin(request);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());

        assertEquals(Long.valueOf(120L), adminId);
        assertEquals(Integer.valueOf(1), captor.getValue().getRole());
        assertEquals(Integer.valueOf(1), captor.getValue().getStatus());
        assertEquals(UserAccountSupport.encryptPassword("secret88"), captor.getValue().getPassword());
    }

    /**
     * 编辑管理员且不填写密码时应保留原密码。
     */
    @Test
    void shouldUpdateAdminWithoutChangingPasswordWhenPasswordEmpty() {
        AdminSaveAdminRequest request = new AdminSaveAdminRequest();
        request.setAdminUserId(99L);
        request.setUsername("edited-admin");
        request.setNickname("已编辑管理员");
        request.setPhone("13900139000");
        request.setEmail("edited@example.com");
        request.setPassword("");
        request.setStatus(1);

        User targetAdmin = buildAdminUser();
        targetAdmin.setId(120L);
        targetAdmin.setUsername("old-admin");
        targetAdmin.setStatus(1);

        when(userMapper.selectById(99L)).thenReturn(buildAdminUser());
        when(userMapper.selectById(120L)).thenReturn(targetAdmin);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.update(any(), any())).thenReturn(1);

        adminService.updateAdmin(120L, request);
        ArgumentCaptor<LambdaUpdateWrapper> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(userMapper).update(org.mockito.ArgumentMatchers.isNull(), captor.capture());

        String sqlSet = captor.getValue().getSqlSet();
        assertTrue(sqlSet.contains("username"));
        assertTrue(sqlSet.contains("nickname"));
        assertTrue(sqlSet.contains("phone"));
        assertTrue(sqlSet.contains("email"));
        assertFalse(sqlSet.contains("password"));
    }

    /**
     * 删除当前登录管理员应被拒绝。
     */
    @Test
    void shouldRejectDeleteCurrentAdmin() {
        when(userMapper.selectById(99L)).thenReturn(buildAdminUser());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.deleteAdmin(99L, 99L)
        );

        assertEquals("不能删除当前登录管理员", exception.getMessage());
    }

    /**
     * 删除管理员成功时应调用删除操作。
     */
    @Test
    void shouldDeleteAdminSuccessfully() {
        User targetAdmin = buildAdminUser();
        targetAdmin.setId(120L);
        targetAdmin.setStatus(1);

        when(userMapper.selectById(99L)).thenReturn(buildAdminUser());
        when(userMapper.selectById(120L)).thenReturn(targetAdmin);
        when(userMapper.selectCount(any())).thenReturn(2L);
        when(userMapper.delete(any())).thenReturn(1);

        adminService.deleteAdmin(120L, 99L);

        verify(userMapper).delete(any());
    }

    /**
     * 管理员列表应返回格式化结果。
     */
    @Test
    void shouldListAdmins() {
        User admin = buildAdminUser();
        admin.setId(120L);
        admin.setUsername("manager");
        admin.setNickname("管理员甲");
        admin.setPhone("13800138000");
        admin.setEmail("manager@example.com");
        admin.setCreatedAt(LocalDateTime.of(2026, 4, 17, 11, 0));

        when(userMapper.selectById(99L)).thenReturn(buildAdminUser());
        when(userMapper.selectList(any())).thenReturn(List.of(admin));

        List<AdminUserVO> result = adminService.listAdmins(99L);

        assertEquals(1, result.size());
        assertEquals("manager", result.get(0).getUsername());
        assertEquals("正常", result.get(0).getStatusText());
        assertTrue(result.get(0).getCreatedAt().startsWith("2026-04-17"));
    }

    /**
     * 构造待审核预约。
     *
     * @param id              预约ID
     * @param userId          用户ID
     * @param roomId          会议室ID
     * @param reservationDate 预约日期
     * @return 预约实体
     */
    private Reservation buildPendingReservation(Long id, Long userId, Long roomId, LocalDate reservationDate) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setReservationNo("RTEST00" + id);
        reservation.setUserId(userId);
        reservation.setRoomId(roomId);
        reservation.setTitle("项目评审");
        reservation.setPurpose("评审会议");
        reservation.setAttendeeCount(8);
        reservation.setReservationDate(reservationDate);
        reservation.setStartTime(LocalTime.of(14, 0));
        reservation.setEndTime(LocalTime.of(15, 0));
        reservation.setStatus(0);
        return reservation;
    }

    /**
     * 构造管理员用户。
     *
     * @return 管理员实体
     */
    private User buildAdminUser() {
        User admin = new User();
        admin.setId(99L);
        admin.setUsername("root");
        admin.setStatus(1);
        admin.setRole(1);
        return admin;
    }

    /**
     * 构造申请用户。
     *
     * @return 用户实体
     */
    private User buildApplicantUser() {
        User user = new User();
        user.setId(2L);
        user.setUsername("applicant");
        user.setNickname("申请人");
        return user;
    }

    /**
     * 构造会议室实体。
     *
     * @param id   会议室ID
     * @param name 会议室名称
     * @return 会议室实体
     */
    private MeetingRoom buildMeetingRoom(Long id, String name) {
        MeetingRoom room = new MeetingRoom();
        room.setId(id);
        room.setName(name);
        room.setStatus(1);
        room.setCapacity(20);
        return room;
    }
}
