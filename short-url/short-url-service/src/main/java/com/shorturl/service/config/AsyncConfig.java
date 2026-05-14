package com.shorturl.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步 & 定时任务配置
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * 异步日志写入线程池
     */
    @Bean("accessLogExecutor")
    public Executor accessLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-access-log-");
        executor.setRejectedExecutionHandler((r, e) -> {
            // 队列满时丢弃（日志允许丢失少量）
        });
        executor.initialize();
        return executor;
    }
}
