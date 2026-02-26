package com.example.backend.dto;

import lombok.Data;

/**
 * 注册请求参数。
 */
@Data
public class RegisterRequest {

    /**
     * 用户名。
     */
    private String username;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 密码。
     */
    private String password;
}
