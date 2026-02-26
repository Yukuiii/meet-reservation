package com.example.backend.service;

import com.example.backend.vo.MeetingRoomListItemVO;

import java.util.List;

/**
 * 会议室业务接口。
 */
public interface MeetingRoomService {

    /**
     * 查询会议室列表。
     *
     * @return 会议室列表
     */
    List<MeetingRoomListItemVO> listRooms();
}
