package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约表数据访问接口。
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}
