package com.shorturl.service.controller;

import com.shorturl.service.dto.StatsResponse;
import com.shorturl.service.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 统计查询控制器
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * 查询短链访问统计
     *
     * GET /api/stats/{shortCode}?days=30
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<StatsResponse> getStats(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "30") int days) {
        StatsResponse stats = statsService.getStats(shortCode, days);
        return ResponseEntity.ok(stats);
    }
}
