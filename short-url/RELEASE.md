# 发布到 GitHub 流程

## 1. 初始化本地 Git 仓库

```bash
cd short-url
git init
```

## 2. 添加 .gitignore

项目根目录已包含 Maven 构建产物、IDE 配置等忽略规则。确认 `target/`、`.idea/`、`*.iml` 等未被跟踪即可。

## 3. 提交代码

```bash
git add .
git commit -m "feat: 短链接服务 - 支持生成/重定向/统计，含前端 SPA"
```

## 4. 创建 GitHub 仓库

- 打开 https://github.com/new
- Repository name: `short-url`
- 选择 Public 或 Private
- **不要勾选** "Add a README file"（已有）
- 点击 "Create repository"

## 5. 推送代码

```bash
git remote add origin https://github.com/<your-username>/short-url.git
git branch -M main
git push -u origin main
```

## 6. （可选）创建 Release

**通过 GitHub Web：**

1. 进入仓库 → Releases → "Create a new release"
2. Tag: `v1.0.0`
3. Title: `v1.0.0 - 首个可用版本`
4. 描述：

```markdown
## 功能

- 长链转短链（Base62 编码 + 发号器）
- 302 重定向（布隆过滤器 + Redis 缓存）
- 访问统计（PV/UV、每日趋势、地域分布）
- 过期链接自动清理
- 前端 SPA（缩短/复制/统计图表）

## 部署

需要 JDK 17+、MySQL 8.0+、Redis 6+。

修改 application.yml 中的数据库/Redis 密码后：

mvn clean package -DskipTests
java -jar short-url-web/target/short-url-web-1.0.0-SNAPSHOT.jar
```

**通过 CLI：**

```bash
gh release create v1.0.0 \
  --title "v1.0.0 - 首个可用版本" \
  --notes "长链转短链、302 重定向、访问统计、前端 SPA"
```

## 7. 生产环境部署前检查

- [ ] 修改 `short-url.domain` 为实际公网域名（如 `https://s.yourdomain.com`）
- [ ] 修改数据库和 Redis 密码为强密码
- [ ] JPA `ddl-auto` 改为 `validate`
- [ ] `sql.init.mode` 改为 `never`
- [ ] 配置 `GEOIP2_DB_PATH` 启用地域解析
- [ ] 前端 `SHORTURL_API_BASE` 指向生产 API 地址
- [ ] 酌情调整日志级别为 `WARN`
