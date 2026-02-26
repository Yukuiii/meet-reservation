package com.example.backend.vo;

import lombok.Data;

/**
 * 登录用户信息。
 */
@Data
public class LoginUserInfo {

    /**
     * 用户ID。
     */
    private Long id;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 用户角色。
     */
    private Integer role;
}
