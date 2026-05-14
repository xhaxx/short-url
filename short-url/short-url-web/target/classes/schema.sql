-- =============================================
-- 短链接服务 数据库初始化脚本
-- =============================================

-- 发号器序列表
CREATE TABLE IF NOT EXISTS id_sequence (
    biz_tag     VARCHAR(32)   NOT NULL COMMENT '业务标识',
    max_id      BIGINT        NOT NULL DEFAULT 0 COMMENT '当前已分配的最大ID',
    step        INT           NOT NULL DEFAULT 1000 COMMENT '每次预取的步长',
    PRIMARY KEY (biz_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发号器序列表';

-- 插入默认发号器记录（如不存在）
INSERT IGNORE INTO id_sequence (biz_tag, max_id, step) VALUES ('short_url', 0, 1000);

-- 短链接主表
CREATE TABLE IF NOT EXISTS short_url (
    id              BIGINT        NOT NULL COMMENT '发号器生成的ID',
    short_code      VARCHAR(16)   NOT NULL COMMENT '短码(Base62)',
    long_url        VARCHAR(2048) NOT NULL COMMENT '原始长链接',
    long_url_hash   CHAR(8)       NOT NULL COMMENT '长链MD5前8位,用于去重',
    expire_time     DATETIME      DEFAULT NULL COMMENT '过期时间,NULL=永不过期',
    status          TINYINT       NOT NULL DEFAULT 1 COMMENT '1=有效 0=已删除',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_short_code (short_code),
    INDEX idx_long_url_hash (long_url_hash),
    INDEX idx_expire_time (expire_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链接主表';

-- 访问日志表
CREATE TABLE IF NOT EXISTS access_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    short_code      VARCHAR(16)   NOT NULL COMMENT '短码',
    visitor_ip      VARCHAR(45)   NOT NULL COMMENT '访问者IP',
    country         VARCHAR(64)   DEFAULT NULL COMMENT '国家',
    province        VARCHAR(64)   DEFAULT NULL COMMENT '省份',
    city            VARCHAR(64)   DEFAULT NULL COMMENT '城市',
    user_agent      VARCHAR(512)  DEFAULT NULL COMMENT 'User-Agent',
    referer         VARCHAR(2048) DEFAULT NULL COMMENT 'Referer',
    visit_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    PRIMARY KEY (id),
    INDEX idx_short_code_time (short_code, visit_time),
    INDEX idx_visit_time (visit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问日志表';

-- 统计汇总表（定时任务聚合）
CREATE TABLE IF NOT EXISTS access_stats (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    short_code      VARCHAR(16)   NOT NULL COMMENT '短码',
    stat_date       DATE          NOT NULL COMMENT '统计日期',
    pv              BIGINT        NOT NULL DEFAULT 0 COMMENT '页面访问量',
    uv              BIGINT        NOT NULL DEFAULT 0 COMMENT '独立访客数(按IP去重)',
    country         VARCHAR(64)   DEFAULT NULL COMMENT '国家',
    province        VARCHAR(64)   DEFAULT NULL COMMENT '省份',
    city            VARCHAR(64)   DEFAULT NULL COMMENT '城市',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code_date_region (short_code, stat_date, country, province, city),
    INDEX idx_short_code_date (short_code, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统计汇总表';
