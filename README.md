# Backend

## OpenEdge JDBC 驱动

供应商驱动位于本机 `vendor/openedge.jar`，并被 Git 忽略。应用会在测试 OpenEdge 连接时直接加载该文件，不需要 Maven Profile 或命令行安装。若 IDEA 的工作目录不是 `backend/`，请在 Spring Boot 运行配置中添加 `OPENEDGE_DRIVER_PATH`，值为 `D:\\allcode\\UM-api-center\\backend\\vendor\\openedge.jar`。

不要提交 JAR、数据库密码或加密密钥。

## IDEA 启动配置

在 IDEA 中创建 Spring Boot 运行配置，主类选择 `com.um.apicenter.UmApiCenterApplication`，并在 Environment variables 填入 `PLATFORM_DB_URL`、`PLATFORM_DB_USERNAME`、`PLATFORM_DB_PASSWORD` 与 `DATASOURCE_ENCRYPTION_KEY`。可直接在 IDEA 运行 `com.um.apicenter.datasource.EncryptionKeyTool` 生成后一项的值。

OpenEdge 连接测试前，确认 `vendor/openedge.jar` 存在即可。

## 创建首个管理员账号

模块 1 完成后，在 PowerShell 中从本目录执行下列命令，交互式生成 BCrypt 密码哈希。命令不会在终端历史记录中保存密码：

```powershell
.\mvnw.cmd exec:java
```

将输出的哈希替换到数据库管理员提供的 `INSERT` 语句中。公网业务调用保持 HTTPS；本项目已确认内网管理后台 HTTP 例外，在服务器设置 `ADMIN_COOKIE_SECURE=false`。

## 部署与更新记录

读取 [实际部署与更新手册](docs/OPERATIONS.md)。当前服务器使用 9090，管理后台为 `http://192.168.10.185/um-api-center/`。

V0.1.2 更新前在开发库和生产库通过 Navicat 执行 `database/mysql/005_v0_1_2_call_log_diagnostics.sql`。诊断详情仅通过管理员调用日志接口提供，公网仍返回统一认证失败信息。
