package com.web.config;

import com.web.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    // 注入我们刚才写的拦截器
    public WebConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // [原有代码保持不变] 处理静态资源映射
        String uploadPath = Paths.get("uploads")
                .toAbsolutePath()
                .toUri()
                .toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有 /api 开头的核心接口
                .addPathPatterns("/api/**")
                // 白名单放行：登录注册接口、健康检查、静态资源
                .excludePathPatterns(
                        "/api/auth/**",
                        "/health",
                        "/uploads/**"
                );
    }
}