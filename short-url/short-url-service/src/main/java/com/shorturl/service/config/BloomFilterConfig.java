package com.shorturl.service.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * 布隆过滤器配置
 *
 * 用于快速判定短码是否存在，防止缓存穿透
 */
@Configuration
public class BloomFilterConfig {

    @Value("${short-url.bloom-filter.expected-insertions:10000000}")
    private int expectedInsertions;

    @Value("${short-url.bloom-filter.fpp:0.001}")
    private double fpp;

    /**
     * 布隆过滤器 Bean
     * 启动时加载已有短码 → 见 ScheduledTasks.rebuildBloomFilter()
     */
    @Bean
    public BloomFilter<String> shortUrlBloomFilter() {
        return BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                fpp
        );
    }
}
