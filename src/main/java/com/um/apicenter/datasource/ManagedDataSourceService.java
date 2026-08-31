package com.um.apicenter.datasource;

import com.um.apicenter.admin.AdminPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ManagedDataSourceService {

    private final ManagedDataSourceMapper dataSourceMapper;
    private final DataSourcePasswordCipher passwordCipher;
    private final JdbcConnectionTester connectionTester;

    public ManagedDataSourceService(ManagedDataSourceMapper dataSourceMapper, DataSourcePasswordCipher passwordCipher,
                                    JdbcConnectionTester connectionTester) {
        this.dataSourceMapper = dataSourceMapper;
        this.passwordCipher = passwordCipher;
        this.connectionTester = connectionTester;
    }

    public List<ManagedDataSource> list() {
        return dataSourceMapper.findAll();
    }

    @Transactional
    public ManagedDataSource create(DataSourceInput input, AdminPrincipal administrator) {
        ManagedDataSource dataSource = fromInput(input, administrator.username(), passwordCipher.encrypt(input.password()));
        dataSourceMapper.insert(dataSource);
        return require(dataSource.getId());
    }

    @Transactional
    public ManagedDataSource update(long id, DataSourceInput input, AdminPrincipal administrator) {
        ManagedDataSource existing = require(id);
        String encryptedPassword = input.password() == null || input.password().isBlank()
                ? existing.getPasswordEncrypted() : passwordCipher.encrypt(input.password());
        ManagedDataSource updated = fromInput(input, administrator.username(), encryptedPassword);
        updated.setId(id);
        if (dataSourceMapper.update(updated) == 0) {
            throw new DataSourceNotFoundException();
        }
        return require(id);
    }

    @Transactional
    public ManagedDataSource setEnabled(long id, boolean enabled, AdminPrincipal administrator) {
        if (dataSourceMapper.updateEnabled(id, enabled, administrator.username()) == 0) {
            throw new DataSourceNotFoundException();
        }
        return require(id);
    }

    public JdbcConnectionTester.ConnectionTestResult testConnection(long id) {
        ManagedDataSource dataSource = require(id);
        return connectionTester.test(dataSource, passwordCipher.decrypt(dataSource.getPasswordEncrypted()));
    }

    private ManagedDataSource require(long id) {
        ManagedDataSource dataSource = dataSourceMapper.findById(id);
        if (dataSource == null) {
            throw new DataSourceNotFoundException();
        }
        return dataSource;
    }

    private ManagedDataSource fromInput(DataSourceInput input, String administrator, String encryptedPassword) {
        ManagedDataSource dataSource = new ManagedDataSource();
        dataSource.setName(input.name());
        dataSource.setDbType(input.dbType());
        dataSource.setHost(input.host());
        dataSource.setPort(input.port());
        dataSource.setDatabaseName(input.databaseName());
        dataSource.setUsername(input.username());
        dataSource.setPasswordEncrypted(encryptedPassword);
        dataSource.setConnectionOptions(null);
        dataSource.setEnabled(input.enabled());
        dataSource.setRemark(blankToNull(input.remark()));
        dataSource.setCreatedBy(administrator);
        dataSource.setUpdatedBy(administrator);
        return dataSource;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record DataSourceInput(String name, DataSourceType dbType, String host, int port, String databaseName,
                                  String username, String password, boolean enabled, String remark) {
    }
}
