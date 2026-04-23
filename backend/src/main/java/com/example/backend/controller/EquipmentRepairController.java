package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.CreateEquipmentRepairRequest;
import com.example.backend.dto.ResolveEquipmentRepairRequest;
import com.example.backend.service.EquipmentRepairService;
import com.example.backend.vo.EquipmentRepairVO;
import com.example.backend.vo.RepairEquipmentOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备报修控制器。
 */
@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EquipmentRepairController {

    private final EquipmentRepairService equipmentRepairService;

    /**
     * 查询已完成预约可报修设备选项。
     *
     * @param userId        用户ID
     * @param reservationId 预约ID
     * @return 设备选项列表
     */
    @GetMapping("/api/equipment-repairs/options")
    public ApiResponse<List<RepairEquipmentOptionVO>> listRepairableEquipments(
            @RequestParam Long userId,
            @RequestParam Long reservationId) {
        try {
            return ApiResponse.success(equipmentRepairService.listRepairableEquipments(userId, reservationId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("查询可报修设备失败");
        }
    }

    /**
     * 提交设备报修。
     *
     * @param request 报修参数
     * @return 报修ID
     */
    @PostMapping("/api/equipment-repairs")
    public ApiResponse<Long> createRepair(@RequestBody CreateEquipmentRepairRequest request) {
        try {
            return ApiResponse.success("报修提交成功", equipmentRepairService.createRepair(request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("提交报修失败，请稍后重试");
        }
    }

    /**
     * 管理员查询设备报修列表。
     *
     * @param adminUserId 管理员用户ID
     * @return 报修列表
     */
    @GetMapping("/api/admin/equipment-repairs")
    public ApiResponse<List<EquipmentRepairVO>> listAdminRepairs(@RequestParam Long adminUserId) {
        try {
            return ApiResponse.success(equipmentRepairService.listAdminRepairs(adminUserId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("查询报修列表失败");
        }
    }

    /**
     * 管理员标记设备报修已修复。
     *
     * @param repairId 报修ID
     * @param request  修复参数
     * @return 统一响应
     */
    @PostMapping("/api/admin/equipment-repairs/{repairId}/resolve")
    public ApiResponse<Void> resolveRepair(@PathVariable Long repairId,
                                           @RequestBody ResolveEquipmentRepairRequest request) {
        try {
            equipmentRepairService.resolveRepair(repairId, request);
            return ApiResponse.success("报修已标记修复", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("更新报修状态失败，请稍后重试");
        }
    }
}
