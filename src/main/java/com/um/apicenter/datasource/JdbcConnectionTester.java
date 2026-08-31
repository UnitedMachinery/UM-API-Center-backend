package com.um.apicenter.datasource;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@Component
public class JdbcConnectionTester {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcConnectionTester.class);
    private static final String OPENEDGE_DRIVER_CLASS = "com.ddtek.jdbc.openedge.OpenEdgeDriver";
    private volatile Driver openEdgeDriver;

    public ConnectionTestResult test(ManagedDataSource dataSource, String password) {
        try {
            try (Connection connection = openConnection(dataSource, password)) {
                return connection.isValid(5) ? ConnectionTestResult.succeeded() : ConnectionTestResult.failed();
            }
        } catch (ReflectiveOperationException | IOException exception) {
            LOGGER.warn("JDBC driver unavailable for datasource name={}, type={}",
                    dataSource.getName(), dataSource.getDbType(), exception);
            return ConnectionTestResult.driverUnavailable();
        } catch (SQLException exception) {
            LOGGER.warn("JDBC connection test failed for datasource name={}, type={}, host={}, port={}, database={}, sqlState={}, errorCode={}",
                    dataSource.getName(), dataSource.getDbType(), dataSource.getHost(), dataSource.getPort(),
                    dataSource.getDatabaseName(), exception.getSQLState(), exception.getErrorCode(), exception);
            return ConnectionTestResult.failed();
        }
    }

    private Connection openConnection(ManagedDataSource dataSource, String password)
            throws SQLException, ClassNotFoundException, ReflectiveOperationException, IOException {
        Properties properties = new Properties();
        properties.setProperty("user", dataSource.getUsername());
        properties.setProperty("password", password);

        if (dataSource.getDbType() == DataSourceType.OPENEDGE) {
            Connection connection = openEdgeDriver().connect(buildUrl(dataSource), properties);
            if (connection == null) {
                throw new SQLException("OpenEdge driver rejected the connection URL");
            }
            return connection;
        }

        Class.forName(driverClass(dataSource.getDbType()));
        return DriverManager.getConnection(buildUrl(dataSource), properties);
    }

    private Driver openEdgeDriver() throws ReflectiveOperationException, IOException {
        if (openEdgeDriver != null) {
            return openEdgeDriver;
        }
        synchronized (this) {
            if (openEdgeDriver == null) {
                Path jarPath = findOpenEdgeJar();
                URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, getClass().getClassLoader());
                Class<?> driverClass = Class.forName(OPENEDGE_DRIVER_CLASS, true, classLoader);
                openEdgeDriver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            }
            return openEdgeDriver;
        }
    }

    private Path findOpenEdgeJar() throws IOException {
        String configuredPath = System.getenv("OPENEDGE_DRIVER_PATH");
        Path[] candidates = configuredPath == null || configuredPath.isBlank()
                ? new Path[]{Path.of("vendor", "openedge.jar"), Path.of("backend", "vendor", "openedge.jar")}
                : new Path[]{Path.of(configuredPath)};
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath();
            }
        }
        throw new IOException("OpenEdge JDBC driver JAR is missing");
    }

    private String driverClass(DataSourceType type) {
        return switch (type) {
            case OPENEDGE -> OPENEDGE_DRIVER_CLASS;
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case SQLSERVER -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        };
    }

    private String buildUrl(ManagedDataSource dataSource) {
        return switch (dataSource.getDbType()) {
            case OPENEDGE -> "jdbc:datadirect:openedge://%s:%d;databaseName=%s".formatted(
                    dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName());
            case MYSQL -> "jdbc:mysql://%s:%d/%s".formatted(
                    dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName());
            case SQLSERVER -> "jdbc:sqlserver://%s:%d;databaseName=%s".formatted(
                    dataSource.getHost(), dataSource.getPort(), dataSource.getDatabaseName());
        };
    }

    public record ConnectionTestResult(boolean success, String message) {
        static ConnectionTestResult succeeded() {
            return new ConnectionTestResult(true, "连接成功");
        }

        static ConnectionTestResult failed() {
            return new ConnectionTestResult(false, "连接失败，请检查配置和网络");
        }

        static ConnectionTestResult driverUnavailable() {
            return new ConnectionTestResult(false, "该类型的 JDBC 驱动未安装");
        }
    }
}
