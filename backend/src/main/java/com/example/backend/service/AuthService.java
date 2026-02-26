package com.example.backend.service;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.vo.LoginResponse;

/**
 * 认证业务接口。
 */
public interface AuthService {

    /**
     * 用户注册。
     *
     * @param registerRequest 注册参数
     */
    void register(RegisterRequest registerRequest);

    /**
     * 用户登录。
     *
     * @param loginRequest 登录参数
     * @return 登录结果
     */
    LoginResponse login(LoginRequest loginRequest);
}
