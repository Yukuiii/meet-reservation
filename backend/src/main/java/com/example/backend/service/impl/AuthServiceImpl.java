package com.example.backend.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.AuthService;
import com.example.backend.service.support.UserAccountSupport;
import com.example.backend.vo.LoginResponse;
import com.example.backend.vo.LoginUserInfo;

/**
 * 认证业务实现。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

    /**
     * 用户注册。
     *
     * @param registerRequest 注册参数
     */
    @Override
    public void register(RegisterRequest registerRequest) {
        String username = safeTrim(registerRequest == null ? null : registerRequest.getUsername());
        String phone = safeTrim(registerRequest == null ? null : registerRequest.getPhone());
        String password = safeTrim(registerRequest == null ? null : registerRequest.getPassword());

        validateRegisterParams(username, phone, password);

        User existsUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .last("limit 1")
        );
        if (existsUser != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setNickname(username);
        user.setPhone(phone);
        user.setPassword(UserAccountSupport.encryptPassword(password));
        user.setRole(0);
        user.setStatus(1);

        int rows = userMapper.insert(user);
        if (rows != 1) {
            throw new IllegalStateException("注册失败，请稍后重试");
        }
    }

    /**
     * 用户登录。
     *
     * @param loginRequest 登录参数
     * @return 登录结果
     */
    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        String username = safeTrim(loginRequest == null ? null : loginRequest.getUsername());
        String password = safeTrim(loginRequest == null ? null : loginRequest.getPassword());
        String loginType = normalizeLoginType(loginRequest == null ? null : loginRequest.getLoginType());

        if (username.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .last("limit 1")
        );
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String encryptedPassword = UserAccountSupport.encryptPassword(password);
        // 兼容历史数据：若数据库已有明文密码，也允许通过并在后续流程逐步迁移。
        boolean passwordMatched = Objects.equals(user.getPassword(), encryptedPassword)
                || Objects.equals(user.getPassword(), password);
        if (!passwordMatched) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!Objects.equals(user.getStatus(), 1)) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        if ("admin".equals(loginType) && !Objects.equals(user.getRole(), 1)) {
            throw new IllegalArgumentException("当前账号不是管理员，无法使用管理员登录");
        }
        if ("user".equals(loginType) && Objects.equals(user.getRole(), 1)) {
            throw new IllegalArgumentException("当前账号为管理员，请切换为管理员登录");
        }

        LoginUserInfo userInfo = new LoginUserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setPhone(user.getPhone());
        userInfo.setRole(user.getRole());

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(generateToken(user.getId()));
        loginResponse.setUserInfo(userInfo);
        return loginResponse;
    }

    /**
     * 注册参数校验。
     *
     * @param username 用户名
     * @param phone    手机号
     * @param password 密码
     */
    private void validateRegisterParams(String username, String phone, String password) {
        UserAccountSupport.validateUsername(username);
        UserAccountSupport.validatePhone(phone);
        UserAccountSupport.validatePassword(password);
    }

    /**
     * 空安全的字符串清洗。
     *
     * @param value 原字符串
     * @return 去空格后的字符串
     */
    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 标准化登录类型。
     *
     * @param loginType 原始登录类型
     * @return user 或 admin
     */
    private String normalizeLoginType(String loginType) {
        String finalLoginType = safeTrim(loginType);
        if (finalLoginType.isEmpty()) {
            return "user";
        }
        if ("user".equalsIgnoreCase(finalLoginType)) {
            return "user";
        }
        if ("admin".equalsIgnoreCase(finalLoginType)) {
            return "admin";
        }
        throw new IllegalArgumentException("登录类型不合法");
    }

    /**
     * 生成轻量登录令牌。
     *
     * @param userId 用户ID
     * @return 令牌字符串
     */
    private String generateToken(Long userId) {
        String rawToken = userId + ":" + UUID.randomUUID();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(rawToken.getBytes(StandardCharsets.UTF_8));
    }
}
