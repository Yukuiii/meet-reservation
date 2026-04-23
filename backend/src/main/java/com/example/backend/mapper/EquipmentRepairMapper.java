package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.EquipmentRepair;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备报修表数据访问接口。
 */
@Mapper
public interface EquipmentRepairMapper extends BaseMapper<EquipmentRepair> {
}
