package com.shorturl.common;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Subdivision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

/**
 * IP 地域解析工具（基于 MaxMind GeoIP2 离线库）
 *
 * 需要下载 GeoLite2-City.mmdb 文件放到指定路径。
 * 下载地址: https://dev.maxmind.com/geoip/geolite2-free-geolocation-data
 */
public class IpGeoUtils {

    private static final Logger log = LoggerFactory.getLogger(IpGeoUtils.class);
    private static volatile DatabaseReader reader;

    /**
     * 初始化数据库读取器（在应用启动时调用一次）
     *
     * @param dbPath GeoLite2-City.mmdb 文件路径
     */
    public static synchronized void init(String dbPath) {
        if (reader != null) {
            return;
        }
        try {
            File dbFile = new File(dbPath);
            if (dbFile.exists()) {
                reader = new DatabaseReader.Builder(dbFile).build();
                log.info("GeoIP2 database loaded from {}", dbPath);
            } else {
                log.warn("GeoIP2 database not found at {}, geo resolution disabled", dbPath);
                reader = null;
            }
        } catch (IOException e) {
            log.error("Failed to load GeoIP2 database from {}", dbPath, e);
            reader = null;
        }
    }

    /**
     * 解析 IP 地域信息
     *
     * @return [country, province, city] 三个元素，解析失败则对应位置为 null
     */
    public static String[] resolve(String ip) {
        if (reader == null) {
            return new String[]{null, null, null};
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);

            // 内网/回环地址直接返回
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()) {
                return new String[]{"内网", null, null};
            }

            CityResponse response = reader.city(addr);

            Country country = response.getCountry();
            Subdivision subdivision = response.getMostSpecificSubdivision();
            City city = response.getCity();

            return new String[]{
                    country != null ? country.getNames().getOrDefault("zh-CN", country.getName()) : null,
                    subdivision != null ? subdivision.getNames().getOrDefault("zh-CN", subdivision.getName()) : null,
                    city != null ? city.getNames().getOrDefault("zh-CN", city.getName()) : null
            };
        } catch (Exception e) {
            log.debug("Failed to resolve geo for IP: {}", ip, e);
            return new String[]{null, null, null};
        }
    }

    /**
     * 清理资源
     */
    public static synchronized void destroy() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                log.warn("Error closing GeoIP2 reader", e);
            }
            reader = null;
        }
    }
}
