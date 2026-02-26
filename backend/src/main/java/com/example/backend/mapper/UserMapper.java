package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表数据访问接口。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
