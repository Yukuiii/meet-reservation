package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.ReservationRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约规则表数据访问接口。
 */
@Mapper
public interface ReservationRuleMapper extends BaseMapper<ReservationRule> {
}
