# UM API Center 服务器与部署、更新记录

整理日期：2026-10-05。V0.1.2 已由用户更新到服务器并启动，业务与日志验收待确认；V0.1.1 保留为回退版本。真实密码、密钥、Cookie 和密码哈希不写入本文件。

最新更新：2026-10-05 09:35:30（Asia/Shanghai）重启应用；管理页面 200、空登录 400。回退目录 `/opt/um-api-center/releases/v0.1.1-20261005-093530`。

## 服务器事实

| 项目 | 实际值 |
|---|---|
| Linux | 192.168.10.185 / PLM / Debian 13.3（trixie） |
| 检查的运行时 | OpenJDK 25.0.1、Nginx 1.26.3、systemd 257（2026-09-08） |
| 后端 | Java 17 编译目标 / Spring Boot 4 / MyBatis |
| 服务和运行身份 | systemd um-api-center；umapi:umapi |
| 后端监听 | 127.0.0.1:9090；8080 被其他应用占用 |
| 平台库 | MySQL 8，um_api_center，已建五张表和生产管理员 |
| 管理后台 | http://192.168.10.185/um-api-center/ |
| 业务接口 | https://www.united-machining.com/api/... |
| 域名服务器 | Windows Server 2012、IIS、URL Rewrite、ARR；日志观察转发来源 192.168.10.148 |
| IIS 站点 | umwebcore证书；HTTP 12348、HTTPS 12360，主机名为空 |

公网 443 到 IIS 的外层映射未完整核实。Windows 已有 Nginx，不修改它的配置。新增接口只需管理后台配置并启用，不需要修改代理或部署。
旧版 OpenEdge 驱动此前在 Java 17 验证，服务器 Java 25 应在每次更新后验证连接与查询；如需 Java 17，只修改本应用 ExecStart 的 Java 路径。

## 文件、权限、环境

```text
/opt/um-api-center/backend/um-api-center.jar
/opt/um-api-center/backend/vendor/openedge.jar
/opt/um-api-center/frontend/index.html
/opt/um-api-center/frontend/assets/
/etc/um-api-center/um-api-center.env
/etc/systemd/system/um-api-center.service
/etc/nginx/sites-available/united-machining
/etc/nginx/sites-enabled/united-machining
/etc/nginx/sites-available/um-api-center
```

应用顶层 751，后端目录 750、JAR 640、属 umapi:umapi；前端目录 755、文件 644。
环境目录 root:umapi、750；环境文件 root:umapi、640。systemd 使用 EnvironmentFile，不会自动加载项目 .env。

```ini
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=9090
PLATFORM_DB_URL=jdbc:mysql://<MySQL地址>:3306/um_api_center?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
PLATFORM_DB_USERNAME=<账号>
PLATFORM_DB_PASSWORD=<密码>
DATASOURCE_ENCRYPTION_KEY=<已有生产密钥>
OPENEDGE_DRIVER_PATH=/opt/um-api-center/backend/vendor/openedge.jar
ADMIN_COOKIE_SECURE=false
```

生产密钥是 Base64 编码 32 字节值，首次部署时独立生成；更新时不重新生成。开发库数据源密文不能配生产新密钥。
用户明确允许 HTTP 内网管理后台：Cookie 非 Secure，仍为 HttpOnly、SameSite=Lax、路径 /api/admin、8 小时；公网业务接口使用 HTTPS。

systemd 的 User/Group 是 umapi，WorkingDirectory 是 /opt/um-api-center/backend，启动命令：

```text
/usr/bin/java -jar /opt/um-api-center/backend/um-api-center.jar --spring.profiles.active=prod
```

服务 Restart=on-failure、RestartSec=10、SuccessExitStatus=143、UMask=0027，启用开机运行。

## IIS 与 Linux 路由

IIS 站点原有规则末尾新增 `UM API Center API`：正则 `^api/(.*)$`，重写 `http://192.168.10.185/api/{R:1}`，追加查询字符串、停止后续规则。保留 CRTT、qc-inspect 等原规则及官网首页。

ARR 当前把 Host 改为 192.168.10.185，因此实际使用的 Linux IP 站点 united-machining 内必须包含以下区块。不能覆盖整个站点文件，也不全局调整 ARR 的 Host 行为：

```nginx
location ^~ /api/ {
    proxy_pass http://127.0.0.1:9090;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Proto https;
    proxy_connect_timeout 5s;
    proxy_read_timeout 25s;
    proxy_send_timeout 25s;
}
location = /um-api-center {
    return 301 /um-api-center/;
}
location ^~ /um-api-center/ {
    alias /opt/um-api-center/frontend/;
    try_files $uri $uri/ /um-api-center/index.html;
}
```

独立域名站点 um-api-center 也已创建，但仅修改该站点不能覆盖 IIS 的 IP Host 请求。
前端生产基础路径为 /um-api-center/，路由使用 import.meta.env.BASE_URL，Axios 保持同源 /api。
当前代理硬编码 X-Forwarded-Proto https，会把内部 HTTP 也声明为 HTTPS；不能当成内部传输已加密的证明。
后端 9090 不对外监听；防火墙限制的实际执行情况尚未在对话核实。

## 更新：本机 PowerShell

