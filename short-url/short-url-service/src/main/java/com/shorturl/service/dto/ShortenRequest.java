package com.shorturl.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 短链创建请求
 */
public class ShortenRequest {

    @NotBlank(message = "longUrl 不能为空")
    @Pattern(regexp = "^https?://.+$", message = "longUrl 必须以 http:// 或 https:// 开头")
    private String longUrl;

    /**
     * 过期天数，null 表示使用默认值
     */
    private Integer expireDays;

    public String getLongUrl() { return longUrl; }
    public void setLongUrl(String longUrl) { this.longUrl = longUrl; }

    public Integer getExpireDays() { return expireDays; }
    public void setExpireDays(Integer expireDays) { this.expireDays = expireDays; }
}
