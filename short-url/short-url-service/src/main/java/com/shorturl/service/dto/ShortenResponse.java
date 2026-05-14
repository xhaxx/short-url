package com.shorturl.service.dto;

import java.time.LocalDateTime;

/**
 * 短链创建响应
 */
public class ShortenResponse {

    private String shortCode;
    private String shortUrl;
    private String longUrl;
    private LocalDateTime expireTime;

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getShortUrl() { return shortUrl; }
    public void setShortUrl(String shortUrl) { this.shortUrl = shortUrl; }

    public String getLongUrl() { return longUrl; }
    public void setLongUrl(String longUrl) { this.longUrl = longUrl; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
}
