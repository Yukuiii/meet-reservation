package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.ReservationRecommendation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约改约推荐表数据访问接口。
 */
@Mapper
public interface ReservationRecommendationMapper extends BaseMapper<ReservationRecommendation> {
}
