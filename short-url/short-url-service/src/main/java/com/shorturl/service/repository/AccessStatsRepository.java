package com.shorturl.service.repository;

import com.shorturl.service.model.AccessStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AccessStatsRepository extends JpaRepository<AccessStats, Long> {

    /**
     * 按短码和日期查询统计
     */
    @Query("SELECT a FROM AccessStats a WHERE a.shortCode = :shortCode " +
           "AND a.statDate BETWEEN :startDate AND :endDate ORDER BY a.statDate DESC")
    List<AccessStats> findByShortCodeAndDateRange(@Param("shortCode") String shortCode,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    /**
     * 按短码汇总 PV/UV
     */
    @Query("SELECT COALESCE(SUM(a.pv), 0) FROM AccessStats a WHERE a.shortCode = :shortCode")
    Long sumPvByShortCode(@Param("shortCode") String shortCode);

    @Query("SELECT COALESCE(SUM(a.uv), 0) FROM AccessStats a WHERE a.shortCode = :shortCode")
    Long sumUvByShortCode(@Param("shortCode") String shortCode);
}
