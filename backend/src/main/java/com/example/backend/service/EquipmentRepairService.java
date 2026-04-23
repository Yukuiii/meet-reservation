package com.example.backend.service;

import com.example.backend.dto.CreateEquipmentRepairRequest;
import com.example.backend.dto.ResolveEquipmentRepairRequest;
import com.example.backend.vo.EquipmentRepairVO;
import com.example.backend.vo.RepairEquipmentOptionVO;

import java.util.List;

/**
 * 设备报修业务接口。
 */
public interface EquipmentRepairService {

    /**
     * 查询指定已完成预约可报修的设备选项。
     *
     * @param userId        用户ID
     * @param reservationId 预约ID
     * @return 设备选项列表
     */
    List<RepairEquipmentOptionVO> listRepairableEquipments(Long userId, Long reservationId);

    /**
     * 创建设备报修记录。
     *
     * @param request 报修参数
     * @return 报修ID
     */
    Long createRepair(CreateEquipmentRepairRequest request);

    /**
     * 查询管理员可见的设备报修列表。
     *
     * @param adminUserId 管理员用户ID
     * @return 报修列表
     */
    List<EquipmentRepairVO> listAdminRepairs(Long adminUserId);

    /**
     * 标记设备报修已修复。
     *
     * @param repairId 报修ID
     * @param request  修复参数
     */
    void resolveRepair(Long repairId, ResolveEquipmentRepairRequest request);
}
