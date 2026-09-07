package com.travlejava.ai.service.config;

import com.travlejava.ai.service.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                // 只限制关键接口
                .addPathPatterns(
                        "/api/rag/**",        // AI对话接口(最耗资源)
                        "/api/search/**",     // 搜索接口
                        "/api/blog/**"        // 博客相关接口
                )
                // 排除不需要限流的接口
                .excludePathPatterns(
                        "/api/health",        // 健康检查
                        "/api/doc/**",        // 文档接口
                        "/api/user/login",    // 登录接口(已有其他防护)
                        "/api/user/register"  // 注册接口
                );
    }
}