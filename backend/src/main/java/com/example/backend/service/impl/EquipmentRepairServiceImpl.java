package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.dto.CreateEquipmentRepairRequest;
import com.example.backend.dto.ResolveEquipmentRepairRequest;
import com.example.backend.entity.Equipment;
import com.example.backend.entity.EquipmentRepair;
import com.example.backend.entity.MeetingRoom;
import com.example.backend.entity.Reservation;
import com.example.backend.entity.RoomEquipment;
import com.example.backend.entity.User;
import com.example.backend.mapper.EquipmentMapper;
import com.example.backend.mapper.EquipmentRepairMapper;
import com.example.backend.mapper.MeetingRoomMapper;
import com.example.backend.mapper.ReservationMapper;
import com.example.backend.mapper.RoomEquipmentMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.EquipmentRepairService;
import com.example.backend.service.support.ReservationStatusManager;
import com.example.backend.vo.EquipmentRepairVO;
import com.example.backend.vo.RepairEquipmentOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 设备报修业务实现。
 */
@Service
@RequiredArgsConstructor
public class EquipmentRepairServiceImpl implements EquipmentRepairService {

    /**
     * 预约状态：已完成。
     */
    private static final int RESERVATION_FINISHED = 4;

    /**
     * 用户角色：管理员。
     */
    private static final int USER_ROLE_ADMIN = 1;

    /**
     * 用户状态：禁用。
     */
    private static final int USER_STATUS_DISABLED = 0;

    /**
     * 设备状态：停用。
     */
    private static final int EQUIPMENT_DISABLED = 0;

    /**
     * 报修状态：待处理。
     */
    private static final int REPAIR_PENDING = 0;

    /**
     * 报修状态：已修复。
     */
    private static final int REPAIR_FIXED = 1;

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EquipmentRepairMapper equipmentRepairMapper;
    private final ReservationMapper reservationMapper;
    private final RoomEquipmentMapper roomEquipmentMapper;
    private final EquipmentMapper equipmentMapper;
    private final MeetingRoomMapper meetingRoomMapper;
    private final UserMapper userMapper;
    private final ReservationStatusManager reservationStatusManager;

    /**
     * 查询指定已完成预约可报修的设备选项。
     *
     * @param userId        用户ID
     * @param reservationId 预约ID
     * @return 设备选项列表
     */
    @Override
    public List<RepairEquipmentOptionVO> listRepairableEquipments(Long userId, Long reservationId) {
        reservationStatusManager.refreshExpiredReservations();
        ensureNormalUser(userId);
        Reservation reservation = ensureFinishedReservation(userId, reservationId);
        return buildRepairableEquipmentOptions(reservation.getRoomId());
    }

