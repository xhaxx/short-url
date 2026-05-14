package com.shorturl.service.dto;

import java.util.List;

/**
 * 访问统计响应
 */
public class StatsResponse {

    private Long totalPv;
    private Long totalUv;
    private List<DailyStat> dailyStats;
    private List<RegionStat> regionStats;

    // 每日统计
    public static class DailyStat {
        private String date;
        private Long pv;
        private Long uv;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public Long getPv() { return pv; }
        public void setPv(Long pv) { this.pv = pv; }

        public Long getUv() { return uv; }
        public void setUv(Long uv) { this.uv = uv; }
    }

    // 地域统计
    public static class RegionStat {
        private String country;
        private String province;
        private String city;
        private Long pv;
        private Long uv;

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public Long getPv() { return pv; }
        public void setPv(Long pv) { this.pv = pv; }

        public Long getUv() { return uv; }
        public void setUv(Long uv) { this.uv = uv; }
    }

    // --- Getters / Setters ---

    public Long getTotalPv() { return totalPv; }
    public void setTotalPv(Long totalPv) { this.totalPv = totalPv; }

    public Long getTotalUv() { return totalUv; }
    public void setTotalUv(Long totalUv) { this.totalUv = totalUv; }

    public List<DailyStat> getDailyStats() { return dailyStats; }
    public void setDailyStats(List<DailyStat> dailyStats) { this.dailyStats = dailyStats; }

    public List<RegionStat> getRegionStats() { return regionStats; }
    public void setRegionStats(List<RegionStat> regionStats) { this.regionStats = regionStats; }
}
