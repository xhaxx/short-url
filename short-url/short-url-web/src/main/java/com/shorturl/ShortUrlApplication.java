package com.shorturl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 短链接服务 - Spring Boot 启动入口
 */
@SpringBootApplication
public class ShortUrlApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortUrlApplication.class, args);
    }
}
