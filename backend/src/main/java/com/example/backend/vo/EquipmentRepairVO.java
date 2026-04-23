package com.example.backend.vo;

import lombok.Data;

/**
 * 设备报修记录视图对象。
 */
@Data
public class EquipmentRepairVO {

    /**
     * 报修ID。
     */
    private Long id;

    /**
     * 报修编号。
     */
    private String repairNo;

    /**
     * 报修用户ID。
     */
    private Long userId;

    /**
     * 报修用户名。
     */
    private String username;

    /**
     * 报修用户昵称。
     */
    private String nickname;

    /**
     * 预约ID。
     */
    private Long reservationId;

    /**
     * 预约编号。
     */
    private String reservationNo;

    /**
     * 会议室ID。
     */
    private Long roomId;

    /**
     * 会议室名称。
     */
    private String roomName;

    /**
     * 设备ID。
     */
    private Long equipmentId;

    /**
     * 设备名称。
     */
    private String equipmentName;

    /**
     * 故障描述。
     */
    private String description;

    /**
     * 状态码。
     */
    private Integer status;

    /**
     * 状态文案。
     */
    private String statusText;

    /**
     * 创建时间。
     */
    private String createdAt;

    /**
     * 修复管理员ID。
     */
    private Long fixedBy;

    /**
     * 修复时间。
     */
    private String fixedAt;

    /**
     * 修复备注。
     */
    private String fixRemark;
}
