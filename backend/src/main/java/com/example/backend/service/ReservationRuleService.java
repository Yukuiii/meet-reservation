package com.example.backend.service;

import com.example.backend.dto.SaveReservationRuleRequest;
import com.example.backend.vo.ReservationRuleVO;

/**
 * 预约规则业务接口。
 */
public interface ReservationRuleService {

    /**
     * 获取当前生效预约规则。
     *
     * @return 预约规则
     */
    ReservationRuleVO getRule();

    /**
     * 保存当前预约规则。
     *
     * @param request 保存参数
     * @return 预约规则
     */
    ReservationRuleVO saveRule(SaveReservationRuleRequest request);
}
