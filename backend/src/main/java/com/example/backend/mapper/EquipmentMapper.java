package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Equipment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备表数据访问接口。
 */
@Mapper
public interface EquipmentMapper extends BaseMapper<Equipment> {
}
