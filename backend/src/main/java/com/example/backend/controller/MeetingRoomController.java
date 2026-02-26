package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.service.MeetingRoomService;
import com.example.backend.vo.MeetingRoomListItemVO;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会议室信息控制器。
 */
@RestController
@RequestMapping("/api/meeting-rooms")
@CrossOrigin(origins = "*")
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    /**
     * 构造函数注入。
     *
     * @param meetingRoomService 会议室业务
     */
    public MeetingRoomController(MeetingRoomService meetingRoomService) {
        this.meetingRoomService = meetingRoomService;
    }

    /**
     * 查询会议室列表。
     *
     * @return 会议室列表
     */
    @GetMapping
    public ApiResponse<List<MeetingRoomListItemVO>> listRooms() {
        try {
            return ApiResponse.success(meetingRoomService.listRooms());
        } catch (Exception e) {
            return ApiResponse.fail("查询会议室列表失败");
        }
    }
}
