package com.shorturl.service.service;

import com.shorturl.common.IpGeoUtils;
import com.shorturl.service.model.AccessLog;
import com.shorturl.service.repository.AccessLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 访问日志异步写入服务
 *
 * 设计：每个短链访问 → 投递到内存队列 → 后台线程批量写入数据库
 */
@Service
public class AccessLogService {

    private static final Logger log = LoggerFactory.getLogger(AccessLogService.class);

    private final AccessLogRepository accessLogRepository;

    private final BlockingQueue<AccessLog> queue;

    @Value("${short-url.access-log.batch-size:200}")
    private int batchSize;

    @Value("${short-url.access-log.flush-interval-ms:5000}")
    private long flushIntervalMs;

    private volatile boolean running = true;
    private final Thread workerThread;

    public AccessLogService(AccessLogRepository accessLogRepository) {
        this.accessLogRepository = accessLogRepository;
        this.queue = new LinkedBlockingQueue<>(100000);
        this.workerThread = new Thread(this::consume, "access-log-writer");
        this.workerThread.setDaemon(true);
        this.workerThread.start();
    }

    /**
     * 异步记录访问日志
     */
    @Async
    public void logAccess(String shortCode, String visitorIp, String userAgent, String referer) {
        try {
            // 地域解析
            String[] geo = IpGeoUtils.resolve(visitorIp);

            AccessLog logEntry = new AccessLog();
            logEntry.setShortCode(shortCode);
            logEntry.setVisitorIp(visitorIp);
            logEntry.setUserAgent(userAgent);
            logEntry.setReferer(referer);
            logEntry.setCountry(geo[0]);
            logEntry.setProvince(geo[1]);
            logEntry.setCity(geo[2]);
            logEntry.setVisitTime(LocalDateTime.now());

            if (!queue.offer(logEntry, 100, TimeUnit.MILLISECONDS)) {
                log.warn("Access log queue full, dropping entry for {}", shortCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while queuing access log");
        }
    }

    /**
     * 后台消费线程：批量写入数据库
     */
    private void consume() {
        List<AccessLog> batch = new ArrayList<>(batchSize);
        long lastFlush = System.currentTimeMillis();

        while (running) {
            try {
                AccessLog entry = queue.poll(1, TimeUnit.SECONDS);
                if (entry != null) {
                    batch.add(entry);
                }

                // 达到批量大小 或 超过刷新间隔 → 写入
                boolean batchFull = batch.size() >= batchSize;
                boolean timeUp = !batch.isEmpty() &&
                        (System.currentTimeMillis() - lastFlush) >= flushIntervalMs;

                if (batchFull || timeUp) {
                    flushBatch(batch);
                    batch.clear();
                    lastFlush = System.currentTimeMillis();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // 退出前刷一次
                if (!batch.isEmpty()) {
                    flushBatch(batch);
                    batch.clear();
                }
                break;
            } catch (Exception e) {
                log.error("Error in access log consumer", e);
            }
        }

        // 最终刷新
        if (!batch.isEmpty()) {
            flushBatch(batch);
        }
    }

    private void flushBatch(List<AccessLog> batch) {
        if (batch.isEmpty()) {
            return;
        }
        try {
            accessLogRepository.saveAll(batch);
            log.debug("Flushed {} access logs to DB", batch.size());
        } catch (Exception e) {
            log.error("Failed to flush {} access logs", batch.size(), e);
        }
    }

    /**
     * 获取队列当前积压量
     */
    public int getQueueSize() {
        return queue.size();
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        workerThread.interrupt();
        log.info("AccessLogService shutdown initiated");
    }
}
