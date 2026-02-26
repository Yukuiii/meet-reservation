package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.entity.Equipment;
import com.example.backend.entity.MeetingRoom;
import com.example.backend.entity.RoomEquipment;
import com.example.backend.entity.RoomImage;
import com.example.backend.mapper.EquipmentMapper;
import com.example.backend.mapper.MeetingRoomMapper;
import com.example.backend.mapper.RoomEquipmentMapper;
import com.example.backend.mapper.RoomImageMapper;
import com.example.backend.service.MeetingRoomService;
import com.example.backend.vo.MeetingRoomListItemVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 会议室业务实现。
 */
@Service
public class MeetingRoomServiceImpl implements MeetingRoomService {

    /**
     * 默认图片，用于兜底无图会议室。
     */
    private static final String DEFAULT_ROOM_IMAGE =
            "https://images.unsplash.com/photo-1497366216548-37526070297c?w=600&h=400&fit=crop";

    private final MeetingRoomMapper meetingRoomMapper;
    private final RoomImageMapper roomImageMapper;
    private final RoomEquipmentMapper roomEquipmentMapper;
    private final EquipmentMapper equipmentMapper;

    /**
     * 构造函数注入。
     *
     * @param meetingRoomMapper  会议室数据访问对象
     * @param roomImageMapper    会议室图片数据访问对象
     * @param roomEquipmentMapper 会议室设备关联数据访问对象
     * @param equipmentMapper    设备数据访问对象
     */
    public MeetingRoomServiceImpl(MeetingRoomMapper meetingRoomMapper,
                                  RoomImageMapper roomImageMapper,
                                  RoomEquipmentMapper roomEquipmentMapper,
                                  EquipmentMapper equipmentMapper) {
        this.meetingRoomMapper = meetingRoomMapper;
        this.roomImageMapper = roomImageMapper;
        this.roomEquipmentMapper = roomEquipmentMapper;
        this.equipmentMapper = equipmentMapper;
    }

