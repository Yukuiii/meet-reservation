package com.example.backend.controller;

import com.example.backend.common.ApiResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.service.AuthService;
import com.example.backend.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录注册控制器。
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 注册接口。
     *
     * @param registerRequest 注册参数
     * @return 统一响应
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequest registerRequest) {
        try {
            authService.register(registerRequest);
            return ApiResponse.success("注册成功", null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("注册失败，请稍后重试");
        }
    }

    /**
     * 登录接口。
     *
     * @param loginRequest 登录参数
     * @return 统一响应
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = authService.login(loginRequest);
            return ApiResponse.success("登录成功", loginResponse);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("登录失败，请稍后重试");
        }
    }
}
