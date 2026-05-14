package com.shorturl.service.service;

import com.shorturl.common.Base62Utils;
import com.shorturl.common.Md5Utils;
import com.shorturl.idgen.IdGenerator;
import com.shorturl.service.dto.ShortenRequest;
import com.shorturl.service.dto.ShortenResponse;
import com.shorturl.service.model.ShortUrl;
import com.shorturl.service.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 短链接核心服务
 * 负责长链→短链 和 短链→长链 的映射
 */
@Service
public class ShortUrlService {

    private static final Logger log = LoggerFactory.getLogger(ShortUrlService.class);

    private final ShortUrlRepository shortUrlRepository;
    private final IdGenerator idGenerator;

    @Value("${short-url.domain:https://s.cn}")
    private String domain;

    @Value("${short-url.default-expire-days:90}")
    private int defaultExpireDays;

    public ShortUrlService(ShortUrlRepository shortUrlRepository, IdGenerator idGenerator) {
        this.shortUrlRepository = shortUrlRepository;
        this.idGenerator = idGenerator;
    }

    /**
     * 长链 → 短链
     * 1. 计算长链 hash 去重
     * 2. 发号器取 ID
     * 3. Base62 编码
     * 4. 入库
     */
    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        String longUrl = request.getLongUrl();

        // 1. 去重：相同长链返回已有短码
        String hash = Md5Utils.md5First8(longUrl);
        Optional<ShortUrl> existing = shortUrlRepository.findByLongUrlHashAndStatus(hash, 1);
        if (existing.isPresent()) {
            log.debug("Duplicate long URL detected, returning existing short code: {}", existing.get().getShortCode());
            return toResponse(existing.get());
        }

        // 2. 获取 ID
        long id = idGenerator.nextId();

        // 3. Base62 编码
        String shortCode = Base62Utils.encode(id);

        // 4. 计算过期时间
        int expireDays = request.getExpireDays() != null ? request.getExpireDays() : defaultExpireDays;
        LocalDateTime expireTime = LocalDateTime.now().plusDays(expireDays);

        // 5. 入库
        ShortUrl entity = new ShortUrl();
        entity.setId(id);
        entity.setShortCode(shortCode);
        entity.setLongUrl(longUrl);
        entity.setLongUrlHash(hash);
        entity.setExpireTime(expireTime);
        entity.setStatus(1);

        shortUrlRepository.save(entity);

        log.info("Short URL created: {} -> {}", shortCode, longUrl);
        return toResponse(entity);
    }

    /**
     * 短链 → 长链（用于重定向）
     *
     * @return 长链 URL，未找到返回 null
     */
    public String getLongUrl(String shortCode) {
        Optional<ShortUrl> opt = shortUrlRepository.findByShortCodeAndStatus(shortCode, 1);
        if (opt.isEmpty()) {
            return null;
        }
        ShortUrl entity = opt.get();

        // 检查是否过期
        if (entity.getExpireTime() != null && entity.getExpireTime().isBefore(LocalDateTime.now())) {
            log.debug("Short URL expired: {}", shortCode);
            return null;
        }

        return entity.getLongUrl();
    }

    /**
     * 查询短链信息
     */
    public Optional<ShortenResponse> getShortUrlInfo(String shortCode) {
        return shortUrlRepository.findByShortCodeAndStatus(shortCode, 1)
                .map(this::toResponse);
    }

    private ShortenResponse toResponse(ShortUrl entity) {
        ShortenResponse resp = new ShortenResponse();
        resp.setShortCode(entity.getShortCode());
        resp.setShortUrl(domain + "/" + entity.getShortCode());
        resp.setLongUrl(entity.getLongUrl());
        resp.setExpireTime(entity.getExpireTime());
        return resp;
    }
}