    /**
     * 创建设备报修记录。
     *
     * @param request 报修参数
     * @return 报修ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRepair(CreateEquipmentRepairRequest request) {
        reservationStatusManager.refreshExpiredReservations();
        validateCreateRequest(request);
        ensureNormalUser(request.getUserId());
        Reservation reservation = ensureFinishedReservation(request.getUserId(), request.getReservationId());
        Equipment equipment = ensureRoomEquipmentAvailable(reservation.getRoomId(), request.getEquipmentId());
        ensureNoDuplicatePendingRepair(request.getReservationId(), equipment.getId());

        EquipmentRepair repair = new EquipmentRepair();
        repair.setRepairNo(generateRepairNo());
        repair.setUserId(request.getUserId());
        repair.setReservationId(reservation.getId());
        repair.setRoomId(reservation.getRoomId());
        repair.setEquipmentId(equipment.getId());
        repair.setDescription(cleanText(request.getDescription()));
        repair.setStatus(REPAIR_PENDING);
        repair.setCreatedAt(LocalDateTime.now());
        repair.setUpdatedAt(LocalDateTime.now());

        int rows = equipmentRepairMapper.insert(repair);
        if (rows != 1 || repair.getId() == null) {
            throw new IllegalStateException("提交报修失败，请稍后重试");
        }
        return repair.getId();
    }

    /**
     * 查询管理员可见的设备报修列表。
     *
     * @param adminUserId 管理员用户ID
     * @return 报修列表
     */
    @Override
    public List<EquipmentRepairVO> listAdminRepairs(Long adminUserId) {
        ensureAdminUser(adminUserId);
        List<EquipmentRepair> repairList = equipmentRepairMapper.selectList(
                new LambdaQueryWrapper<EquipmentRepair>()
                        .orderByAsc(EquipmentRepair::getStatus)
                        .orderByDesc(EquipmentRepair::getCreatedAt)
                        .orderByDesc(EquipmentRepair::getId)
        );
        if (repairList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = buildUserMap(repairList);
        Map<Long, MeetingRoom> roomMap = buildRoomMap(repairList);
        Map<Long, Equipment> equipmentMap = buildEquipmentMap(repairList);
        Map<Long, Reservation> reservationMap = buildReservationMap(repairList);

        return repairList.stream()
                .map(item -> toRepairVO(
                        item,
                        userMap.get(item.getUserId()),
                        roomMap.get(item.getRoomId()),
                        equipmentMap.get(item.getEquipmentId()),
                        reservationMap.get(item.getReservationId())
                ))
                .toList();
    }

    /**
     * 标记设备报修已修复。
     *
     * @param repairId 报修ID
     * @param request  修复参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveRepair(Long repairId, ResolveEquipmentRepairRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("修复参数不能为空");
        }
        User admin = ensureAdminUser(request.getAdminUserId());
        if (repairId == null || repairId <= 0) {
            throw new IllegalArgumentException("报修ID不合法");
        }

        EquipmentRepair repair = equipmentRepairMapper.selectById(repairId);
        if (repair == null) {
            throw new IllegalArgumentException("报修记录不存在");
        }
        if (Integer.valueOf(REPAIR_FIXED).equals(repair.getStatus())) {
            return;
        }

        EquipmentRepair updateEntity = new EquipmentRepair();
        updateEntity.setStatus(REPAIR_FIXED);
        updateEntity.setFixedBy(admin.getId());
        updateEntity.setFixedAt(LocalDateTime.now());
        updateEntity.setFixRemark(cleanText(request.getFixRemark()));
        updateEntity.setUpdatedAt(LocalDateTime.now());
        int rows = equipmentRepairMapper.update(
                updateEntity,
                new LambdaQueryWrapper<EquipmentRepair>()
                        .eq(EquipmentRepair::getId, repairId)
                        .eq(EquipmentRepair::getStatus, REPAIR_PENDING)
        );
        if (rows != 1) {
            throw new IllegalStateException("更新报修状态失败，请刷新后重试");
        }
    }

    /**
     * 校验创建报修请求。
     *
     * @param request 创建参数
     */
    private void validateCreateRequest(CreateEquipmentRepairRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("报修参数不能为空");
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new IllegalArgumentException("用户信息缺失，请重新登录");
        }
        if (request.getReservationId() == null || request.getReservationId() <= 0) {
            throw new IllegalArgumentException("预约ID不合法");
        }
        if (request.getEquipmentId() == null || request.getEquipmentId() <= 0) {
            throw new IllegalArgumentException("请选择报修设备");
        }
        if (!StringUtils.hasText(cleanText(request.getDescription()))) {
            throw new IllegalArgumentException("请输入故障描述");
        }
    }

