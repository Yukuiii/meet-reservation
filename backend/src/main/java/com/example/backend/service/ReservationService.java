package com.example.backend.service;

import com.example.backend.dto.CreateReservationRequest;
import com.example.backend.vo.ReservationCalendarVO;
import com.example.backend.vo.CreateReservationResponseVO;
import com.example.backend.vo.ReservationScheduleItemVO;
import com.example.backend.vo.UserReservationVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 预约业务接口。
 */
public interface ReservationService {

    /**
     * 查询会议室指定日期的占用时段。
     *
     * @param roomId          会议室ID
     * @param reservationDate 预约日期
     * @return 占用时段列表
     */
    List<ReservationScheduleItemVO> listRoomSchedule(Long roomId, LocalDate reservationDate);

    /**
     * 查询日历视图预约数据。
     *
     * @param userId      用户ID
     * @param viewType    视图类型：day/week/month
     * @param targetDate  目标日期
     * @return 日历数据
     */
    ReservationCalendarVO getCalendar(Long userId, String viewType, LocalDate targetDate);

    /**
     * 创建预约。
     *
     * @param request 创建预约请求
     * @return 创建结果
     */
    CreateReservationResponseVO createReservation(CreateReservationRequest request);

    /**
     * 查询用户预约记录列表。
     *
     * @param userId 用户ID
     * @return 预约记录列表
     */
    List<UserReservationVO> listUserReservations(Long userId);

    /**
     * 查询预约详情。
     *
     * @param userId        用户ID
     * @param reservationId 预约ID
     * @return 预约详情
     */
    UserReservationVO getReservationDetail(Long userId, Long reservationId);

    /**
     * 取消预约。
     *
     * @param userId        用户ID
     * @param reservationId 预约ID
     * @param cancelReason  取消原因
     */
    void cancelReservation(Long userId, Long reservationId, String cancelReason);
}
