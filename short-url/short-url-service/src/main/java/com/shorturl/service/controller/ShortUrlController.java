package com.shorturl.service.controller;

import com.shorturl.service.dto.ShortenRequest;
import com.shorturl.service.dto.ShortenResponse;
import com.shorturl.service.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * 短链接 REST API 控制器
 */
@RestController
@RequestMapping("/api")
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    /**
     * 创建短链接
     *
     * POST /api/shorten
     * Body: { "longUrl": "https://...", "expireDays": 30 }
     */
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        ShortenResponse response = shortUrlService.shorten(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 查询短链信息
     *
     * GET /api/shorten/{shortCode}
     */
    @GetMapping("/shorten/{shortCode}")
    public ResponseEntity<?> getShortUrlInfo(@PathVariable String shortCode) {
        Optional<ShortenResponse> opt = shortUrlService.getShortUrlInfo(shortCode);
        if (opt.isPresent()) {
            return ResponseEntity.ok(opt.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "短链接不存在或已过期"));
    }
}
