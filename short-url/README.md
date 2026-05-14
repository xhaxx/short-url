# short-url — 短链接服务

基于 Spring Boot 3.4.3 + JDK 17 的短链接服务，支持长链转短链、302 重定向、访问统计（PV/UV/地域）、过期自动清理。配套前端页面提供简约美观的 Web 交互界面。

## 快速启动

### 1. 环境准备

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Redis 6+
- （可选）GeoLite2-City.mmdb — 用于 IP 地域解析

### 2. 创建数据库

```sql
CREATE DATABASE short_url_db DEFAULT CHARACTER SET utf8mb4;
```

项目启动时会自动执行 `schema.sql` 建表，无需手动导入。

### 3. 修改配置

编辑 `short-url-web/src/main/resources/application.yml`，将数据库和 Redis 密码改为实际值：

```yaml
spring:
  datasource:
    password: your_mysql_password
  data:
    redis:
      password: your_redis_password
```

### 4. 编译启动

```bash
cd short-url
mvn clean package -DskipTests
java -jar short-url-web/target/short-url-web-1.0.0-SNAPSHOT.jar
```

也可以直接：

```bash
mvn spring-boot:run -pl short-url-web
```

### 5. 启动前端

浏览器直接打开 `short-url-frontend/index.html`，或使用任意静态服务器托管该目录：

```bash
# 例如用 Python 内置服务器
cd short-url-frontend
python -m http.server 5500
```

前端默认连接 `http://localhost:8080`，如需修改可通过全局变量 `window.SHORTURL_API_BASE` 指定后端地址。

---

## API 使用

### 创建短链接

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://www.example.com/very/long/path?param=value", "expireDays": 30}'
```

响应：

```json
{
  "shortCode": "1C",
  "shortUrl": "http://localhost:8080/1C",
  "longUrl": "https://www.example.com/very/long/path?param=value",
  "expireTime": "2026-06-12T10:30:00"
}
```

- `expireDays` 可选，不传默认 90 天
- 同一长链重复请求返回已有的短码，不会生成新的

### 访问短链接（302 重定向）

浏览器直接打开 `http://localhost:8080/1C`，或：

```bash
curl -v http://localhost:8080/1C
# HTTP/1.1 302
# Location: https://www.example.com/very/long/path?param=value
```

### 查询短链信息

```bash
curl http://localhost:8080/api/shorten/1C
```

### 查询访问统计

```bash
curl "http://localhost:8080/api/stats/1C?days=30"
```

响应：

```json
{
  "totalPv": 15230,
  "totalUv": 8921,
  "dailyStats": [
    { "date": "2026-05-13", "pv": 520, "uv": 301 },
    { "date": "2026-05-12", "pv": 480, "uv": 275 }
  ],
  "regionStats": [
    { "country": "中国", "province": "广东", "city": "深圳", "pv": 3500 },
    { "country": "中国", "province": "北京", "city": "北京", "pv": 2800 }
  ]
}
```

---

## 配置参考

`application.yml` 中可调整的关键项：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `short-url.domain` | `http://localhost:8080` | 短链接前缀（生产环境改为实际域名） |
| `short-url.default-expire-days` | `90` | 创建短链的默认过期天数 |
| `short-url.bloom-filter.expected-insertions` | `10000000` | 布隆过滤器预期元素数 |
| `short-url.bloom-filter.fpp` | `0.001` | 布隆过滤器误判率 |
| `short-url.access-log.batch-size` | `200` | 访问日志批量写入大小 |
| `id-generator.step` | `1000` | 发号器每次预取的号段大小 |

---

## 定时任务

| 任务 | 时间 | 说明 |
|------|------|------|
| 过期链接清理 | 每天 03:00 | 分批标记过期短链为删除，清除 Redis 缓存 |
| 访问日志聚合 | 每天 04:00 | 前一天的 access_log 聚合到 access_stats，删除 30 天前日志 |
| 布隆过滤器重建 | 每周日 02:00 | 从 DB 全量加载有效短码重建 BloomFilter |

---

## 项目结构

```
short-url/
├── short-url-common/       公共工具（Base62、MD5、GeoIP2）
├── short-url-idgen/        发号器（数据库号段模式）
├── short-url-service/      核心服务（Controller / Service / Repository / 定时任务 / CORS 配置）
├── short-url-web/          启动模块（Application + 配置 + schema.sql）
│
short-url-frontend/         前端 SPA（纯 HTML/CSS/JS，无构建步骤）
├── index.html              主入口 + 页面模板
├── css/style.css           完整样式
└── js/
    ├── api.js               API 服务层
    └── app.js               路由 / 渲染 / 交互逻辑
```