数据库增量脚本由用户先在 Navicat 的开发、生产 um_api_center 库执行，再升级后端。
V0.1.2 对应 database/mysql/005_v0_1_2_call_log_diagnostics.sql。001–004 不重复执行，不删除旧日志。
IDEA 停止本地后端，避免 target 文件锁，然后打包：

```powershell
Set-Location D:\allcode\UM-api-center\backend
.\mvnw.cmd package
# 必须 BUILD SUCCESS，包含测试通过。
Set-Location D:\allcode\UM-api-center\frontend
pnpm build
# Vite 临时配置目录无法写入时可用：
# pnpm run build -- --configLoader runner
Set-Location D:\allcode\UM-api-center
tar -czf frontend-release.tar.gz -C frontend/dist .
scp .\backend\target\um-api-center-backend-0.0.1-SNAPSHOT.jar root@192.168.10.185:/tmp/um-api-center.jar.new
scp .\frontend-release.tar.gz root@192.168.10.185:/tmp/frontend-release.tar.gz
ssh root@192.168.10.185
```

frontend/.env.production 保留 VITE_APP_BASE=/um-api-center/；该文件只保存非敏感部署路径，可入前端 Git。
若用隔离输出目录构建，上传必须使用该目录的新产物，不上传旧 target/dist。不能强行删除用户使用中的目录。
Maven 带点号的 -D 参数在 PowerShell 中加完整引号，例如 "-Dexec.mainClass=..."。
OpenEdge 驱动已归位、Git 忽略，日常更新不重新上传或换驱动；不上传本机 .env。

## 更新：Linux Bash / root

先检查上传包、备份，再停止本应用替换 JAR，避免 JVM 读取被覆盖的运行归档：

```bash
ls -lh /tmp/um-api-center.jar.new /tmp/frontend-release.tar.gz
tar -tzf /tmp/frontend-release.tar.gz | head -20
release_backup="/opt/um-api-center/releases/$(date +%Y%m%d-%H%M%S)"
install -d -m 750 "$release_backup"
cp -p /opt/um-api-center/backend/um-api-center.jar "$release_backup/um-api-center.jar"
tar -czf "$release_backup/frontend.tar.gz" -C /opt/um-api-center/frontend .
printf '本次回退目录：%s\n' "$release_backup"
systemctl stop um-api-center
install -o umapi -g umapi -m 640 /tmp/um-api-center.jar.new /opt/um-api-center/backend/um-api-center.jar
tar -xzf /tmp/frontend-release.tar.gz -C /opt/um-api-center/frontend
chown -R umapi:umapi /opt/um-api-center/frontend
chmod 751 /opt/um-api-center
find /opt/um-api-center/frontend -type d -exec chmod 755 {} \;
find /opt/um-api-center/frontend -type f -exec chmod 644 {} \;
systemctl start um-api-center
systemctl status um-api-center --no-pager -l
ss -ltnp 'sport = :9090'
```

等待 Tomcat started 和 Started UmApiCenterApplication；status 的瞬时 active 不代替启动完成检查。
这里覆盖 index.html 并保留旧 assets，便于现有页面刷新，不在更新时递归清空目录。
只更新产物无需 Nginx reload；改了规则才先 nginx -t，再 systemctl reload nginx。
环境文件改动需要重启应用；只有 systemd 单元变更需要 daemon-reload。

本机无密码检查：

```bash
curl -sS -o /dev/null -w '管理后台: HTTP %{http_code}\n' -H 'Host: 192.168.10.185' http://127.0.0.1/um-api-center/
curl -sS -o /dev/null -w '登录路由: HTTP %{http_code}\n' -H 'Host: 192.168.10.185' -H 'Content-Type: application/json' -X POST --data '{}' http://127.0.0.1/api/admin/login
```

预期 200、400。然后浏览器验证登录、刷新、退出及 OpenEdge/MySQL 连接；调用工具经公网 HTTPS 测试正确/错误密码、停用、参数不合法，并核对日志。
400 只证明代理到达后端，不能替代实际查询验收。Windows Server 2012 PowerShell 可在当前会话启用 TLS 1.2：

```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
```

## 排错和回退

服务日志 journalctl -u um-api-center -n 80 --no-pager；代理问题检查 Nginx access/error 日志。输出分享前去除凭据。
405 通常检查 Host 和 IP 站点 /api/；前端 500 检查目录穿越/读取权限；登录掉线检查 Cookie Secure 配置已被新 JAR 读取；数据源解密失败检查已有密钥未变。

需要回退时在同一个 shell 使用上面的 release_backup。若换 shell，先赋值为已确认存在的具体备份目录，确认文件后执行：

```bash
ls -lh "$release_backup/um-api-center.jar" "$release_backup/frontend.tar.gz"
systemctl stop um-api-center
install -o umapi -g umapi -m 640 "$release_backup/um-api-center.jar" /opt/um-api-center/backend/um-api-center.jar
tar -xzf "$release_backup/frontend.tar.gz" -C /opt/um-api-center/frontend
chown -R umapi:umapi /opt/um-api-center/frontend
find /opt/um-api-center/frontend -type d -exec chmod 755 {} \;
find /opt/um-api-center/frontend -type f -exec chmod 644 {} \;
systemctl start um-api-center
systemctl status um-api-center --no-pager -l
```

005 的新增字段可空，回退旧后端时保留字段和数据，不执行 DROP。
