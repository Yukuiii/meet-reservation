package com.example.backend.dto;

import lombok.Data;

import java.util.List;

/**
 * 管理员保存会议室请求参数（新增/编辑共用）。
 */
@Data
public class AdminSaveMeetingRoomRequest {

    /**
     * 管理员用户ID。
     */
    private Long adminUserId;

    /**
     * 会议室名称。
     */
    private String name;

    /**
     * 容纳人数。
     */
    private Integer capacity;

    /**
     * 位置描述。
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
     * 会议室描述。
     */
    private String description;

    /**
     * 封面图URL。
     */
    private String coverImage;

    /**
     * 会议室状态：0-停用，1-正常，2-维护中。
     */
    private Integer status;

    /**
     * 排序权重。
     */
    private Integer sortOrder;

    /**
     * 设备ID列表。
     */
    private List<Long> equipmentIds;
}
