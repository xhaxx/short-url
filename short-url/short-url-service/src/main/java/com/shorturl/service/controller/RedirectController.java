package com.shorturl.service.controller;

import com.shorturl.service.service.AccessLogService;
import com.shorturl.service.service.ShortUrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URI;

/**
 * 短链重定向控制器
 *
 * GET /{shortCode} → 302 重定向到原始长链接
 */
@Controller
public class RedirectController {

    private static final Logger log = LoggerFactory.getLogger(RedirectController.class);

    private final ShortUrlService shortUrlService;
    private final AccessLogService accessLogService;

    public RedirectController(ShortUrlService shortUrlService, AccessLogService accessLogService) {
        this.shortUrlService = shortUrlService;
        this.accessLogService = accessLogService;
    }

    /**
     * 短链 → 302 重定向
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        // 1. 查找长链
        String longUrl = shortUrlService.getLongUrl(shortCode);

        if (longUrl == null) {
            log.debug("Short URL not found or expired: {}", shortCode);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("短链接不存在或已过期");
        }

        // 2. 异步记录访问日志
        String visitorIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");
        accessLogService.logAccess(shortCode, visitorIp, userAgent, referer);

        // 3. 302 重定向
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(longUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * 获取客户端真实 IP（考虑代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
