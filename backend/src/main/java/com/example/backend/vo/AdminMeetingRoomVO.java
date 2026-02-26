package com.example.backend.vo;

import lombok.Data;

import java.util.List;

/**
 * 管理员会议室对象。
 */
@Data
public class AdminMeetingRoomVO {

    /**
     * 会议室ID。
     */
    private Long id;

    /**
     * 名称。
     */
    private String name;

    /**
     * 容量。
     */
    private Integer capacity;

    /**
     * 位置。
     */
    private String location;

    /**
     * 楼栋。
     */
    private String building;

    /**
     * 楼层。
     */
    private String floor;

    /**
     * 描述。
     */
    private String description;

    /**
     * 封面图URL。
     */
    private String coverImage;

    /**
     * 状态码。
     */
    private Integer status;

    /**
     * 状态文案。
     */
    private String statusText;

    /**
     * 排序权重。
     */
    private Integer sortOrder;

    /**
     * 设备ID列表。
     */
    private List<Long> equipmentIds;

    /**
     * 设备名称列表。
     */
    private List<String> equipmentNames;
}