    /**
     * 查询会议室列表。
     *
     * @return 会议室列表
     */
    @Override
    public List<MeetingRoomListItemVO> listRooms() {
        List<MeetingRoom> rooms = meetingRoomMapper.selectList(
                new LambdaQueryWrapper<MeetingRoom>()
                        .ne(MeetingRoom::getStatus, 0)
                        .orderByDesc(MeetingRoom::getSortOrder)
                        .orderByAsc(MeetingRoom::getId)
        );
        if (rooms.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roomIds = rooms.stream().map(MeetingRoom::getId).toList();

        Map<Long, String> firstImageMap = buildFirstImageMap(roomIds);
        Map<Long, List<String>> roomEquipmentMap = buildRoomEquipmentMap(roomIds);

        List<MeetingRoomListItemVO> result = new ArrayList<>(rooms.size());
        for (MeetingRoom room : rooms) {
            result.add(buildRoomItem(room, firstImageMap, roomEquipmentMap));
        }
        return result;
    }

    /**
     * 根据ID查询会议室详情。
     *
     * @param roomId 会议室ID
     * @return 会议室详情
     */
    @Override
    public MeetingRoomListItemVO getRoomById(Long roomId) {
        MeetingRoom room = meetingRoomMapper.selectById(roomId);
        if (room == null || Integer.valueOf(0).equals(room.getStatus())) {
            return null;
        }

        Map<Long, String> firstImageMap = buildFirstImageMap(Collections.singletonList(roomId));
        Map<Long, List<String>> roomEquipmentMap = buildRoomEquipmentMap(Collections.singletonList(roomId));
        return buildRoomItem(room, firstImageMap, roomEquipmentMap);
    }

    /**
     * 构建会议室返回对象。
     *
     * @param room             会议室实体
     * @param firstImageMap    图片映射
     * @param roomEquipmentMap 设备映射
     * @return 会议室返回对象
     */
    private MeetingRoomListItemVO buildRoomItem(MeetingRoom room,
                                                Map<Long, String> firstImageMap,
                                                Map<Long, List<String>> roomEquipmentMap) {
        MeetingRoomListItemVO item = new MeetingRoomListItemVO();
        item.setId(room.getId());
        item.setName(room.getName());
        item.setCapacity(room.getCapacity());
        item.setLocation(room.getLocation());
        item.setLocationBuilding(extractBuilding(room));
        item.setImage(pickRoomImage(room, firstImageMap.get(room.getId())));
        item.setDescription(room.getDescription());
        item.setEquipment(roomEquipmentMap.getOrDefault(room.getId(), Collections.emptyList()));
        item.setStatus(mapStatus(room.getStatus()));
        item.setStatusText(mapStatusText(room.getStatus()));
        return item;
    }

    /**
     * 构建会议室首图映射。
     *
     * @param roomIds 会议室ID列表
     * @return key为会议室ID，value为首图URL
     */
    private Map<Long, String> buildFirstImageMap(List<Long> roomIds) {
        List<RoomImage> roomImages = roomImageMapper.selectList(
                new LambdaQueryWrapper<RoomImage>()
                        .in(RoomImage::getRoomId, roomIds)
                        .orderByAsc(RoomImage::getSortOrder)
                        .orderByAsc(RoomImage::getId)
        );

        Map<Long, String> firstImageMap = new HashMap<>();
        // 仅记录每个会议室第一张图，后续同 roomId 的图片会被忽略。
        for (RoomImage roomImage : roomImages) {
            if (StringUtils.hasText(roomImage.getImageUrl())) {
                firstImageMap.putIfAbsent(roomImage.getRoomId(), roomImage.getImageUrl());
            }
        }
        return firstImageMap;
    }

    /**
     * 构建会议室设备名称映射。
     *
     * @param roomIds 会议室ID列表
     * @return key为会议室ID，value为设备名称列表
     */
    private Map<Long, List<String>> buildRoomEquipmentMap(List<Long> roomIds) {
        List<RoomEquipment> roomEquipmentList = roomEquipmentMapper.selectList(
                new LambdaQueryWrapper<RoomEquipment>()
                        .in(RoomEquipment::getRoomId, roomIds)
                        .orderByAsc(RoomEquipment::getId)
        );
        if (roomEquipmentList.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> equipmentIds = roomEquipmentList.stream()
                .map(RoomEquipment::getEquipmentId)
                .collect(Collectors.toSet());

        List<Equipment> equipmentList = equipmentMapper.selectList(
                new LambdaQueryWrapper<Equipment>()
                        .in(Equipment::getId, equipmentIds)
        );

        Map<Long, String> equipmentNameMap = equipmentList.stream()
                .filter(equipment -> StringUtils.hasText(equipment.getName()))
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName));

        Map<Long, Set<String>> roomEquipNameSetMap = new LinkedHashMap<>();
        // 先按 roomId 聚合为集合，去重后再转列表，避免重复设备展示。
        for (RoomEquipment roomEquipment : roomEquipmentList) {
            String equipmentName = equipmentNameMap.get(roomEquipment.getEquipmentId());
            if (!StringUtils.hasText(equipmentName)) {
                continue;
            }
            roomEquipNameSetMap
                    .computeIfAbsent(roomEquipment.getRoomId(), key -> new TreeSet<>())
                    .add(equipmentName);
        }

        Map<Long, List<String>> roomEquipmentMap = new HashMap<>();
        for (Map.Entry<Long, Set<String>> entry : roomEquipNameSetMap.entrySet()) {
            roomEquipmentMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return roomEquipmentMap;
    }

    /**
     * 提取楼栋值。
     *
     * @param room 会议室实体
     * @return 楼栋值
     */
    private String extractBuilding(MeetingRoom room) {
        if (StringUtils.hasText(room.getBuilding())) {
            return room.getBuilding().trim();
        }
        if (!StringUtils.hasText(room.getLocation())) {
            return "";
        }
        String location = room.getLocation().trim();
        int index = location.indexOf("栋");
        if (index > 0) {
            return location.substring(0, index);
        }
        return "";
    }

    /**
     * 选择会议室展示图片。
     *
     * @param room          会议室实体
     * @param fallbackImage 图片表首图
     * @return 最终图片URL
     */
    private String pickRoomImage(MeetingRoom room, String fallbackImage) {
        if (StringUtils.hasText(room.getCoverImage())) {
            return room.getCoverImage();
        }
        if (StringUtils.hasText(fallbackImage)) {
            return fallbackImage;
        }
        return DEFAULT_ROOM_IMAGE;
    }

    /**
     * 映射状态标识。
     *
     * @param status 数据库存储状态
     * @return 前端状态标识
     */
    private String mapStatus(Integer status) {
        if (status == null) {
            return "occupied";
        }
        return switch (status) {
            case 1 -> "available";
            case 2 -> "reserved";
            default -> "occupied";
        };
    }

    /**
     * 映射状态文案。
     *
     * @param status 数据库存储状态
     * @return 前端状态文案
     */
    private String mapStatusText(Integer status) {
        if (status == null) {
            return "不可用";
        }
        return switch (status) {
            case 1 -> "可预约";
            case 2 -> "维护中";
            default -> "不可用";
        };
    }
}
