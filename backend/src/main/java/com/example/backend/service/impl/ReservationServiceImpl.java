package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.dto.CreateReservationRequest;
import com.example.backend.entity.MeetingRoom;
import com.example.backend.entity.Reservation;
import com.example.backend.entity.User;
import com.example.backend.mapper.MeetingRoomMapper;
import com.example.backend.mapper.ReservationMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.ReservationService;
import com.example.backend.vo.CreateReservationResponseVO;
import com.example.backend.vo.ReservationScheduleItemVO;
import com.example.backend.vo.UserReservationVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 预约业务实现。
 */
@Service
public class ReservationServiceImpl implements ReservationService {

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
     * 已拒绝状态。
     */
    private static final int STATUS_REJECTED = 2;

    /**
     * 已完成状态。
     */
    private static final int STATUS_FINISHED = 4;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationMapper reservationMapper;
    private final MeetingRoomMapper meetingRoomMapper;
    private final UserMapper userMapper;

    /**
     * 构造函数注入。
     *
     * @param reservationMapper 预约数据访问对象
     * @param meetingRoomMapper 会议室数据访问对象
     * @param userMapper        用户数据访问对象
     */
    public ReservationServiceImpl(ReservationMapper reservationMapper,
                                  MeetingRoomMapper meetingRoomMapper,
                                  UserMapper userMapper) {
        this.reservationMapper = reservationMapper;
        this.meetingRoomMapper = meetingRoomMapper;
        this.userMapper = userMapper;
    }

