package com.web.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.common.ApiResponse;
import com.web.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Slf4j
@Component // 注册为 Spring Bean，以便注入 JwtUtil
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 业界规范：将提取出的用户信息放在 Request Attribute 中的固定 Key
    public static final String USERNAME_ATTRIBUTE = "CURRENT_USERNAME";

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 放行前端的 CORS OPTIONS 预检请求
        if (HttpMethod.OPTIONS.name().equals(request.getMethod())) {
            return true;
        }

        // 2. 从 HTTP Header 获取 Authorization
        String authHeader = request.getHeader("Authorization");

        // 3. 校验前缀规范
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("拦截非法请求：未携带合法 Token, 路径: {}", request.getRequestURI());
            returnUnauthorized(response, "请求未授权，请先登录");
            return false;
        }

        // 4. 截取真正的 Token 部分
        String token = authHeader.substring(7);

        try {
            // 5. 验签并解析
            if (jwtUtil.isTokenValid(token)) {
                String username = jwtUtil.extractUsername(token);
                // 6. 关键步骤：把解析出的用户名挂载到本次请求的上下文中
                // 这样后续的 Controller 只需要 request.getAttribute(USERNAME_ATTRIBUTE) 就能拿到人名
                request.setAttribute(USERNAME_ATTRIBUTE, username);
                return true; // 放行
            }
        } catch (Exception e) {
            log.error("Token 解析异常: {}", e.getMessage());
        }

        // Token 校验失败，统一返回 401
        returnUnauthorized(response, "Token 已过期或无效，请重新登录");
        return false;
    }

    /**
     * 辅助方法：返回标准的 401 响应体
     */
    private void returnUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 设置 HTTP 状态码为 401
        response.setContentType("application/json;charset=UTF-8");

        // 保持和全局一致的 ApiResponse 格式
        ApiResponse<Void> failResponse = ApiResponse.fail(401, message);
        response.getWriter().write(objectMapper.writeValueAsString(failResponse));
    }
}