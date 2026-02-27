package com.example.backend.vo;

import lombok.Data;

import java.util.List;

/**
 * 日历视图结果。
 */
@Data
public class ReservationCalendarVO {

    /**
     * 视图类型：day/week。
     */
    private String viewType;

    /**
     * 起始日期，格式：yyyy-MM-dd。
     */
    private String startDate;

    /**
     * 结束日期，格式：yyyy-MM-dd。
     */
    private String endDate;

    /**
     * 区间内预约总数。
     */
    private Integer totalCount;

    /**
     * 分日数据。
     */
    private List<ReservationCalendarDayVO> days;
}