    /**
     * 查询会议室指定日期的占用时段。
     *
     * @param roomId          会议室ID
     * @param reservationDate 预约日期
     * @return 占用时段列表
     */
    @Override
    public List<ReservationScheduleItemVO> listRoomSchedule(Long roomId, LocalDate reservationDate) {
        validateRoomIdAndDate(roomId, reservationDate);
        ensureRoomCanReserve(roomId);

        List<Reservation> reservationList = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getRoomId, roomId)
                        .eq(Reservation::getReservationDate, reservationDate)
                        .in(Reservation::getStatus, STATUS_PENDING, STATUS_APPROVED)
                        .orderByAsc(Reservation::getStartTime)
                        .orderByAsc(Reservation::getEndTime)
                        .orderByAsc(Reservation::getId)
        );

        return reservationList.stream().map(this::toScheduleItem).toList();
    }

    /**
     * 创建预约。
     *
     * @param request 创建预约请求
     * @return 创建结果
     */
    @Override
    public CreateReservationResponseVO createReservation(CreateReservationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("预约参数不能为空");
        }

        validateCreateRequest(request);

        MeetingRoom room = ensureRoomCanReserve(request.getRoomId());
        ensureUserCanReserve(request.getUserId());

        if (request.getAttendeeCount() > room.getCapacity()) {
            throw new IllegalArgumentException("参与人数不能超过会议室容量");
        }

        ensureNoConflict(request.getRoomId(), request.getReservationDate(), request.getStartTime(), request.getEndTime());

        Reservation reservation = new Reservation();
        reservation.setReservationNo(generateReservationNo());
        reservation.setUserId(request.getUserId());
        reservation.setRoomId(request.getRoomId());
        reservation.setTitle(request.getTitle().trim());
        reservation.setPurpose(request.getPurpose().trim());
        reservation.setAttendeeCount(request.getAttendeeCount());
        reservation.setReservationDate(request.getReservationDate());
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setStatus(STATUS_PENDING);
        reservation.setRemark("用户提交预约");
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setUpdatedAt(LocalDateTime.now());

        int rows = reservationMapper.insert(reservation);
        if (rows != 1) {
            throw new IllegalStateException("预约提交失败，请稍后重试");
        }

        CreateReservationResponseVO response = new CreateReservationResponseVO();
        response.setId(reservation.getId());
        response.setReservationNo(reservation.getReservationNo());
        response.setStatus(STATUS_PENDING);
        response.setStatusText("待审核");
        return response;
    }

    /**
     * 查询用户预约记录列表。
     *
     * @param userId 用户ID
     * @return 预约记录列表
     */
    @Override
    public List<UserReservationVO> listUserReservations(Long userId) {
        ensureUserCanReserve(userId);

        List<Reservation> reservationList = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, userId)
                        .in(Reservation::getStatus, STATUS_PENDING, STATUS_APPROVED, STATUS_CANCELLED)
                        .orderByDesc(Reservation::getReservationDate)
                        .orderByDesc(Reservation::getStartTime)
                        .orderByDesc(Reservation::getId)
        );
        if (reservationList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> roomNameMap = buildRoomNameMap(reservationList);
        return reservationList.stream()
                .map(item -> toUserReservationVO(item, roomNameMap.get(item.getRoomId())))
                .toList();
    }

    /**
     * 查询预约详情。
     *
     * @param userId        用户ID
     * @param reservationId 预约ID
     * @return 预约详情
     */
    @Override
    public UserReservationVO getReservationDetail(Long userId, Long reservationId) {
        ensureUserCanReserve(userId);
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("预约ID不合法");
        }

        Reservation reservation = reservationMapper.selectOne(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getId, reservationId)
                        .eq(Reservation::getUserId, userId)
                        .last("limit 1")
        );
        if (reservation == null) {
            throw new IllegalArgumentException("预约记录不存在");
        }

        MeetingRoom room = meetingRoomMapper.selectById(reservation.getRoomId());
        String roomName = room == null ? "未知会议室" : room.getName();
        return toUserReservationVO(reservation, roomName);
    }

    /**
     * 取消预约。
     *
     * @param userId        用户ID
     * @param reservationId 预约ID
     * @param cancelReason  取消原因
     */
    @Override
    public void cancelReservation(Long userId, Long reservationId, String cancelReason) {
        ensureUserCanReserve(userId);
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("预约ID不合法");
        }

        Reservation reservation = reservationMapper.selectOne(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getId, reservationId)
                        .eq(Reservation::getUserId, userId)
                        .last("limit 1")
        );
        if (reservation == null) {
            throw new IllegalArgumentException("预约记录不存在");
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(reservation.getStatus())
                && !Integer.valueOf(STATUS_APPROVED).equals(reservation.getStatus())) {
            throw new IllegalArgumentException("当前状态不支持取消预约");
        }

        Reservation updateEntity = new Reservation();
        updateEntity.setStatus(STATUS_CANCELLED);
        updateEntity.setCancelReason(normalizeCancelReason(cancelReason));
        updateEntity.setCancelledAt(LocalDateTime.now());
        updateEntity.setUpdatedAt(LocalDateTime.now());

        int rows = reservationMapper.update(
                updateEntity,
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getId, reservationId)
                        .eq(Reservation::getUserId, userId)
                        .in(Reservation::getStatus, STATUS_PENDING, STATUS_APPROVED)
        );
        if (rows != 1) {
            throw new IllegalStateException("取消预约失败，请稍后重试");
        }
    }

    /**
     * 校验基础参数。
     *
     * @param roomId          会议室ID
     * @param reservationDate 预约日期
     */
    private void validateRoomIdAndDate(Long roomId, LocalDate reservationDate) {
        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("会议室ID不合法");
        }
        if (reservationDate == null) {
            throw new IllegalArgumentException("预约日期不能为空");
        }
    }

    /**
     * 校验创建预约参数。
     *
     * @param request 创建请求
     */
    private void validateCreateRequest(CreateReservationRequest request) {
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new IllegalArgumentException("用户信息缺失，请重新登录");
        }
        validateRoomIdAndDate(request.getRoomId(), request.getReservationDate());
        if (request.getReservationDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("预约日期不能早于今天");
        }

        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("预约时间不能为空");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("开始时间必须早于结束时间");
        }

        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("会议主题不能为空");
        }
        if (!StringUtils.hasText(request.getPurpose())) {
            throw new IllegalArgumentException("预约事由不能为空");
        }
        if (request.getAttendeeCount() == null || request.getAttendeeCount() <= 0) {
            throw new IllegalArgumentException("参与人数必须大于0");
        }
    }

    /**
     * 校验会议室是否可预约。
     *
     * @param roomId 会议室ID
     * @return 会议室实体
     */
    private MeetingRoom ensureRoomCanReserve(Long roomId) {
        MeetingRoom room = meetingRoomMapper.selectById(roomId);
        if (room == null || Integer.valueOf(0).equals(room.getStatus())) {
            throw new IllegalArgumentException("会议室不存在或已停用");
        }
        if (Integer.valueOf(2).equals(room.getStatus())) {
            throw new IllegalArgumentException("会议室维护中，暂不可预约");
        }
        return room;
    }

    /**
     * 校验用户是否可预约。
     *
     * @param userId 用户ID
     */
    private void ensureUserCanReserve(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户信息缺失，请重新登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在，请重新登录");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new IllegalArgumentException("账号已被禁用，无法预约");
        }
    }

    /**
     * 校验预约冲突。
     *
     * @param roomId          会议室ID
     * @param reservationDate 预约日期
     * @param startTime       开始时间
     * @param endTime         结束时间
     */
    private void ensureNoConflict(Long roomId, LocalDate reservationDate, LocalTime startTime, LocalTime endTime) {
        Long conflictCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getRoomId, roomId)
                        .eq(Reservation::getReservationDate, reservationDate)
                        .in(Reservation::getStatus, STATUS_PENDING, STATUS_APPROVED)
                        // 新预约开始时间早于已有预约结束 且 新预约结束时间晚于已有预约开始，则发生时间重叠。
                        .lt(Reservation::getStartTime, endTime)
                        .gt(Reservation::getEndTime, startTime)
        );
        if (conflictCount != null && conflictCount > 0) {
            throw new IllegalArgumentException("所选时间段已被占用，请更换时段");
        }
    }

    /**
     * 预约实体转占用时段VO。
     *
     * @param reservation 预约实体
     * @return 时段VO
     */
    private ReservationScheduleItemVO toScheduleItem(Reservation reservation) {
        ReservationScheduleItemVO item = new ReservationScheduleItemVO();
        item.setId(reservation.getId());
        item.setReservationNo(reservation.getReservationNo());
        item.setStartTime(formatTime(reservation.getStartTime()));
        item.setEndTime(formatTime(reservation.getEndTime()));
        item.setStatus(reservation.getStatus());
        item.setStatusText(mapStatusText(reservation.getStatus()));
        item.setTitle(reservation.getTitle());
        return item;
    }

    /**
     * 时间格式化为 HH:mm。
     *
     * @param time 时间
     * @return 格式化字符串
     */
    private String formatTime(LocalTime time) {
        if (time == null) {
            return "";
        }
        return TIME_FORMATTER.format(time);
    }

    /**
     * 状态码转换为文案。
     *
     * @param status 状态码
     * @return 状态文案
     */
    private String mapStatusText(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case STATUS_PENDING -> "待审核";
            case STATUS_APPROVED -> "已通过";
            case STATUS_REJECTED -> "已拒绝";
            case STATUS_CANCELLED -> "已取消";
            case STATUS_FINISHED -> "已完成";
            default -> "未知状态";
        };
    }

    /**
     * 状态码转换为前端状态键。
     *
     * @param status 状态码
     * @return 状态键
     */
    private String mapStatusKey(Integer status) {
        if (status == null) {
            return "unknown";
        }
        return switch (status) {
            case STATUS_PENDING -> "pending";
            case STATUS_APPROVED -> "approved";
            case STATUS_CANCELLED -> "cancelled";
            case STATUS_REJECTED -> "rejected";
            case STATUS_FINISHED -> "finished";
            default -> "unknown";
        };
    }

    /**
     * 构建会议室名称映射。
     *
     * @param reservationList 预约列表
     * @return 会议室名称映射
     */
    private Map<Long, String> buildRoomNameMap(List<Reservation> reservationList) {
        Set<Long> roomIds = reservationList.stream()
                .map(Reservation::getRoomId)
                .collect(Collectors.toSet());
        if (roomIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<MeetingRoom> roomList = meetingRoomMapper.selectBatchIds(roomIds);
        Map<Long, String> roomNameMap = new HashMap<>();
        for (MeetingRoom room : roomList) {
            roomNameMap.put(room.getId(), room.getName());
        }
        return roomNameMap;
    }

    /**
     * 预约实体转用户预约视图对象。
     *
     * @param reservation 预约实体
     * @param roomName    会议室名称
     * @return 用户预约视图对象
     */
    private UserReservationVO toUserReservationVO(Reservation reservation, String roomName) {
        UserReservationVO item = new UserReservationVO();
        item.setId(reservation.getId());
        item.setReservationNo(reservation.getReservationNo());
        item.setRoomId(reservation.getRoomId());
        item.setRoomName(StringUtils.hasText(roomName) ? roomName : "未知会议室");
        item.setDate(formatDate(reservation.getReservationDate()));
        item.setStartTime(formatTime(reservation.getStartTime()));
        item.setEndTime(formatTime(reservation.getEndTime()));
        item.setTimeSlot(item.getStartTime() + "-" + item.getEndTime());
        item.setTitle(reservation.getTitle());
        item.setPurpose(reservation.getPurpose());
        item.setAttendees(reservation.getAttendeeCount());
        item.setStatus(reservation.getStatus());
        item.setStatusKey(mapStatusKey(reservation.getStatus()));
        item.setStatusText(mapStatusText(reservation.getStatus()));
        item.setCancelReason(reservation.getCancelReason());
        item.setRejectReason(reservation.getRejectReason());
        item.setRemark(reservation.getRemark());
        return item;
    }

    /**
     * 日期格式化为 yyyy-MM-dd。
     *
     * @param date 日期
     * @return 日期字符串
     */
    private String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return DATE_FORMATTER.format(date);
    }

    /**
     * 标准化取消原因。
     *
     * @param cancelReason 原始取消原因
     * @return 标准化结果
     */
    private String normalizeCancelReason(String cancelReason) {
        if (!StringUtils.hasText(cancelReason)) {
            return "用户主动取消";
        }
        return cancelReason.trim();
    }

    /**
     * 生成预约编号。
     *
     * @return 预约编号
     */
    private String generateReservationNo() {
        String timePart = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "R" + timePart + randomPart;
    }
}
