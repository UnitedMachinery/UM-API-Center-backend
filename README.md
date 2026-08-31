# Backend

## OpenEdge JDBC 驱动

供应商驱动位于本机 `vendor/openedge.jar`，并被 Git 忽略。模块 2 开始前，在安装 Maven 后从本目录执行：

```powershell
mvn install:install-file -Dfile=vendor/openedge.jar -DgroupId=com.ddtek -DartifactId=openedge-jdbc -Dversion=legacy-local -Dpackaging=jar
```

启用 OpenEdge 依赖构建时附加 `-Dopenedge.jdbc.enabled=true`。例如：

```powershell
mvn -Dopenedge.jdbc.enabled=true spring-boot:run
```

不要提交 JAR、数据库密码或加密密钥。
