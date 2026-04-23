package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.CancelReservationRequest;
import com.example.backend.dto.CreateReservationRequest;
import com.example.backend.service.ReservationService;
import com.example.backend.vo.CreateReservationResponseVO;
import com.example.backend.vo.ReservationCalendarVO;
import com.example.backend.vo.ReservationScheduleItemVO;
import com.example.backend.vo.UserReservationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 预约管理控制器。
 */
@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 查询会议室指定日期的占用状态。
     *
     * @param roomId 会议室ID
     * @param date   日期，格式：yyyy-MM-dd
     * @return 占用列表
     */
    @GetMapping("/schedule")
    public ApiResponse<List<ReservationScheduleItemVO>> listRoomSchedule(@RequestParam Long roomId,
                                                                         @RequestParam String date) {
        try {
            LocalDate reservationDate = LocalDate.parse(date);
            return ApiResponse.success(reservationService.listRoomSchedule(roomId, reservationDate));
        } catch (DateTimeParseException e) {
            return ApiResponse.fail("日期格式错误，请使用yyyy-MM-dd");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("查询占用状态失败");
        }
    }

    /**
     * 查询预约日历视图数据（按日/周/月）。
     *
     * @param userId   用户ID
     * @param viewType 视图类型：day/week/month
     * @param date     目标日期，格式：yyyy-MM-dd
     * @return 日历数据
     */
    @GetMapping("/calendar")
    public ApiResponse<ReservationCalendarVO> getCalendar(@RequestParam Long userId,
                                                           @RequestParam(required = false) String viewType,
                                                           @RequestParam(required = false) String date) {
        try {
            LocalDate targetDate = StringUtils.hasText(date) ? LocalDate.parse(date) : LocalDate.now();
            return ApiResponse.success(reservationService.getCalendar(userId, viewType, targetDate));
        } catch (DateTimeParseException e) {
            return ApiResponse.fail("日期格式错误，请使用yyyy-MM-dd");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("查询日历数据失败");
        }
    }

    /**
     * 创建预约。
     *
     * @param request 预约参数
     * @return 创建结果
     */
    @PostMapping
    public ApiResponse<CreateReservationResponseVO> createReservation(@RequestBody CreateReservationRequest request) {
        try {
            return ApiResponse.success("预约提交成功", reservationService.createReservation(request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("预约提交失败，请稍后重试");
        }
    }

    /**
     * 查询当前用户预约记录。
     *
     * @param userId 用户ID
     * @return 预约记录列表
     */
    @GetMapping("/my")
    public ApiResponse<List<UserReservationVO>> listMyReservations(@RequestParam Long userId) {
        try {
            return ApiResponse.success(reservationService.listUserReservations(userId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("查询预约记录失败");
        }
    }

    /**
     * 查询预约详情。
     *
     * @param reservationId 预约ID
     * @param userId        用户ID
     * @return 预约详情
     */
    @GetMapping("/{reservationId}")
    public ApiResponse<UserReservationVO> getReservationDetail(@PathVariable Long reservationId,
                                                               @RequestParam Long userId) {
        try {
            return ApiResponse.success(reservationService.getReservationDetail(userId, reservationId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("查询预约详情失败");
        }
    }

    /**
     * 取消预约。
     *
     * @param reservationId 预约ID
     * @param request       取消参数
     * @return 统一响应
     */
    @PostMapping("/{reservationId}/cancel")
    public ApiResponse<Void> cancelReservation(@PathVariable Long reservationId,
                                               @RequestBody CancelReservationRequest request) {
        try {
            if (request == null) {
                return ApiResponse.fail("取消参数不能为空");
            }
            reservationService.cancelReservation(request.getUserId(), reservationId, request.getCancelReason());
            return ApiResponse.success("取消预约成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("取消预约失败，请稍后重试");
        }
    }
}
