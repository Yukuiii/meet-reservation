package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内通知表数据访问接口。
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
