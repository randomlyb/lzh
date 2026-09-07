package com.travlejava.ai.service.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.of(
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        // 失败率阈值:50%
                        .failureRateThreshold(50)
                        // 慢调用率阈值:80%
                        .slowCallRateThreshold(80)
                        // 慢调用时间阈值:3秒
                        .slowCallDurationThreshold(Duration.ofSeconds(3))
                        // 熔断器打开后,等待10秒后进入半开状态
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        // 半开状态下允许的请求数
                        .permittedNumberOfCallsInHalfOpenState(3)
                        // 滑动窗口大小
                        .slidingWindowSize(10)
                        // 滑动窗口类型:基于时间
                        .slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                        // 最小请求数
                        .minimumNumberOfCalls(5)
                        .build()
        );
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        return TimeLimiterRegistry.of(
                io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                        // 超时时间:10秒
                        .timeoutDuration(Duration.ofSeconds(10))
                        // 是否取消运行中的任务
                        .cancelRunningFuture(true)
                        .build()
        );
    }
}