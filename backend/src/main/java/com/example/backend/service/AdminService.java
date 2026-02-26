package com.example.backend.service;

import com.example.backend.dto.AdminEmergencyOccupyRequest;
import com.example.backend.dto.AdminReviewReservationRequest;
import com.example.backend.dto.AdminSaveMeetingRoomRequest;
import com.example.backend.vo.AdminEmergencyOccupyVO;
import com.example.backend.vo.AdminEquipmentVO;
import com.example.backend.vo.AdminMeetingRoomVO;
import com.example.backend.vo.AdminReservationVO;
import com.example.backend.vo.AdminStatsVO;

import java.util.List;

/**
 * 管理员业务接口。
 */
public interface AdminService {

    /**
     * 查询待审核预约列表。
     *
     * @param adminUserId 管理员用户ID
     * @return 待审核预约列表
     */
    List<AdminReservationVO> listPendingReservations(Long adminUserId);

    /**
     * 审核预约申请。
     *
     * @param reservationId 预约ID
     * @param request       审核参数
     */
    void reviewReservation(Long reservationId, AdminReviewReservationRequest request);

    /**
     * 查询会议室管理列表。
     *
     * @param adminUserId 管理员用户ID
     * @return 会议室列表
     */
    List<AdminMeetingRoomVO> listMeetingRooms(Long adminUserId);

    /**
     * 新增会议室。
     *
     * @param request 保存参数
     * @return 新会议室ID
     */
    Long createMeetingRoom(AdminSaveMeetingRoomRequest request);

    /**
     * 编辑会议室。
     *
     * @param roomId  会议室ID
     * @param request 保存参数
     */
    void updateMeetingRoom(Long roomId, AdminSaveMeetingRoomRequest request);

    /**
     * 停用会议室。
     *
     * @param roomId      会议室ID
     * @param adminUserId 管理员用户ID
     */
    void disableMeetingRoom(Long roomId, Long adminUserId);

    /**
     * 查询设备选项。
     *
     * @param adminUserId 管理员用户ID
     * @return 设备选项列表
     */
    List<AdminEquipmentVO> listEquipmentOptions(Long adminUserId);

    /**
     * 查询统计概览。
     *
     * @param adminUserId 管理员用户ID
     * @return 统计概览
     */
    AdminStatsVO getStats(Long adminUserId);

    /**
     * 提交紧急占用。
     *
     * @param request 紧急占用参数
     * @return 紧急占用结果
     */
    AdminEmergencyOccupyVO emergencyOccupy(AdminEmergencyOccupyRequest request);
}
