package com.web.controller;

import com.web.common.ApiResponse;
import com.web.dto.AuthRequest;
import com.web.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // 只注入 AuthService (厨师长)
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody AuthRequest request) {
        try {
            // 把活儿丢给 Service
            authService.register(request);
            return ApiResponse.ok("注册成功");
        } catch (RuntimeException e) {
            // 如果 Service 抛出异常（如用户名重复），在这里捕获并返回给前端
            return ApiResponse.fail(1001, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody AuthRequest request) {
        try {
            // 找 Service 要 Token
            String token = authService.login(request);

            // 组装返回给前端的数据结构
            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            data.put("username", request.getUsername());

            return ApiResponse.ok(data);
        } catch (RuntimeException e) {
            // 密码错误或用户不存在等异常
            return ApiResponse.fail(1002, e.getMessage());
        }
    }
}