package com.um.apicenter.apidefinition;

import com.um.apicenter.datasource.DataSourcePasswordCipher;
import com.um.apicenter.datasource.JdbcConnectionTester;
import com.um.apicenter.datasource.ManagedDataSource;
import com.um.apicenter.datasource.ManagedDataSourceMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Component
public class DynamicQueryExecutor {

    private final ManagedDataSourceMapper dataSourceMapper;
    private final DataSourcePasswordCipher passwordCipher;
    private final JdbcConnectionTester connectionTester;
    private final NamedParameterSqlConverter sqlConverter;

    public DynamicQueryExecutor(ManagedDataSourceMapper dataSourceMapper, DataSourcePasswordCipher passwordCipher,
                                JdbcConnectionTester connectionTester, NamedParameterSqlConverter sqlConverter) {
        this.dataSourceMapper = dataSourceMapper;
        this.passwordCipher = passwordCipher;
        this.connectionTester = connectionTester;
        this.sqlConverter = sqlConverter;
    }

    public QueryResult execute(ApiDefinition definition, Map<String, Object> parameters) {
        ManagedDataSource dataSource = dataSourceMapper.findById(definition.getDatasourceId());
        if (dataSource == null || !dataSource.isEnabled()) {
            throw BusinessApiException.dataSourceUnavailable();
        }
        try {
            SqlSessionFactory factory = createSessionFactory(definition, dataSource);
            LimitedResultHandler resultHandler = new LimitedResultHandler(definition.getMaxRows());
            try (SqlSession session = factory.openSession()) {
                session.select(statementId(definition), parameters, resultHandler);
            }
            return new QueryResult(resultHandler.rows(), resultHandler.truncated());
        } catch (RuntimeException exception) {
            if (containsTimeout(exception)) {
                throw BusinessApiException.timeout();
            }
            throw exception;
        }
    }

    private SqlSessionFactory createSessionFactory(ApiDefinition definition, ManagedDataSource managedDataSource) {
        DataSource dataSource = new ManagedConnectionDataSource(managedDataSource,
                passwordCipher.decrypt(managedDataSource.getPasswordEncrypted()), connectionTester);
        String id = statementId(definition);
        Configuration configuration = new Configuration(new Environment(id, new JdbcTransactionFactory(), dataSource));
        SqlSource sqlSource = new XMLLanguageDriver().createSqlSource(configuration,
                sqlConverter.toMyBatisSql(definition.getSqlText()), Map.class);
        ResultMap resultMap = new ResultMap.Builder(configuration, id + "-result", Map.class, List.of()).build();
        MappedStatement statement = new MappedStatement.Builder(configuration, id, sqlSource, SqlCommandType.SELECT)
                .resultMaps(List.of(resultMap))
                .timeout(definition.getTimeoutSeconds())
                .useCache(false)
                .build();
        configuration.addMappedStatement(statement);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private boolean containsTimeout(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String statementId(ApiDefinition definition) {
        return "configuredApi." + definition.getId();
    }

    public record QueryResult(List<Map<String, Object>> rows, boolean truncated) {
    }

    private static class LimitedResultHandler implements ResultHandler<Map<String, Object>> {
        private final int maxRows;
        private final List<Map<String, Object>> rows = new ArrayList<>();
        private boolean truncated;

        private LimitedResultHandler(int maxRows) {
            this.maxRows = maxRows;
        }

        @Override
        public void handleResult(ResultContext<? extends Map<String, Object>> context) {
            if (rows.size() == maxRows) {
                truncated = true;
                context.stop();
                return;
            }
            rows.add(context.getResultObject());
        }

        private List<Map<String, Object>> rows() { return List.copyOf(rows); }
        private boolean truncated() { return truncated; }
    }

    private static class ManagedConnectionDataSource implements DataSource {
        private final ManagedDataSource dataSource;
        private final String password;
        private final JdbcConnectionTester connectionTester;

        private ManagedConnectionDataSource(ManagedDataSource dataSource, String password,
                                            JdbcConnectionTester connectionTester) {
            this.dataSource = dataSource;
            this.password = password;
            this.connectionTester = connectionTester;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return connectionTester.open(dataSource, password);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Unsupported"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() { return Logger.getGlobal(); }
    }
}
