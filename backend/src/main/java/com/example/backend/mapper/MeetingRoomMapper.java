package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.MeetingRoom;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议室表数据访问接口。
 */
@Mapper
public interface MeetingRoomMapper extends BaseMapper<MeetingRoom> {
}
