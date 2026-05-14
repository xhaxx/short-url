package com.shorturl.service.config;

import com.shorturl.common.IpGeoUtils;
import com.shorturl.idgen.DbSegmentIdGenerator;
import com.shorturl.idgen.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 应用启动配置（初始化发号器、GeoIP2 等）
 */
@Configuration
public class ApplicationStartupConfig {

    private static final Logger log = LoggerFactory.getLogger(ApplicationStartupConfig.class);

    @Value("${id-generator.biz-tag:short_url}")
    private String bizTag;

    @Value("${id-generator.step:1000}")
    private int step;

    @Value("${short-url.geoip2.db-path:}")
    private String geoip2DbPath;

    /**
     * 发号器 Bean
     */
    @Bean
    public IdGenerator idGenerator(DataSource dataSource) {
        return new DbSegmentIdGenerator(dataSource, bizTag, step, 0.2);
    }

    /**
     * 启动时初始化
     */
    @Bean
    public CommandLineRunner init(IdGenerator idGenerator) {
        return args -> {
            // 1. 初始化发号器
            idGenerator.init();
            log.info("ID Generator initialized");

            // 2. 初始化 GeoIP2（如果配置了路径）
            if (geoip2DbPath != null && !geoip2DbPath.isEmpty()) {
                IpGeoUtils.init(geoip2DbPath);
            }
        };
    }
}
