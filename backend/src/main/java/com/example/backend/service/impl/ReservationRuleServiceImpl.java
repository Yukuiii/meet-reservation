package com.example.backend.service.impl;

import com.example.backend.dto.SaveReservationRuleRequest;
import com.example.backend.entity.ReservationRule;
import com.example.backend.entity.User;
import com.example.backend.mapper.ReservationRuleMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.ReservationRuleService;
import com.example.backend.vo.ReservationRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 预约规则业务实现。
 */
@Service
@RequiredArgsConstructor
public class ReservationRuleServiceImpl implements ReservationRuleService {

    /**
     * 固定规则ID。
     */
    private static final long RULE_ID = 1L;

    /**
     * 默认最大预约时长，单位分钟。
     */
    private static final int DEFAULT_MAX_DURATION_MINUTES = 120;

    /**
     * 默认最少提前预约时间，单位分钟。
     */
    private static final int DEFAULT_MIN_ADVANCE_MINUTES = 0;

    /**
     * 用户角色：管理员。
     */
    private static final int USER_ROLE_ADMIN = 1;

    /**
     * 用户状态：禁用。
     */
    private static final int USER_STATUS_DISABLED = 0;

    private final ReservationRuleMapper reservationRuleMapper;
    private final UserMapper userMapper;

    /**
     * 获取当前生效预约规则。
     *
     * @return 预约规则
     */
    @Override
    public ReservationRuleVO getRule() {
        ReservationRule rule = reservationRuleMapper.selectById(RULE_ID);
        if (rule == null) {
            return createDefaultRuleVO();
        }
        return toRuleVO(rule);
    }

    /**
     * 保存当前预约规则。
     *
     * @param request 保存参数
     * @return 预约规则
     */
    @Override
    public ReservationRuleVO saveRule(SaveReservationRuleRequest request) {
        validateSaveRequest(request);
        ensureAdminUser(request.getAdminUserId());

        ReservationRule rule = new ReservationRule();
        rule.setId(RULE_ID);
        rule.setMaxDurationMinutes(request.getMaxDurationMinutes());
        rule.setMinAdvanceMinutes(request.getMinAdvanceMinutes());
        rule.setUpdatedAt(LocalDateTime.now());

        ReservationRule existing = reservationRuleMapper.selectById(RULE_ID);
        if (existing == null) {
            rule.setCreatedAt(LocalDateTime.now());
            int rows = reservationRuleMapper.insert(rule);
            if (rows != 1) {
                throw new IllegalStateException("保存预约规则失败，请稍后重试");
            }
        } else {
            int rows = reservationRuleMapper.updateById(rule);
            if (rows != 1) {
                throw new IllegalStateException("保存预约规则失败，请稍后重试");
            }
        }
        return toRuleVO(rule);
    }

    /**
     * 校验保存预约规则请求。
     *
     * @param request 保存参数
     */
    private void validateSaveRequest(SaveReservationRuleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("预约规则参数不能为空");
        }
        if (request.getMaxDurationMinutes() == null || request.getMaxDurationMinutes() <= 0) {
            throw new IllegalArgumentException("单次最大预约时长必须大于0分钟");
        }
        if (request.getMinAdvanceMinutes() == null || request.getMinAdvanceMinutes() < 0) {
            throw new IllegalArgumentException("最少提前预约时间不能小于0分钟");
        }
    }

    /**
     * 校验管理员用户可用。
     *
     * @param adminUserId 管理员用户ID
     */
    private void ensureAdminUser(Long adminUserId) {
        if (adminUserId == null || adminUserId <= 0) {
            throw new IllegalArgumentException("管理员信息缺失");
        }
        User admin = userMapper.selectById(adminUserId);
        if (admin == null || !Integer.valueOf(USER_ROLE_ADMIN).equals(admin.getRole())) {
            throw new IllegalArgumentException("请使用管理员账号操作");
        }
        if (Integer.valueOf(USER_STATUS_DISABLED).equals(admin.getStatus())) {
            throw new IllegalArgumentException("管理员账号已禁用");
        }
    }

    /**
     * 创建默认预约规则视图对象。
     *
     * @return 默认预约规则
     */
    private ReservationRuleVO createDefaultRuleVO() {
        ReservationRuleVO rule = new ReservationRuleVO();
        rule.setMaxDurationMinutes(DEFAULT_MAX_DURATION_MINUTES);
        rule.setMinAdvanceMinutes(DEFAULT_MIN_ADVANCE_MINUTES);
        return rule;
    }

    /**
     * 预约规则实体转换为视图对象。
     *
     * @param rule 预约规则实体
     * @return 预约规则视图对象
     */
    private ReservationRuleVO toRuleVO(ReservationRule rule) {
        ReservationRuleVO vo = createDefaultRuleVO();
        if (rule.getMaxDurationMinutes() != null && rule.getMaxDurationMinutes() > 0) {
            vo.setMaxDurationMinutes(rule.getMaxDurationMinutes());
        }
        if (rule.getMinAdvanceMinutes() != null && rule.getMinAdvanceMinutes() >= 0) {
            vo.setMinAdvanceMinutes(rule.getMinAdvanceMinutes());
        }
        return vo;
    }
}
