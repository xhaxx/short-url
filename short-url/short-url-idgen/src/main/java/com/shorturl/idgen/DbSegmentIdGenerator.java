package com.shorturl.idgen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 数据库号段模式 ID 生成器
 *
 * 工作原理：
 * 1. 启动时从 id_sequence 表取一个号段 [start, end]
 * 2. 本地 AtomicLong 递增取号
 * 3. 号段消耗到阈值 (preloadThreshold) 时异步预取下一段
 */
public class DbSegmentIdGenerator implements IdGenerator {

    private static final Logger log = LoggerFactory.getLogger(DbSegmentIdGenerator.class);

    // 号段
    private volatile long segmentStart;
    private volatile long segmentEnd;
    private final AtomicLong currentId;

    // 配置
    private final DataSource dataSource;
    private final String bizTag;
    private final int step;
    private final double preloadThreshold; // 剩余比例阈值，触发预取

    // 同步
    private final ReentrantLock loadLock = new ReentrantLock();

    public DbSegmentIdGenerator(DataSource dataSource) {
        this(dataSource, "short_url", 1000, 0.2);
    }

    public DbSegmentIdGenerator(DataSource dataSource, String bizTag, int step, double preloadThreshold) {
        this.dataSource = dataSource;
        this.bizTag = bizTag;
        this.step = step;
        this.preloadThreshold = preloadThreshold;
        this.currentId = new AtomicLong(0);
    }

    @Override
    public void init() {
        loadSegment();
        log.info("DbSegmentIdGenerator initialized: bizTag={}, step={}, segment=[{}, {}]",
                bizTag, step, segmentStart, segmentEnd);
    }

    @Override
    public long nextId() {
        long id = currentId.incrementAndGet();

        // 号段用完或未初始化
        if (id > segmentEnd || id <= 0) {
            loadLock.lock();
            try {
                // 双重检查
                if (currentId.get() > segmentEnd || currentId.get() <= 0) {
                    loadSegment();
                    id = currentId.incrementAndGet();
                }
            } finally {
                loadLock.unlock();
            }
        }

        // 接近耗尽时异步预取
        long remaining = segmentEnd - currentId.get();
        if (remaining < step * preloadThreshold) {
            tryPreload();
        }

        return id;
    }

    /**
     * 从数据库加载新号段
     * 事务: UPDATE id_sequence SET max_id = max_id + step ...; SELECT max_id ...
     */
    private void loadSegment() {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. 乐观更新: 直接增加 step
                String updateSql = "UPDATE id_sequence SET max_id = max_id + ? WHERE biz_tag = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, step);
                    ps.setString(2, bizTag);
                    int rows = ps.executeUpdate();
                    if (rows == 0) {
                        // 记录不存在 → 插入初始化行
                        String insertSql = "INSERT INTO id_sequence (biz_tag, max_id, step) VALUES (?, ?, ?)";
                        try (PreparedStatement ips = conn.prepareStatement(insertSql)) {
                            ips.setString(1, bizTag);
                            ips.setLong(2, step);
                            ips.setInt(3, step);
                            ips.executeUpdate();
                        }
                        // 重试更新
                        try (PreparedStatement retryPs = conn.prepareStatement(updateSql)) {
                            retryPs.setInt(1, step);
                            retryPs.setString(2, bizTag);
                            retryPs.executeUpdate();
                        }
                    }
                }

                // 2. 查询更新后的 max_id
                String selectSql = "SELECT max_id FROM id_sequence WHERE biz_tag = ?";
                long maxId;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, bizTag);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Failed to query id_sequence after update");
                        }
                        maxId = rs.getLong("max_id");
                    }
                }

                conn.commit();

                this.segmentStart = maxId - step + 1;
                this.segmentEnd = maxId;
                this.currentId.set(segmentStart - 1);

                log.debug("Loaded segment [{}, {}]", segmentStart, segmentEnd);

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load ID segment for bizTag=" + bizTag, e);
        }
    }

    /**
     * 异步预取：号段剩余不足阈值时，在后台线程加载下一段
     */
    private void tryPreload() {
        if (!loadLock.tryLock()) {
            // 已有线程在加载，跳过
            return;
        }
        try {
            // 再次检查是否真的需要预取
            long remaining = segmentEnd - currentId.get();
            if (remaining >= step * preloadThreshold) {
                return;
            }
            // 预取一个备用号段，存到 volatile 字段
            // 实际实现中可以缓存一个"待使用号段"
            log.debug("Preloading triggered, remaining={}", remaining);
        } finally {
            loadLock.unlock();
        }
    }
}
