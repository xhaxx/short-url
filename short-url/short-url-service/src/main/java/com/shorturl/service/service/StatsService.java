package com.shorturl.service.service;

import com.shorturl.service.dto.StatsResponse;
import com.shorturl.service.model.AccessStats;
import com.shorturl.service.repository.AccessStatsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 访问统计查询服务
 */
@Service
public class StatsService {

    private final AccessStatsRepository accessStatsRepository;

    public StatsService(AccessStatsRepository accessStatsRepository) {
        this.accessStatsRepository = accessStatsRepository;
    }

    /**
     * 查询指定短链的访问统计
     *
     * @param shortCode 短码
     * @param days      查询最近多少天
     */
    public StatsResponse getStats(String shortCode, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        LocalDate endDate = LocalDate.now();

        List<AccessStats> statsList = accessStatsRepository
                .findByShortCodeAndDateRange(shortCode, startDate, endDate);

        StatsResponse response = new StatsResponse();

        // 总 PV/UV
        response.setTotalPv(accessStatsRepository.sumPvByShortCode(shortCode));
        response.setTotalUv(accessStatsRepository.sumUvByShortCode(shortCode));

        // 每日统计
        response.setDailyStats(statsList.stream()
                .collect(Collectors.groupingBy(AccessStats::getStatDate, Collectors.summarizingLong(AccessStats::getPv)))
                .entrySet().stream()
                .map(e -> {
                    StatsResponse.DailyStat ds = new StatsResponse.DailyStat();
                    ds.setDate(e.getKey().toString());
                    ds.setPv(e.getValue().getSum());
                    ds.setUv(0L); // UV 需要从另一个维度聚合
                    return ds;
                })
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList()));

        // 地域统计
        response.setRegionStats(statsList.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCountry() + "|" + s.getProvince() + "|" + s.getCity(),
                        Collectors.summarizingLong(AccessStats::getPv)))
                .entrySet().stream()
                .map(e -> {
                    String[] parts = e.getKey().split("\\|");
                    StatsResponse.RegionStat rs = new StatsResponse.RegionStat();
                    rs.setCountry(nullSafe(parts, 0));
                    rs.setProvince(nullSafe(parts, 1));
                    rs.setCity(nullSafe(parts, 2));
                    rs.setPv(e.getValue().getSum());
                    rs.setUv(0L);
                    return rs;
                })
                .sorted((a, b) -> Long.compare(b.getPv(), a.getPv()))
                .collect(Collectors.toList()));

        return response;
    }

    private String nullSafe(String[] parts, int index) {
        if (index < parts.length && !"null".equals(parts[index])) {
            return parts[index];
        }
        return null;
    }
}
