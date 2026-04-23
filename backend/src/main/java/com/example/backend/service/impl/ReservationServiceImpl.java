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
import com.example.backend.service.ReservationRuleService;
import com.example.backend.service.support.ReservationStatusManager;
import com.example.backend.vo.CreateReservationResponseVO;
import com.example.backend.vo.ReservationCalendarDayVO;
import com.example.backend.vo.ReservationCalendarItemVO;
import com.example.backend.vo.ReservationCalendarVO;
import com.example.backend.vo.ReservationRuleVO;
import com.example.backend.vo.ReservationScheduleItemVO;
import com.example.backend.vo.UserReservationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 预约业务实现。
 */
@Service
@RequiredArgsConstructor
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

    /**
     * 日视图。
     */
    private static final String VIEW_DAY = "day";

    /**
     * 周视图。
     */
    private static final String VIEW_WEEK = "week";

    /**
     * 月视图。
     */
    private static final String VIEW_MONTH = "month";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReservationMapper reservationMapper;
    private final MeetingRoomMapper meetingRoomMapper;
    private final UserMapper userMapper;
    private final ReservationStatusManager reservationStatusManager;
    private final ReservationRuleService reservationRuleService;

    /**
     * 查询会议室指定日期的占用时段。
     *
     * @param roomId          会议室ID
     * @param reservationDate 预约日期
     * @return 占用时段列表
     */
    @Override
    public List<ReservationScheduleItemVO> listRoomSchedule(Long roomId, LocalDate reservationDate) {
        reservationStatusManager.refreshExpiredReservations();
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
     * 查询日历视图预约数据。
     *
     * @param userId     用户ID
     * @param viewType   视图类型：day/week/month
     * @param targetDate 目标日期
     * @return 日历数据
     */
    @Override
    public ReservationCalendarVO getCalendar(Long userId, String viewType, LocalDate targetDate) {
        reservationStatusManager.refreshExpiredReservations();
        ensureUserCanReserve(userId);

        String finalViewType = normalizeViewType(viewType);
        LocalDate finalDate = targetDate == null ? LocalDate.now() : targetDate;
        LocalDate startDate = resolveCalendarStartDate(finalViewType, finalDate);
        LocalDate endDate = resolveCalendarEndDate(finalViewType, startDate, finalDate);

        List<Reservation> reservationList = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .between(Reservation::getReservationDate, startDate, endDate)
                        .in(Reservation::getStatus, STATUS_PENDING, STATUS_APPROVED, STATUS_FINISHED)
                        .orderByAsc(Reservation::getReservationDate)
                        .orderByAsc(Reservation::getStartTime)
                        .orderByAsc(Reservation::getEndTime)
                        .orderByAsc(Reservation::getId)
        );

        Map<Long, String> roomNameMap = buildRoomNameMap(reservationList);
        Map<LocalDate, List<Reservation>> dayReservationMap = initDayReservationMap(startDate, endDate);
        for (Reservation reservation : reservationList) {
            dayReservationMap
                    .computeIfAbsent(reservation.getReservationDate(), key -> new ArrayList<>())
                    .add(reservation);
        }

        List<ReservationCalendarDayVO> dayList = new ArrayList<>(dayReservationMap.size());
        int totalCount = 0;
        for (Map.Entry<LocalDate, List<Reservation>> entry : dayReservationMap.entrySet()) {
            LocalDate currentDate = entry.getKey();
            List<ReservationCalendarItemVO> itemList = entry.getValue().stream()
                    .map(item -> toCalendarItem(item, roomNameMap.get(item.getRoomId())))
                    .toList();

            ReservationCalendarDayVO dayVO = new ReservationCalendarDayVO();
            dayVO.setDate(formatDate(currentDate));
            dayVO.setWeekDay(mapWeekDay(currentDate.getDayOfWeek()));
            dayVO.setTotalCount(itemList.size());
            dayVO.setItems(itemList);
            dayList.add(dayVO);
            totalCount += itemList.size();
        }

        ReservationCalendarVO result = new ReservationCalendarVO();
        result.setViewType(finalViewType);
        result.setStartDate(formatDate(startDate));
        result.setEndDate(formatDate(endDate));
        result.setTotalCount(totalCount);
        result.setDays(dayList);
        return result;
    }

    /**
     * 创建预约。
     *
     * @param request 创建预约请求
     * @return 创建结果
     */
    @Override
    public CreateReservationResponseVO createReservation(CreateReservationRequest request) {
        reservationStatusManager.refreshExpiredReservations();
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
        reservationStatusManager.refreshExpiredReservations();
        ensureUserCanReserve(userId);

        List<Reservation> reservationList = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getUserId, userId)
                        .in(Reservation::getStatus, STATUS_PENDING, STATUS_APPROVED, STATUS_CANCELLED, STATUS_FINISHED)
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
        reservationStatusManager.refreshExpiredReservations();
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
        reservationStatusManager.refreshExpiredReservations();
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
        if (reservationStatusManager.isReservationEnded(reservation)) {
            throw new IllegalArgumentException("预约时段已结束，无法取消");
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
        if (reservationStatusManager.hasReservationStarted(request.getReservationDate(), request.getStartTime())) {
            throw new IllegalArgumentException("今天已开始的时段不可预约");
        }
        validateReservationRule(request);

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
     * 校验预约规则。
     *
     * @param request 创建请求
     */
    private void validateReservationRule(CreateReservationRequest request) {
        ReservationRuleVO rule = reservationRuleService.getRule();
        long durationMinutes = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        if (durationMinutes > rule.getMaxDurationMinutes()) {
            throw new IllegalArgumentException("单次预约时长不能超过" + rule.getMaxDurationMinutes() + "分钟");
        }

        LocalDateTime reservationStart = LocalDateTime.of(request.getReservationDate(), request.getStartTime());
        LocalDateTime earliestStart = LocalDateTime.now().plusMinutes(rule.getMinAdvanceMinutes());
        if (reservationStart.isBefore(earliestStart)) {
            throw new IllegalArgumentException("预约需至少提前" + rule.getMinAdvanceMinutes() + "分钟");
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
     * 标准化日历视图类型。
     *
     * @param viewType 原始视图类型
     * @return day、week 或 month
     */
    private String normalizeViewType(String viewType) {
        if (!StringUtils.hasText(viewType)) {
            return VIEW_DAY;
        }
        String finalViewType = viewType.trim().toLowerCase();
        if (VIEW_DAY.equals(finalViewType)
                || VIEW_WEEK.equals(finalViewType)
                || VIEW_MONTH.equals(finalViewType)) {
            return finalViewType;
        }
        throw new IllegalArgumentException("日历视图类型不合法，仅支持day/week/month");
    }

    /**
     * 计算日历视图起始日期。
     *
     * @param viewType 视图类型
     * @param date     目标日期
     * @return 起始日期
     */
    private LocalDate resolveCalendarStartDate(String viewType, LocalDate date) {
        if (VIEW_WEEK.equals(viewType)) {
            return getWeekStart(date);
        }
        if (VIEW_MONTH.equals(viewType)) {
            return date.withDayOfMonth(1);
        }
        return date;
    }

    /**
     * 计算日历视图结束日期。
     *
     * @param viewType  视图类型
     * @param startDate 起始日期
     * @param date      目标日期
     * @return 结束日期
     */
    private LocalDate resolveCalendarEndDate(String viewType, LocalDate startDate, LocalDate date) {
        if (VIEW_WEEK.equals(viewType)) {
            return startDate.plusDays(6);
        }
        if (VIEW_MONTH.equals(viewType)) {
            return date.withDayOfMonth(date.lengthOfMonth());
        }
        return date;
    }

    /**
     * 计算目标日期所在周（周一）起始日。
     *
     * @param date 目标日期
     * @return 周起始日
     */
    private LocalDate getWeekStart(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
    }

    /**
     * 初始化日期到预约列表映射，保证空日期也会返回。
     *
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 日期映射
     */
    private Map<LocalDate, List<Reservation>> initDayReservationMap(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<Reservation>> dayReservationMap = new LinkedHashMap<>();
        for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
            dayReservationMap.put(currentDate, new ArrayList<>());
        }
        return dayReservationMap;
    }

    /**
     * 预约实体转日历预约项。
     *
     * @param reservation 预约实体
     * @param roomName    会议室名称
     * @return 日历预约项
     */
    private ReservationCalendarItemVO toCalendarItem(Reservation reservation, String roomName) {
        ReservationCalendarItemVO item = new ReservationCalendarItemVO();
        item.setId(reservation.getId());
        item.setReservationNo(reservation.getReservationNo());
        item.setRoomId(reservation.getRoomId());
        item.setRoomName(StringUtils.hasText(roomName) ? roomName : "未知会议室");
        item.setDate(formatDate(reservation.getReservationDate()));
        item.setStartTime(formatTime(reservation.getStartTime()));
        item.setEndTime(formatTime(reservation.getEndTime()));
        item.setTimeSlot(item.getStartTime() + "-" + item.getEndTime());
        item.setTitle(reservation.getTitle());
        item.setStatus(reservation.getStatus());
        item.setStatusKey(mapStatusKey(reservation.getStatus()));
        item.setStatusText(mapStatusText(reservation.getStatus()));
        return item;
    }

    /**
     * 星期枚举转换为文案。
     *
     * @param dayOfWeek 星期枚举
     * @return 星期文案
     */
    private String mapWeekDay(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
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
        item.setCanCancel(reservationStatusManager.canCancelReservation(reservation));
        item.setCanReportRepair(Integer.valueOf(STATUS_FINISHED).equals(reservation.getStatus()));
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
