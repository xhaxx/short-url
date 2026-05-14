package com.shorturl.service.schedule;

import com.google.common.hash.BloomFilter;
import com.shorturl.service.model.AccessLog;
import com.shorturl.service.model.AccessStats;
import com.shorturl.service.model.ShortUrl;
import com.shorturl.service.repository.AccessLogRepository;
import com.shorturl.service.repository.AccessStatsRepository;
import com.shorturl.service.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定时任务调度
 *
 * 1. 过期链接清理（每天凌晨3点）
 * 2. 访问日志归档与聚合（每天凌晨4点）
 * 3. 布隆过滤器重建（每周日凌晨2点）
 */
@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final ShortUrlRepository shortUrlRepository;
    private final AccessLogRepository accessLogRepository;
    private final AccessStatsRepository accessStatsRepository;
    private final StringRedisTemplate redisTemplate;
    private final BloomFilter<String> bloomFilter;
    private final DataSource dataSource;

    private static final int CLEAN_BATCH_SIZE = 500;

    public ScheduledTasks(ShortUrlRepository shortUrlRepository,
                          AccessLogRepository accessLogRepository,
                          AccessStatsRepository accessStatsRepository,
                          StringRedisTemplate redisTemplate,
                          BloomFilter<String> bloomFilter,
                          DataSource dataSource) {
        this.shortUrlRepository = shortUrlRepository;
        this.accessLogRepository = accessLogRepository;
        this.accessStatsRepository = accessStatsRepository;
        this.redisTemplate = redisTemplate;
        this.bloomFilter = bloomFilter;
        this.dataSource = dataSource;
    }

    /**
     * 过期链接清理（每天凌晨3点）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanExpiredUrls() {
        log.info("Starting expired URL cleanup...");
        LocalDateTime now = LocalDateTime.now();
        int totalCleaned = 0;

        while (true) {
            List<ShortUrl> expired = shortUrlRepository.findExpiredUrls(now);
            if (expired.isEmpty()) {
                break;
            }

            List<Long> ids = expired.stream().map(ShortUrl::getId).collect(Collectors.toList());
            int cleaned = shortUrlRepository.batchDelete(ids);
            totalCleaned += cleaned;

            // 删除对应 Redis 缓存
            for (ShortUrl url : expired) {
                redisTemplate.delete("url:" + url.getShortCode());
            }

            if (expired.size() < CLEAN_BATCH_SIZE) {
                break;
            }
        }

        log.info("Expired URL cleanup finished, cleaned {} records", totalCleaned);
    }

    /**
     * 访问日志归档与聚合（每天凌晨4点）
     * 将前一天的 access_log 聚合到 access_stats，并清理 30 天前的日志
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void aggregateStats() {
        log.info("Starting access log aggregation...");
        LocalDate yesterday = LocalDate.now().minusDays(1);

        try {
            // 使用 JDBC 直接聚合，性能优于 JPA 逐条处理
            String aggregateSql = """
                    INSERT INTO access_stats (short_code, stat_date, pv, uv, country, province, city)
                    SELECT
                        short_code,
                        DATE(visit_time) AS stat_date,
                        COUNT(*) AS pv,
                        COUNT(DISTINCT visitor_ip) AS uv,
                        country,
                        province,
                        city
                    FROM access_log
                    WHERE DATE(visit_time) = ?
                    GROUP BY short_code, DATE(visit_time), country, province, city
                    ON DUPLICATE KEY UPDATE
                        pv = pv + VALUES(pv),
                        uv = uv + VALUES(uv)
                    """;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(aggregateSql)) {
                ps.setDate(1, Date.valueOf(yesterday));
                int rows = ps.executeUpdate();
                log.info("Aggregated {} stat rows for {}", rows, yesterday);
            }
        } catch (SQLException e) {
            log.error("Failed to aggregate access stats", e);
        }

        // 清理 30 天前的原始日志
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int deleted = accessLogRepository.deleteOldLogs(thirtyDaysAgo);
        log.info("Deleted {} access logs older than 30 days", deleted);
    }

    /**
     * 布隆过滤器重建（每周日凌晨2点）
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void rebuildBloomFilter() {
        log.info("Starting BloomFilter rebuild...");
        List<String> activeCodes = shortUrlRepository.findAllActiveShortCodes();
        for (String code : activeCodes) {
            bloomFilter.put(code);
        }
        log.info("BloomFilter rebuilt with {} short codes", activeCodes.size());
    }
}
