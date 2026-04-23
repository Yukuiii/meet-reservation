package com.example.backend.vo;

import lombok.Data;

/**
 * 预约改约推荐展示对象。
 */
@Data
public class ReservationRecommendationVO {

    /**
     * 推荐ID。
     */
    private Long id;

    /**
     * 推荐会议室ID。
     */
    private Long roomId;

    /**
     * 推荐会议室名称。
     */
    private String roomName;

    /**
     * 推荐日期。
     */
    private String date;

    /**
     * 推荐时段。
     */
    private String timeSlot;

    /**
     * 推荐状态：0-待处理，1-已接受，2-已放弃。
     */
    private Integer status;

    /**
     * 推荐状态文案。
     */
    private String statusText;

    /**
     * 接受后生成的新预约ID。
     */
    private Long acceptedReservationId;
}
