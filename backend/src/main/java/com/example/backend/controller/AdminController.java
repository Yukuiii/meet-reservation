package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.AdminEmergencyOccupyRequest;
import com.example.backend.dto.AdminOperateRequest;
import com.example.backend.dto.AdminReviewReservationRequest;
import com.example.backend.dto.AdminSaveEquipmentRequest;
import com.example.backend.dto.AdminSaveMeetingRoomRequest;
import com.example.backend.service.AdminService;
import com.example.backend.vo.AdminEmergencyOccupyVO;
import com.example.backend.vo.AdminEquipmentManageVO;
import com.example.backend.vo.AdminEquipmentVO;
import com.example.backend.vo.AdminMeetingRoomVO;
import com.example.backend.vo.AdminReservationVO;
import com.example.backend.vo.AdminStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员控制器。
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 查询待审核预约列表。
     *
     * @param adminUserId 管理员用户ID
     * @return 待审核预约列表
     */
    @GetMapping("/reservations/pending")
    public ApiResponse<List<AdminReservationVO>> listPendingReservations(@RequestParam Long adminUserId) {
        try {
            return ApiResponse.success(adminService.listPendingReservations(adminUserId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("查询待审核预约失败");
        }
    }

    /**
     * 审核预约。
     *
     * @param reservationId 预约ID
     * @param request       审核参数
     * @return 统一响应
     */
    @PostMapping("/reservations/{reservationId}/review")
    public ApiResponse<Void> reviewReservation(@PathVariable Long reservationId,
                                               @RequestBody AdminReviewReservationRequest request) {
        try {
            adminService.reviewReservation(reservationId, request);
            return ApiResponse.success("审核完成", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("审核预约失败，请稍后重试");
        }
    }

    /**
     * 查询会议室管理列表。
     *
     * @param adminUserId 管理员用户ID
     * @return 会议室列表
     */
    @GetMapping("/meeting-rooms")
    public ApiResponse<List<AdminMeetingRoomVO>> listMeetingRooms(@RequestParam Long adminUserId) {
        try {
            return ApiResponse.success(adminService.listMeetingRooms(adminUserId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("查询会议室列表失败");
        }
    }

    /**
     * 新增会议室。
     *
     * @param request 保存参数
     * @return 新会议室ID
     */
    @PostMapping("/meeting-rooms")
    public ApiResponse<Long> createMeetingRoom(@RequestBody AdminSaveMeetingRoomRequest request) {
        try {
            return ApiResponse.success("新增会议室成功", adminService.createMeetingRoom(request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("新增会议室失败，请稍后重试");
        }
    }

    /**
     * 编辑会议室。
     *
     * @param roomId  会议室ID
     * @param request 保存参数
     * @return 统一响应
     */
    @PutMapping("/meeting-rooms/{roomId}")
    public ApiResponse<Void> updateMeetingRoom(@PathVariable Long roomId,
                                               @RequestBody AdminSaveMeetingRoomRequest request) {
        try {
            adminService.updateMeetingRoom(roomId, request);
            return ApiResponse.success("编辑会议室成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("编辑会议室失败，请稍后重试");
        }
    }

    /**
     * 停用会议室。
     *
     * @param roomId  会议室ID
     * @param request 管理员参数
     * @return 统一响应
     */
    @PostMapping("/meeting-rooms/{roomId}/disable")
    public ApiResponse<Void> disableMeetingRoom(@PathVariable Long roomId,
                                                @RequestBody AdminOperateRequest request) {
        try {
            if (request == null) {
                return ApiResponse.fail("操作参数不能为空");
            }
            adminService.disableMeetingRoom(roomId, request.getAdminUserId());
            return ApiResponse.success("停用会议室成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("停用会议室失败，请稍后重试");
        }
    }

    /**
     * 查询设备选项。
     *
     * @param adminUserId 管理员用户ID
     * @return 设备选项列表
     */
    @GetMapping("/equipments")
    public ApiResponse<List<AdminEquipmentVO>> listEquipmentOptions(@RequestParam Long adminUserId) {
        try {
            return ApiResponse.success(adminService.listEquipmentOptions(adminUserId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("查询设备选项失败");
        }
    }

    /**
     * 查询设备管理列表。
     *
     * @param adminUserId 管理员用户ID
     * @return 设备管理列表
     */
    @GetMapping("/equipments/manage")
    public ApiResponse<List<AdminEquipmentManageVO>> listEquipments(@RequestParam Long adminUserId) {
        try {
            return ApiResponse.success(adminService.listEquipments(adminUserId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("查询设备列表失败");
        }
    }

    /**
     * 新增设备。
     *
     * @param request 保存参数
     * @return 新设备ID
     */
    @PostMapping("/equipments")
    public ApiResponse<Long> createEquipment(@RequestBody AdminSaveEquipmentRequest request) {
        try {
            return ApiResponse.success("新增设备成功", adminService.createEquipment(request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("新增设备失败，请稍后重试");
        }
    }

    /**
     * 编辑设备。
     *
     * @param equipmentId 设备ID
     * @param request     保存参数
     * @return 统一响应
     */
    @PutMapping("/equipments/{equipmentId}")
    public ApiResponse<Void> updateEquipment(@PathVariable Long equipmentId,
                                             @RequestBody AdminSaveEquipmentRequest request) {
        try {
            adminService.updateEquipment(equipmentId, request);
            return ApiResponse.success("编辑设备成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("编辑设备失败，请稍后重试");
        }
    }

    /**
     * 停用设备。
     *
     * @param equipmentId 设备ID
     * @param request     管理员参数
     * @return 统一响应
     */
    @PostMapping("/equipments/{equipmentId}/disable")
    public ApiResponse<Void> disableEquipment(@PathVariable Long equipmentId,
                                              @RequestBody AdminOperateRequest request) {
        try {
            if (request == null) {
                return ApiResponse.fail("操作参数不能为空");
            }
            adminService.disableEquipment(equipmentId, request.getAdminUserId());
            return ApiResponse.success("停用设备成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("停用设备失败，请稍后重试");
        }
    }

    /**
     * 查询统计概览。
     *
     * @param adminUserId 管理员用户ID
     * @return 统计概览
     */
    @GetMapping("/stats")
    public ApiResponse<AdminStatsVO> getStats(@RequestParam Long adminUserId) {
        try {
            return ApiResponse.success(adminService.getStats(adminUserId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("查询统计数据失败");
        }
    }

    /**
     * 紧急占用并可选冲突协调。
     *
     * @param request 紧急占用参数
     * @return 占用结果
     */
    @PostMapping("/reservations/emergency-occupy")
    public ApiResponse<AdminEmergencyOccupyVO> emergencyOccupy(@RequestBody AdminEmergencyOccupyRequest request) {
        try {
            return ApiResponse.success("紧急占用成功", adminService.emergencyOccupy(request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("紧急占用失败，请稍后重试");
        }
    }
}
