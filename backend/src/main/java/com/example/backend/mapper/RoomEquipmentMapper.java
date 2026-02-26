package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.RoomEquipment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议室设备关联表数据访问接口。
 */
@Mapper
public interface RoomEquipmentMapper extends BaseMapper<RoomEquipment> {
}