    /**
     * 校验普通用户可用。
     *
     * @param userId 用户ID
     */
    private void ensureNormalUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在，请重新登录");
        }
        if (Integer.valueOf(USER_STATUS_DISABLED).equals(user.getStatus())) {
            throw new IllegalArgumentException("账号已被禁用，无法报修");
        }
    }

    /**
     * 校验管理员可用。
     *
     * @param adminUserId 管理员用户ID
     * @return 管理员用户
     */
    private User ensureAdminUser(Long adminUserId) {
        if (adminUserId == null || adminUserId <= 0) {
            throw new IllegalArgumentException("管理员信息缺失");
        }
        User admin = userMapper.selectById(adminUserId);
        if (admin == null || !Integer.valueOf(USER_ROLE_ADMIN).equals(admin.getRole())) {
            throw new IllegalArgumentException("请使用管理员账号操作");
        }
        if (Integer.valueOf(USER_STATUS_DISABLED).equals(admin.getStatus())) {
            throw new IllegalArgumentException("管理员账号已禁用");
        }
        return admin;
    }

    /**
     * 校验用户拥有已完成预约。
     *
     * @param userId        用户ID
     * @param reservationId 预约ID
     * @return 预约实体
     */
    private Reservation ensureFinishedReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationMapper.selectOne(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getId, reservationId)
                        .eq(Reservation::getUserId, userId)
                        .last("limit 1")
        );
        if (reservation == null) {
            throw new IllegalArgumentException("预约记录不存在");
        }
        if (!Integer.valueOf(RESERVATION_FINISHED).equals(reservation.getStatus())) {
            throw new IllegalArgumentException("仅已完成预约可提交设备报修");
        }
        return reservation;
    }

    /**
     * 构建会议室可报修设备选项。
     *
     * @param roomId 会议室ID
     * @return 设备选项列表
     */
    private List<RepairEquipmentOptionVO> buildRepairableEquipmentOptions(Long roomId) {
        List<RoomEquipment> roomEquipmentList = roomEquipmentMapper.selectList(
                new LambdaQueryWrapper<RoomEquipment>()
                        .eq(RoomEquipment::getRoomId, roomId)
                        .orderByAsc(RoomEquipment::getEquipmentId)
        );
        if (roomEquipmentList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> equipmentIds = roomEquipmentList.stream()
                .map(RoomEquipment::getEquipmentId)
                .toList();
        Map<Long, Equipment> equipmentMap = equipmentMapper.selectBatchIds(equipmentIds)
                .stream()
                .filter(item -> !Integer.valueOf(EQUIPMENT_DISABLED).equals(item.getStatus()))
                .collect(Collectors.toMap(Equipment::getId, item -> item));

        return roomEquipmentList.stream()
                .map(item -> toRepairEquipmentOption(item, equipmentMap.get(item.getEquipmentId())))
                .filter(item -> item.getId() != null)
                .toList();
    }

    /**
     * 将会议室设备转换为报修选项。
     *
     * @param roomEquipment 会议室设备关联
     * @param equipment     设备实体
     * @return 报修选项
     */
    private RepairEquipmentOptionVO toRepairEquipmentOption(RoomEquipment roomEquipment, Equipment equipment) {
        RepairEquipmentOptionVO option = new RepairEquipmentOptionVO();
        if (equipment == null) {
            return option;
        }
        option.setId(equipment.getId());
        option.setName(equipment.getName());
        option.setDescription(equipment.getDescription());
        option.setQuantity(roomEquipment.getQuantity() == null ? 1 : roomEquipment.getQuantity());
        return option;
    }

    /**
     * 校验会议室拥有可用设备。
     *
     * @param roomId      会议室ID
     * @param equipmentId 设备ID
     * @return 设备实体
     */
    private Equipment ensureRoomEquipmentAvailable(Long roomId, Long equipmentId) {
        Long count = roomEquipmentMapper.selectCount(
                new LambdaQueryWrapper<RoomEquipment>()
                        .eq(RoomEquipment::getRoomId, roomId)
                        .eq(RoomEquipment::getEquipmentId, equipmentId)
        );
        if (count == null || count == 0) {
            throw new IllegalArgumentException("该会议室未配置所选设备");
        }

        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null || Integer.valueOf(EQUIPMENT_DISABLED).equals(equipment.getStatus())) {
            throw new IllegalArgumentException("设备不存在或已停用");
        }
        return equipment;
    }

    /**
     * 校验同一预约设备没有待处理报修。
     *
     * @param reservationId 预约ID
     * @param equipmentId   设备ID
     */
    private void ensureNoDuplicatePendingRepair(Long reservationId, Long equipmentId) {
        Long count = equipmentRepairMapper.selectCount(
                new LambdaQueryWrapper<EquipmentRepair>()
                        .eq(EquipmentRepair::getReservationId, reservationId)
                        .eq(EquipmentRepair::getEquipmentId, equipmentId)
                        .eq(EquipmentRepair::getStatus, REPAIR_PENDING)
        );
        if (count != null && count > 0) {
            throw new IllegalArgumentException("该设备已有待处理报修记录");
        }
    }

    /**
     * 构建用户映射。
     *
     * @param repairList 报修列表
     * @return 用户映射
     */
    private Map<Long, User> buildUserMap(List<EquipmentRepair> repairList) {
        Set<Long> ids = repairList.stream().map(EquipmentRepair::getUserId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(User::getId, item -> item));
    }

    /**
     * 构建会议室映射。
     *
     * @param repairList 报修列表
     * @return 会议室映射
     */
    private Map<Long, MeetingRoom> buildRoomMap(List<EquipmentRepair> repairList) {
        Set<Long> ids = repairList.stream().map(EquipmentRepair::getRoomId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return meetingRoomMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(MeetingRoom::getId, item -> item));
    }

    /**
     * 构建设备映射。
     *
     * @param repairList 报修列表
     * @return 设备映射
     */
    private Map<Long, Equipment> buildEquipmentMap(List<EquipmentRepair> repairList) {
        Set<Long> ids = repairList.stream().map(EquipmentRepair::getEquipmentId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return equipmentMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Equipment::getId, item -> item));
    }

    /**
     * 构建预约映射。
     *
     * @param repairList 报修列表
     * @return 预约映射
     */
    private Map<Long, Reservation> buildReservationMap(List<EquipmentRepair> repairList) {
        Set<Long> ids = repairList.stream().map(EquipmentRepair::getReservationId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Reservation> result = new HashMap<>();
        for (Reservation reservation : reservationMapper.selectBatchIds(ids)) {
            result.put(reservation.getId(), reservation);
        }
        return result;
    }

    /**
     * 报修实体转换为视图对象。
     *
     * @param repair      报修实体
     * @param user        用户实体
     * @param room        会议室实体
     * @param equipment   设备实体
     * @param reservation 预约实体
     * @return 报修视图对象
     */
    private EquipmentRepairVO toRepairVO(EquipmentRepair repair, User user, MeetingRoom room,
                                         Equipment equipment, Reservation reservation) {
        EquipmentRepairVO item = new EquipmentRepairVO();
        item.setId(repair.getId());
        item.setRepairNo(repair.getRepairNo());
        item.setUserId(repair.getUserId());
        item.setUsername(user == null ? "" : user.getUsername());
        item.setNickname(user == null ? "" : user.getNickname());
        item.setReservationId(repair.getReservationId());
        item.setReservationNo(reservation == null ? "" : reservation.getReservationNo());
        item.setRoomId(repair.getRoomId());
        item.setRoomName(room == null ? "未知会议室" : room.getName());
        item.setEquipmentId(repair.getEquipmentId());
        item.setEquipmentName(equipment == null ? "未知设备" : equipment.getName());
        item.setDescription(repair.getDescription());
        item.setStatus(repair.getStatus());
        item.setStatusText(mapRepairStatusText(repair.getStatus()));
        item.setCreatedAt(formatDateTime(repair.getCreatedAt()));
        item.setFixedBy(repair.getFixedBy());
        item.setFixedAt(formatDateTime(repair.getFixedAt()));
        item.setFixRemark(repair.getFixRemark());
        return item;
    }

    /**
     * 报修状态转换为文案。
     *
     * @param status 状态码
     * @return 状态文案
     */
    private String mapRepairStatusText(Integer status) {
        if (Integer.valueOf(REPAIR_FIXED).equals(status)) {
            return "已修复";
        }
        return "待处理";
    }

    /**
     * 格式化日期时间。
     *
     * @param dateTime 日期时间
     * @return 日期时间字符串
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATETIME_FORMATTER.format(dateTime);
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
     * 生成报修编号。
     *
     * @return 报修编号
     */
    private String generateRepairNo() {
        return "ER" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}
