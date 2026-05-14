package com.shorturl.service.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * 统计汇总表（定时任务聚合）
 */
@Entity
@Table(name = "access_stats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"shortCode", "statDate", "country", "province", "city"})
}, indexes = {
        @Index(name = "idx_short_code_date", columnList = "shortCode, statDate")
})
public class AccessStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 16)
    private String shortCode;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(nullable = false)
    private Long pv = 0L;

    @Column(nullable = false)
    private Long uv = 0L;

    @Column(length = 64)
    private String country;

    @Column(length = 64)
    private String province;

    @Column(length = 64)
    private String city;

    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }

    public Long getPv() { return pv; }
    public void setPv(Long pv) { this.pv = pv; }

    public Long getUv() { return uv; }
    public void setUv(Long uv) { this.uv = uv; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
