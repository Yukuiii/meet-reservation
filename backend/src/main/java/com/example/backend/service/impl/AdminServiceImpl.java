package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.dto.AdminEmergencyOccupyRequest;
import com.example.backend.dto.AdminReviewReservationRequest;
import com.example.backend.dto.AdminSaveMeetingRoomRequest;
import com.example.backend.entity.Equipment;
import com.example.backend.entity.MeetingRoom;
import com.example.backend.entity.Reservation;
import com.example.backend.entity.RoomEquipment;
import com.example.backend.entity.RoomImage;
import com.example.backend.entity.User;
import com.example.backend.mapper.EquipmentMapper;
import com.example.backend.mapper.MeetingRoomMapper;
import com.example.backend.mapper.ReservationMapper;
import com.example.backend.mapper.RoomEquipmentMapper;
import com.example.backend.mapper.RoomImageMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.AdminService;
import com.example.backend.vo.AdminEmergencyOccupyVO;
import com.example.backend.vo.AdminEquipmentVO;
import com.example.backend.vo.AdminMeetingRoomVO;
import com.example.backend.vo.AdminReservationVO;
import com.example.backend.vo.AdminStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理员业务实现。
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    /**
     * 预约状态：待审核。
     */
    private static final int RESERVATION_PENDING = 0;

    /**
     * 预约状态：已通过。
     */
    private static final int RESERVATION_APPROVED = 1;

    /**
     * 预约状态：已拒绝。
     */
    private static final int RESERVATION_REJECTED = 2;

    /**
     * 预约状态：已取消。
     */
    private static final int RESERVATION_CANCELLED = 3;

    /**
     * 预约状态：已完成。
     */
    private static final int RESERVATION_FINISHED = 4;

    /**
     * 会议室状态：停用。
     */
    private static final int ROOM_DISABLED = 0;

    /**
     * 会议室状态：正常。
     */
    private static final int ROOM_NORMAL = 1;

    /**
     * 会议室状态：维护中。
     */
    private static final int ROOM_MAINTENANCE = 2;

    /**
     * 默认冲突协调取消原因。
     */
    private static final String DEFAULT_EMERGENCY_CANCEL_REASON = "因管理员紧急占用，原预约已协调取消";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final UserMapper userMapper;
    private final ReservationMapper reservationMapper;
    private final MeetingRoomMapper meetingRoomMapper;
    private final RoomEquipmentMapper roomEquipmentMapper;
    private final EquipmentMapper equipmentMapper;
    private final RoomImageMapper roomImageMapper;

    /**
     * 查询待审核预约列表。
     *
     * @param adminUserId 管理员用户ID
     * @return 待审核预约列表
     */
    @Override
    public List<AdminReservationVO> listPendingReservations(Long adminUserId) {
        ensureAdminUser(adminUserId);

        List<Reservation> reservationList = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getStatus, RESERVATION_PENDING)
                        .orderByAsc(Reservation::getReservationDate)
                        .orderByAsc(Reservation::getStartTime)
                        .orderByAsc(Reservation::getId)
        );
        if (reservationList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = buildUserMap(reservationList);
        Map<Long, MeetingRoom> roomMap = buildRoomMap(reservationList);

        List<AdminReservationVO> result = new ArrayList<>(reservationList.size());
        for (Reservation reservation : reservationList) {
            result.add(toAdminReservationVO(reservation, userMap.get(reservation.getUserId()),
                    roomMap.get(reservation.getRoomId())));
        }
        return result;
    }

    /**
     * 审核预约申请。
     *
     * @param reservationId 预约ID
     * @param request       审核参数
     */
    @Override
    public void reviewReservation(Long reservationId, AdminReviewReservationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("审核参数不能为空");
        }
        User admin = ensureAdminUser(request.getAdminUserId());
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("预约ID不合法");
        }
        if (request.getApproved() == null) {
            throw new IllegalArgumentException("审核结果不能为空");
        }

        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("预约记录不存在");
        }
        if (!Integer.valueOf(RESERVATION_PENDING).equals(reservation.getStatus())) {
            throw new IllegalArgumentException("预约已处理，请刷新后重试");
        }

        Reservation updateEntity = new Reservation();
        updateEntity.setReviewerId(admin.getId());
        updateEntity.setReviewedAt(LocalDateTime.now());
        updateEntity.setUpdatedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(request.getApproved())) {
            updateEntity.setStatus(RESERVATION_APPROVED);
            updateEntity.setRejectReason(null);
            updateEntity.setRemark("管理员审核通过");
        } else {
            String rejectReason = cleanText(request.getRejectReason());
            if (!StringUtils.hasText(rejectReason)) {
                throw new IllegalArgumentException("驳回原因不能为空");
            }
            updateEntity.setStatus(RESERVATION_REJECTED);
            updateEntity.setRejectReason(rejectReason);
            updateEntity.setRemark("管理员驳回：" + rejectReason);
        }

        int rows = reservationMapper.update(
                updateEntity,
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getId, reservationId)
                        .eq(Reservation::getStatus, RESERVATION_PENDING)
        );
        if (rows != 1) {
            throw new IllegalStateException("审核失败，请刷新后重试");
        }
    }

    /**
     * 查询会议室管理列表。
     *
     * @param adminUserId 管理员用户ID
     * @return 会议室列表
     */
    @Override
    public List<AdminMeetingRoomVO> listMeetingRooms(Long adminUserId) {
        ensureAdminUser(adminUserId);

        List<MeetingRoom> roomList = meetingRoomMapper.selectList(
                new LambdaQueryWrapper<MeetingRoom>()
                        .orderByDesc(MeetingRoom::getSortOrder)
                        .orderByAsc(MeetingRoom::getId)
        );
        if (roomList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roomIds = roomList.stream().map(MeetingRoom::getId).toList();
        Map<Long, List<Long>> roomEquipmentIdMap = buildRoomEquipmentIdMap(roomIds);
        Map<Long, String> equipmentNameMap = buildEquipmentNameMap(roomEquipmentIdMap);
        Map<Long, String> roomImageMap = buildRoomImageMap(roomIds);

        List<AdminMeetingRoomVO> result = new ArrayList<>(roomList.size());
        for (MeetingRoom room : roomList) {
            AdminMeetingRoomVO roomVO = new AdminMeetingRoomVO();
            roomVO.setId(room.getId());
            roomVO.setName(room.getName());
            roomVO.setCapacity(room.getCapacity());
            roomVO.setLocation(room.getLocation());
            roomVO.setBuilding(room.getBuilding());
            roomVO.setFloor(room.getFloor());
            roomVO.setDescription(room.getDescription());
            roomVO.setCoverImage(selectRoomImage(room, roomImageMap.get(room.getId())));
            roomVO.setStatus(room.getStatus());
            roomVO.setStatusText(mapRoomStatusText(room.getStatus()));
            roomVO.setSortOrder(room.getSortOrder() == null ? 0 : room.getSortOrder());

            List<Long> equipmentIds = roomEquipmentIdMap.getOrDefault(room.getId(), Collections.emptyList());
            roomVO.setEquipmentIds(equipmentIds);
            roomVO.setEquipmentNames(equipmentIds.stream()
                    .map(equipmentNameMap::get)
                    .filter(StringUtils::hasText)
                    .toList());
            result.add(roomVO);
        }
        return result;
    }

    /**
     * 新增会议室。
     *
     * @param request 保存参数
     * @return 新会议室ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMeetingRoom(AdminSaveMeetingRoomRequest request) {
        validateSaveMeetingRoomRequest(request);
        ensureAdminUser(request.getAdminUserId());
        List<Long> equipmentIds = validateAndNormalizeEquipmentIds(request.getEquipmentIds());

        MeetingRoom entity = new MeetingRoom();
        fillMeetingRoomEntity(entity, request);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        int rows = meetingRoomMapper.insert(entity);
        if (rows != 1 || entity.getId() == null) {
            throw new IllegalStateException("新增会议室失败，请稍后重试");
        }

        syncRoomEquipment(entity.getId(), equipmentIds);
        return entity.getId();
    }

    /**
     * 编辑会议室。
     *
     * @param roomId  会议室ID
     * @param request 保存参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMeetingRoom(Long roomId, AdminSaveMeetingRoomRequest request) {
        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("会议室ID不合法");
        }
        if (meetingRoomMapper.selectById(roomId) == null) {
            throw new IllegalArgumentException("会议室不存在");
        }

        validateSaveMeetingRoomRequest(request);
        ensureAdminUser(request.getAdminUserId());
        List<Long> equipmentIds = validateAndNormalizeEquipmentIds(request.getEquipmentIds());

        MeetingRoom updateEntity = new MeetingRoom();
        fillMeetingRoomEntity(updateEntity, request);
        updateEntity.setUpdatedAt(LocalDateTime.now());

        int rows = meetingRoomMapper.update(
                updateEntity,
                new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getId, roomId)
        );
        if (rows != 1) {
            throw new IllegalStateException("编辑会议室失败，请稍后重试");
        }
        syncRoomEquipment(roomId, equipmentIds);
    }

    /**
     * 停用会议室。
     *
     * @param roomId      会议室ID
     * @param adminUserId 管理员用户ID
     */
    @Override
    public void disableMeetingRoom(Long roomId, Long adminUserId) {
        ensureAdminUser(adminUserId);
        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("会议室ID不合法");
        }

        MeetingRoom room = meetingRoomMapper.selectById(roomId);
        if (room == null) {
            throw new IllegalArgumentException("会议室不存在");
        }
        if (Integer.valueOf(ROOM_DISABLED).equals(room.getStatus())) {
            return;
        }

        MeetingRoom updateEntity = new MeetingRoom();
        updateEntity.setStatus(ROOM_DISABLED);
        updateEntity.setUpdatedAt(LocalDateTime.now());
        int rows = meetingRoomMapper.update(
                updateEntity,
                new LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getId, roomId)
        );
        if (rows != 1) {
            throw new IllegalStateException("停用会议室失败，请稍后重试");
        }
    }

    /**
     * 查询设备选项。
     *
     * @param adminUserId 管理员用户ID
     * @return 设备选项列表
     */
    @Override
    public List<AdminEquipmentVO> listEquipmentOptions(Long adminUserId) {
        ensureAdminUser(adminUserId);

        List<Equipment> equipmentList = equipmentMapper.selectList(
                new LambdaQueryWrapper<Equipment>()
                        .eq(Equipment::getStatus, 1)
                        .orderByAsc(Equipment::getId)
        );

        List<AdminEquipmentVO> result = new ArrayList<>(equipmentList.size());
        for (Equipment equipment : equipmentList) {
            AdminEquipmentVO item = new AdminEquipmentVO();
            item.setId(equipment.getId());
            item.setName(equipment.getName());
            result.add(item);
        }
        return result;
    }

    /**
     * 查询统计概览。
     *
     * @param adminUserId 管理员用户ID
     * @return 统计概览
     */
    @Override
    public AdminStatsVO getStats(Long adminUserId) {
        ensureAdminUser(adminUserId);

        AdminStatsVO stats = new AdminStatsVO();
        stats.setTotalUsers(safeCount(userMapper.selectCount(new LambdaQueryWrapper<>())));
        stats.setTotalRooms(safeCount(meetingRoomMapper.selectCount(new LambdaQueryWrapper<>())));
        stats.setNormalRooms(safeCount(
                meetingRoomMapper.selectCount(new LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getStatus, ROOM_NORMAL))));
        stats.setMaintenanceRooms(safeCount(
                meetingRoomMapper.selectCount(new LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getStatus, ROOM_MAINTENANCE))));
        stats.setDisabledRooms(safeCount(
                meetingRoomMapper.selectCount(new LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getStatus, ROOM_DISABLED))));

        stats.setTotalReservations(safeCount(reservationMapper.selectCount(new LambdaQueryWrapper<>())));
        stats.setPendingReservations(safeCount(
                reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getStatus, RESERVATION_PENDING))));
        stats.setApprovedReservations(safeCount(
                reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getStatus, RESERVATION_APPROVED))));
        stats.setRejectedReservations(safeCount(
                reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getStatus, RESERVATION_REJECTED))));
        stats.setCancelledReservations(safeCount(
                reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getStatus, RESERVATION_CANCELLED))));
        stats.setTodayReservations(safeCount(
                reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getReservationDate, LocalDate.now()))));
        return stats;
    }

    /**
     * 提交紧急占用。
     *
     * @param request 紧急占用参数
     * @return 紧急占用结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminEmergencyOccupyVO emergencyOccupy(AdminEmergencyOccupyRequest request) {
        validateEmergencyOccupyRequest(request);
        User admin = ensureAdminUser(request.getAdminUserId());

        MeetingRoom room = meetingRoomMapper.selectById(request.getRoomId());
        if (room == null || Integer.valueOf(ROOM_DISABLED).equals(room.getStatus())) {
            throw new IllegalArgumentException("会议室不存在或已停用");
        }

        List<Reservation> conflictReservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getRoomId, request.getRoomId())
                        .eq(Reservation::getReservationDate, request.getReservationDate())
                        .in(Reservation::getStatus, RESERVATION_PENDING, RESERVATION_APPROVED)
                        // 新时段开始时间 < 已有时段结束时间 且 新时段结束时间 > 已有时段开始时间，说明存在重叠冲突。
                        .lt(Reservation::getStartTime, request.getEndTime())
                        .gt(Reservation::getEndTime, request.getStartTime())
                        .orderByAsc(Reservation::getStartTime)
                        .orderByAsc(Reservation::getEndTime)
        );

        int conflictCount = conflictReservations.size();
        boolean forceOverride = Boolean.TRUE.equals(request.getForceOverride());
        if (conflictCount > 0 && !forceOverride) {
            throw new IllegalArgumentException("当前时段存在" + conflictCount + "条冲突预约，请开启强制协调后重试");
        }

        int cancelledCount = 0;
        if (conflictCount > 0) {
            List<Long> conflictIds = conflictReservations.stream().map(Reservation::getId).toList();
            Reservation updateEntity = new Reservation();
            updateEntity.setStatus(RESERVATION_CANCELLED);
            updateEntity.setCancelReason(normalizeEmergencyCancelReason(request.getCancelReason()));
            updateEntity.setCancelledAt(LocalDateTime.now());
            updateEntity.setReviewerId(admin.getId());
            updateEntity.setReviewedAt(LocalDateTime.now());
            updateEntity.setRemark("管理员紧急占用协调取消");
            updateEntity.setUpdatedAt(LocalDateTime.now());

            cancelledCount = reservationMapper.update(
                    updateEntity,
                    new LambdaQueryWrapper<Reservation>()
                            .in(Reservation::getId, conflictIds)
                            .in(Reservation::getStatus, RESERVATION_PENDING, RESERVATION_APPROVED)
            );
        }

        Reservation emergencyReservation = new Reservation();
        emergencyReservation.setReservationNo(generateEmergencyReservationNo());
        emergencyReservation.setUserId(admin.getId());
        emergencyReservation.setRoomId(request.getRoomId());
        emergencyReservation.setTitle(cleanText(request.getTitle()));
        emergencyReservation.setPurpose(cleanText(request.getPurpose()));
        emergencyReservation.setAttendeeCount(1);
        emergencyReservation.setReservationDate(request.getReservationDate());
        emergencyReservation.setStartTime(request.getStartTime());
        emergencyReservation.setEndTime(request.getEndTime());
        emergencyReservation.setStatus(RESERVATION_APPROVED);
        emergencyReservation.setReviewerId(admin.getId());
        emergencyReservation.setReviewedAt(LocalDateTime.now());
        emergencyReservation.setRemark(conflictCount > 0 ? "管理员紧急占用（已协调冲突）" : "管理员紧急占用");
        emergencyReservation.setCreatedAt(LocalDateTime.now());
        emergencyReservation.setUpdatedAt(LocalDateTime.now());

        int rows = reservationMapper.insert(emergencyReservation);
        if (rows != 1 || emergencyReservation.getId() == null) {
            throw new IllegalStateException("紧急占用失败，请稍后重试");
        }

        AdminEmergencyOccupyVO result = new AdminEmergencyOccupyVO();
        result.setReservationId(emergencyReservation.getId());
        result.setReservationNo(emergencyReservation.getReservationNo());
        result.setConflictCount(conflictCount);
        result.setCancelledCount(cancelledCount);
        result.setStatusText(conflictCount > 0 ? "已强制协调并完成紧急占用" : "紧急占用成功");
        return result;
    }

    /**
     * 校验管理员身份。
     *
     * @param adminUserId 管理员用户ID
     * @return 管理员实体
     */
    private User ensureAdminUser(Long adminUserId) {
        if (adminUserId == null || adminUserId <= 0) {
            throw new IllegalArgumentException("管理员身份缺失，请重新登录");
        }
        User user = userMapper.selectById(adminUserId);
        if (user == null) {
            throw new IllegalArgumentException("管理员不存在，请重新登录");
        }
        if (!Objects.equals(user.getStatus(), 1)) {
            throw new IllegalArgumentException("管理员账号已被禁用");
        }
        if (!Objects.equals(user.getRole(), 1)) {
            throw new IllegalArgumentException("当前用户无管理员权限");
        }
        return user;
    }

    /**
     * 校验会议室保存参数。
     *
     * @param request 保存参数
     */
    private void validateSaveMeetingRoomRequest(AdminSaveMeetingRoomRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("会议室参数不能为空");
        }
        if (!StringUtils.hasText(cleanText(request.getName()))) {
            throw new IllegalArgumentException("会议室名称不能为空");
        }
        if (request.getCapacity() == null || request.getCapacity() <= 0) {
            throw new IllegalArgumentException("会议室容量必须大于0");
        }
        if (!StringUtils.hasText(cleanText(request.getLocation()))) {
            throw new IllegalArgumentException("会议室位置不能为空");
        }
        normalizeRoomStatus(request.getStatus());
        normalizeSortOrder(request.getSortOrder());
    }

    /**
     * 将请求参数写入会议室实体。
     *
     * @param entity  会议室实体
     * @param request 保存参数
     */
    private void fillMeetingRoomEntity(MeetingRoom entity, AdminSaveMeetingRoomRequest request) {
        entity.setName(cleanText(request.getName()));
        entity.setCapacity(request.getCapacity());
        entity.setLocation(cleanText(request.getLocation()));
        entity.setBuilding(optionalText(request.getBuilding()));
        entity.setFloor(optionalText(request.getFloor()));
        entity.setDescription(optionalText(request.getDescription()));
        entity.setCoverImage(optionalText(request.getCoverImage()));
        entity.setStatus(normalizeRoomStatus(request.getStatus()));
        entity.setSortOrder(normalizeSortOrder(request.getSortOrder()));
    }

    /**
     * 同步会议室设备关联。
     *
     * @param roomId       会议室ID
     * @param equipmentIds 设备ID列表
     */
    private void syncRoomEquipment(Long roomId, List<Long> equipmentIds) {
        roomEquipmentMapper.delete(
                new LambdaQueryWrapper<RoomEquipment>()
                        .eq(RoomEquipment::getRoomId, roomId)
        );
        for (Long equipmentId : equipmentIds) {
            RoomEquipment relation = new RoomEquipment();
            relation.setRoomId(roomId);
            relation.setEquipmentId(equipmentId);
            relation.setQuantity(1);
            roomEquipmentMapper.insert(relation);
        }
    }

    /**
     * 校验设备ID并标准化。
     *
     * @param equipmentIds 请求中的设备ID
     * @return 去重后的设备ID列表
     */
    private List<Long> validateAndNormalizeEquipmentIds(List<Long> equipmentIds) {
        List<Long> normalizedIds = normalizeEquipmentIds(equipmentIds);
        if (normalizedIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Equipment> equipmentList = equipmentMapper.selectList(
                new LambdaQueryWrapper<Equipment>()
                        .in(Equipment::getId, normalizedIds)
                        .eq(Equipment::getStatus, 1)
        );
        if (equipmentList.size() != normalizedIds.size()) {
            throw new IllegalArgumentException("设备配置包含无效选项，请刷新后重试");
        }
        return normalizedIds;
    }

    /**
     * 设备ID去重与清洗。
     *
     * @param equipmentIds 原始设备ID列表
     * @return 清洗后的设备ID列表
     */
    private List<Long> normalizeEquipmentIds(List<Long> equipmentIds) {
        if (equipmentIds == null || equipmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return equipmentIds.stream()
                .filter(Objects::nonNull)
                .filter(item -> item > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    /**
     * 构建用户映射。
     *
     * @param reservationList 预约列表
     * @return 用户映射
     */
    private Map<Long, User> buildUserMap(List<Reservation> reservationList) {
        Set<Long> userIds = reservationList.stream()
                .map(Reservation::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
    }

    /**
     * 构建会议室映射。
     *
     * @param reservationList 预约列表
     * @return 会议室映射
     */
    private Map<Long, MeetingRoom> buildRoomMap(List<Reservation> reservationList) {
        Set<Long> roomIds = reservationList.stream()
                .map(Reservation::getRoomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (roomIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return meetingRoomMapper.selectBatchIds(roomIds).stream()
                .collect(Collectors.toMap(MeetingRoom::getId, Function.identity(), (left, right) -> left));
    }

    /**
     * 构建设备ID映射。
     *
     * @param roomIds 会议室ID列表
     * @return 会议室设备ID映射
     */
    private Map<Long, List<Long>> buildRoomEquipmentIdMap(List<Long> roomIds) {
        List<RoomEquipment> relationList = roomEquipmentMapper.selectList(
                new LambdaQueryWrapper<RoomEquipment>()
                        .in(RoomEquipment::getRoomId, roomIds)
                        .orderByAsc(RoomEquipment::getId)
        );
        if (relationList.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<Long>> roomEquipmentIdMap = new HashMap<>();
        for (RoomEquipment relation : relationList) {
            roomEquipmentIdMap
                    .computeIfAbsent(relation.getRoomId(), key -> new ArrayList<>())
                    .add(relation.getEquipmentId());
        }
        return roomEquipmentIdMap;
    }

    /**
     * 构建设备名称映射。
     *
     * @param roomEquipmentIdMap 会议室设备ID映射
     * @return 设备名称映射
     */
    private Map<Long, String> buildEquipmentNameMap(Map<Long, List<Long>> roomEquipmentIdMap) {
        Set<Long> equipmentIds = roomEquipmentIdMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        if (equipmentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Equipment> equipmentList = equipmentMapper.selectBatchIds(equipmentIds);
        Map<Long, String> equipmentNameMap = new HashMap<>();
        for (Equipment equipment : equipmentList) {
            equipmentNameMap.put(equipment.getId(), equipment.getName());
        }
        return equipmentNameMap;
    }

    /**
     * 构建会议室首图映射。
     *
     * @param roomIds 会议室ID列表
     * @return key为会议室ID，value为首图URL
     */
    private Map<Long, String> buildRoomImageMap(List<Long> roomIds) {
        List<RoomImage> imageList = roomImageMapper.selectList(
                new LambdaQueryWrapper<RoomImage>()
                        .in(RoomImage::getRoomId, roomIds)
                        .orderByAsc(RoomImage::getSortOrder)
                        .orderByAsc(RoomImage::getId)
        );

        Map<Long, String> imageMap = new HashMap<>();
        for (RoomImage roomImage : imageList) {
            if (!StringUtils.hasText(roomImage.getImageUrl())) {
                continue;
            }
            imageMap.putIfAbsent(roomImage.getRoomId(), roomImage.getImageUrl());
        }
        return imageMap;
    }

    /**
     * 选择会议室展示图。
     *
     * @param room          会议室实体
     * @param fallbackImage 图片表首图
     * @return 最终图片URL
     */
    private String selectRoomImage(MeetingRoom room, String fallbackImage) {
        if (StringUtils.hasText(room.getCoverImage())) {
            return room.getCoverImage();
        }
        if (StringUtils.hasText(fallbackImage)) {
            return fallbackImage;
        }
        return "";
    }

    /**
     * 预约实体转换为管理员预约视图对象。
     *
     * @param reservation 预约实体
     * @param user        用户实体
     * @param room        会议室实体
     * @return 管理员预约视图对象
     */
    private AdminReservationVO toAdminReservationVO(Reservation reservation, User user, MeetingRoom room) {
        AdminReservationVO item = new AdminReservationVO();
        item.setId(reservation.getId());
        item.setReservationNo(reservation.getReservationNo());
        item.setUserId(reservation.getUserId());
        item.setUsername(user == null ? "" : user.getUsername());
        item.setNickname(user == null ? "" : user.getNickname());
        item.setRoomId(reservation.getRoomId());
        item.setRoomName(room == null ? "未知会议室" : room.getName());
        item.setDate(formatDate(reservation.getReservationDate()));
        item.setStartTime(formatTime(reservation.getStartTime()));
        item.setEndTime(formatTime(reservation.getEndTime()));
        item.setTimeSlot(item.getStartTime() + "-" + item.getEndTime());
        item.setTitle(reservation.getTitle());
        item.setPurpose(reservation.getPurpose());
        item.setAttendeeCount(reservation.getAttendeeCount());
        item.setStatus(reservation.getStatus());
        item.setStatusKey(mapReservationStatusKey(reservation.getStatus()));
        item.setStatusText(mapReservationStatusText(reservation.getStatus()));
        return item;
    }

    /**
     * 校验紧急占用参数。
     *
     * @param request 紧急占用参数
     */
    private void validateEmergencyOccupyRequest(AdminEmergencyOccupyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("紧急占用参数不能为空");
        }
        if (request.getRoomId() == null || request.getRoomId() <= 0) {
            throw new IllegalArgumentException("会议室ID不合法");
        }
        if (request.getReservationDate() == null) {
            throw new IllegalArgumentException("占用日期不能为空");
        }
        if (request.getReservationDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("占用日期不能早于今天");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("占用时段不能为空");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("开始时间必须早于结束时间");
        }
        if (!StringUtils.hasText(cleanText(request.getTitle()))) {
            throw new IllegalArgumentException("占用主题不能为空");
        }
        if (!StringUtils.hasText(cleanText(request.getPurpose()))) {
            throw new IllegalArgumentException("占用说明不能为空");
        }
    }

    /**
     * 状态码转换为预约状态键。
     *
     * @param status 状态码
     * @return 状态键
     */
    private String mapReservationStatusKey(Integer status) {
        if (status == null) {
            return "unknown";
        }
        return switch (status) {
            case RESERVATION_PENDING -> "pending";
            case RESERVATION_APPROVED -> "approved";
            case RESERVATION_REJECTED -> "rejected";
            case RESERVATION_CANCELLED -> "cancelled";
            case RESERVATION_FINISHED -> "finished";
            default -> "unknown";
        };
    }

    /**
     * 状态码转换为预约状态文案。
     *
     * @param status 状态码
     * @return 状态文案
     */
    private String mapReservationStatusText(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case RESERVATION_PENDING -> "待审核";
            case RESERVATION_APPROVED -> "已通过";
            case RESERVATION_REJECTED -> "已拒绝";
            case RESERVATION_CANCELLED -> "已取消";
            case RESERVATION_FINISHED -> "已完成";
            default -> "未知状态";
        };
    }

    /**
     * 状态码转换为会议室状态文案。
     *
     * @param status 状态码
     * @return 状态文案
     */
    private String mapRoomStatusText(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case ROOM_DISABLED -> "停用";
            case ROOM_NORMAL -> "正常";
            case ROOM_MAINTENANCE -> "维护中";
            default -> "未知状态";
        };
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
     * 日期格式化为 yyyy-MM-dd。
     *
     * @param date 日期
     * @return 格式化字符串
     */
    private String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return DATE_FORMATTER.format(date);
    }

    /**
     * 标准化会议室状态。
     *
     * @param status 原始状态
     * @return 状态码
     */
    private Integer normalizeRoomStatus(Integer status) {
        if (status == null) {
            return ROOM_NORMAL;
        }
        if (!Objects.equals(status, ROOM_DISABLED)
                && !Objects.equals(status, ROOM_NORMAL)
                && !Objects.equals(status, ROOM_MAINTENANCE)) {
            throw new IllegalArgumentException("会议室状态不合法");
        }
        return status;
    }

    /**
     * 标准化排序值。
     *
     * @param sortOrder 原始排序值
     * @return 排序值
     */
    private Integer normalizeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }

    /**
     * 标准化紧急占用取消原因。
     *
     * @param cancelReason 原始取消原因
     * @return 最终取消原因
     */
    private String normalizeEmergencyCancelReason(String cancelReason) {
        String finalReason = cleanText(cancelReason);
        if (!StringUtils.hasText(finalReason)) {
            return DEFAULT_EMERGENCY_CANCEL_REASON;
        }
        return finalReason;
    }

    /**
     * 安全清洗文本。
     *
     * @param value 原始文本
     * @return 去空格文本
     */
    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 非空文本转换，空文本时返回 null。
     *
     * @param value 原始文本
     * @return 非空文本或null
     */
    private String optionalText(String value) {
        String finalValue = cleanText(value);
        if (!StringUtils.hasText(finalValue)) {
            return null;
        }
        return finalValue;
    }

    /**
     * 计数空安全转换。
     *
     * @param count 计数值
     * @return 安全计数值
     */
    private Long safeCount(Long count) {
        return count == null ? 0L : count;
    }

    /**
     * 生成紧急占用预约编号。
     *
     * @return 预约编号
     */
    private String generateEmergencyReservationNo() {
        String timePart = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "E" + timePart + randomPart;
    }
}
