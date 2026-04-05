package com.web.controller;

import com.web.common.ApiResponse;
import com.web.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    // 依赖注入：把我们刚刚写好的加工厂老板（Service）请过来
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // 定义前端访问的路径：GET /api/dashboard/summary
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getSummary() {

        // 1. 让加工厂去干活，直接拿回打包好的“大包裹” (Map)
        Map<String, Object> data = dashboardService.getDashboardData();

        // 2. 用你们项目统一的 ApiResponse 包装一下（加上 code:0 和 message:"ok"），扔给前端
        return ApiResponse.ok(data);
    }
}