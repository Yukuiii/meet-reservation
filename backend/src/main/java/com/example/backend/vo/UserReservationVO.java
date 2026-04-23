package com.example.backend.vo;

import lombok.Data;

/**
 * 用户预约记录视图对象。
 */
@Data
public class UserReservationVO {

    /**
     * 预约ID。
     */
    private Long id;

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
     * 预约日期，格式：yyyy-MM-dd。
     */
    private String date;

    /**
     * 开始时间，格式：HH:mm。
     */
    private String startTime;

    /**
     * 结束时间，格式：HH:mm。
     */
    private String endTime;

    /**
     * 时段文本，格式：HH:mm-HH:mm。
     */
    private String timeSlot;

    /**
     * 会议主题。
     */
    private String title;

    /**
     * 预约事由。
     */
    private String purpose;

    /**
     * 参与人数。
     */
    private Integer attendees;

    /**
     * 状态码。
     */
    private Integer status;

    /**
     * 状态键（前端样式使用）。
     */
    private String statusKey;

    /**
     * 状态文案。
     */
    private String statusText;

    /**
     * 取消原因。
     */
    private String cancelReason;

    /**
     * 拒绝原因。
     */
    private String rejectReason;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 是否允许取消。
     */
    private Boolean canCancel;

    /**
     * 是否允许设备报修。
     */
    private Boolean canReportRepair;
}
