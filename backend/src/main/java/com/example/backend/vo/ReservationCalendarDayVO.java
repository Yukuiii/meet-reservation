package com.example.backend.vo;

import lombok.Data;

import java.util.List;

/**
 * 日历视图单日数据。
 */
@Data
public class ReservationCalendarDayVO {

    /**
     * 日期，格式：yyyy-MM-dd。
     */
    private String date;

    /**
     * 星期文案。
     */
    private String weekDay;

    /**
     * 当日预约数量。
     */
    private Integer totalCount;

    /**
     * 当日预约项列表。
     */
    private List<ReservationCalendarItemVO> items;
}
