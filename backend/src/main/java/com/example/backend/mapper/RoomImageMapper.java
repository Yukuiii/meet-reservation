package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.RoomImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会议室图片表数据访问接口。
 */
@Mapper
public interface RoomImageMapper extends BaseMapper<RoomImage> {
}
