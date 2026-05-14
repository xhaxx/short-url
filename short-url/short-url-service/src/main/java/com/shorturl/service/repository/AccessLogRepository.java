package com.shorturl.service.repository;

import com.shorturl.service.model.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    /**
     * 删除指定时间之前的日志
     */
    @Modifying
    @Query("DELETE FROM AccessLog a WHERE a.visitTime < :before")
    int deleteOldLogs(@Param("before") LocalDateTime before);
}
