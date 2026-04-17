package com.example.backend.dto;

import lombok.Data;

/**
 * 管理员账号保存参数。
 */
@Data
public class AdminSaveAdminRequest {

    /**
     * 当前操作管理员ID。
     */
    private Long adminUserId;

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
     * 邮箱。
     */
    private String email;

    /**
     * 密码。
     */
    private String password;

    /**
     * 状态：0-禁用，1-正常。
     */
    private Integer status;
}
