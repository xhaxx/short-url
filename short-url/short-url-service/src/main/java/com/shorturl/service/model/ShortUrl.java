package com.shorturl.service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 短链接主表
 */
@Entity
@Table(name = "short_url", indexes = {
        @Index(name = "idx_short_code", columnList = "shortCode"),
        @Index(name = "idx_long_url_hash", columnList = "longUrlHash"),
        @Index(name = "idx_expire_time", columnList = "expireTime")
})
public class ShortUrl {

    @Id
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 16)
    private String shortCode;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "long_url_hash", nullable = false, length = 8, columnDefinition = "CHAR(8)")
    private String longUrlHash;

    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Column(nullable = false)
    private Integer status = 1; // 1=有效 0=已删除

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = 1;
        }
    }

    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getLongUrl() { return longUrl; }
    public void setLongUrl(String longUrl) { this.longUrl = longUrl; }

    public String getLongUrlHash() { return longUrlHash; }
    public void setLongUrlHash(String longUrlHash) { this.longUrlHash = longUrlHash; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
