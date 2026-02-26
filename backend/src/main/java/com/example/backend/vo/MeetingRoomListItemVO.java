package com.example.backend.vo;

import lombok.Data;

import java.util.List;

/**
 * 会议室列表项视图对象。
 */
@Data
public class MeetingRoomListItemVO {

    /**
     * 会议室ID。
     */
    private Long id;

    /**
     * 会议室名称。
     */
    private String name;

    /**
     * 容纳人数。
     */
    private Integer capacity;

    /**
     * 位置信息。
     */
    private String location;

    /**
     * 所属楼栋（用于筛选）。
     */
    private String locationBuilding;

    /**
     * 会议室图片。
     */
    private String image;

    /**
     * 设备名称列表。
     */
    private List<String> equipment;

    /**
     * 状态标识（用于前端样式）。
     */
    private String status;

    /**
     * 状态文案。
     */
    private String statusText;
}
