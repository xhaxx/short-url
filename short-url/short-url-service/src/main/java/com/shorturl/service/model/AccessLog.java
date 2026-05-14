package com.shorturl.service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 访问日志表
 */
@Entity
@Table(name = "access_log", indexes = {
        @Index(name = "idx_short_code_time", columnList = "shortCode, visitTime"),
        @Index(name = "idx_visit_time", columnList = "visitTime")
})
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 16)
    private String shortCode;

    @Column(name = "visitor_ip", nullable = false, length = 45)
    private String visitorIp;

    @Column(length = 64)
    private String country;

    @Column(length = 64)
    private String province;

    @Column(length = 64)
    private String city;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(length = 2048)
    private String referer;

    @Column(name = "visit_time", nullable = false)
    private LocalDateTime visitTime;

    @PrePersist
    protected void onCreate() {
        if (visitTime == null) {
            visitTime = LocalDateTime.now();
        }
    }

    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getVisitorIp() { return visitorIp; }
    public void setVisitorIp(String visitorIp) { this.visitorIp = visitorIp; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }

    public LocalDateTime getVisitTime() { return visitTime; }
    public void setVisitTime(LocalDateTime visitTime) { this.visitTime = visitTime; }
}
