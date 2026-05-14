package com.shorturl.service.repository;

import com.shorturl.service.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    /**
     * 按短码查询有效链接
     */
    Optional<ShortUrl> findByShortCodeAndStatus(String shortCode, Integer status);

    /**
     * 按长链 hash 去重
     */
    Optional<ShortUrl> findByLongUrlHashAndStatus(String longUrlHash, Integer status);

    /**
     * 查询所有有效短码（用于布隆过滤器重建）
     */
    @Query("SELECT s.shortCode FROM ShortUrl s WHERE s.status = 1")
    List<String> findAllActiveShortCodes();

    /**
     * 查询过期且有效的链接（分批清理）
     */
    @Query("SELECT s FROM ShortUrl s WHERE s.status = 1 AND s.expireTime IS NOT NULL AND s.expireTime < :now")
    List<ShortUrl> findExpiredUrls(@Param("now") LocalDateTime now);

    /**
     * 批量标记为已删除
     */
    @Modifying
    @Query("UPDATE ShortUrl s SET s.status = 0 WHERE s.id IN :ids")
    int batchDelete(@Param("ids") List<Long> ids);
}
