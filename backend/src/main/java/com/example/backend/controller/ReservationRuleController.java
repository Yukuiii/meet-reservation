package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.SaveReservationRuleRequest;
import com.example.backend.service.ReservationRuleService;
import com.example.backend.vo.ReservationRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预约规则控制器。
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReservationRuleController {

    private final ReservationRuleService reservationRuleService;

    /**
     * 查询当前预约规则。
     *
     * @return 预约规则
     */
    @GetMapping("/reservation-rules")
    public ApiResponse<ReservationRuleVO> getRule() {
        try {
            return ApiResponse.success(reservationRuleService.getRule());
        } catch (Exception e) {
            return ApiResponse.fail("查询预约规则失败");
        }
    }

    /**
     * 保存预约规则。
     *
     * @param request 保存参数
     * @return 预约规则
     */
    @PostMapping("/admin/reservation-rules")
    public ApiResponse<ReservationRuleVO> saveRule(@RequestBody SaveReservationRuleRequest request) {
        try {
            return ApiResponse.success("保存预约规则成功", reservationRuleService.saveRule(request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("保存预约规则失败，请稍后重试");
        }
    }
}
