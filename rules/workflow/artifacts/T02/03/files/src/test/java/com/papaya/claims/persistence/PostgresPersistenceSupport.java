package com.papaya.claims.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.database.DatabaseFactory;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

public final class PostgresPersistenceSupport {
    private PostgresPersistenceSupport() {}

    public static Harness start(PostgreSQLContainer<?> postgres) throws Exception {
        try (Connection admin = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            createRole(admin, "claims_owner", postgres.getPassword());
            createRole(admin, "claims_app", postgres.getPassword());
            try (var statement = admin.createStatement()) {
                statement.execute("GRANT CREATE ON SCHEMA public TO claims_owner");
            }
        }

        DataSource migrationDataSource = source(postgres, "claims_owner", postgres.getPassword());
        try (Connection connection = migrationDataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase(
                    "db/db.changelog-master.yml", new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }

        DataSource runtimeDataSource = source(postgres, "claims_app", postgres.getPassword());
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(runtimeDataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mappers/*.xml"));
        SqlSessionFactory factory = factoryBean.getObject();
        SqlSessionTemplate template = new SqlSessionTemplate(factory);
        ObjectMapper json = JsonMapper.builder().findAndAddModules().build();
        TransactionTemplate transactions = new TransactionTemplate(
                new DataSourceTransactionManager(runtimeDataSource));

        return new Harness(runtimeDataSource, factory, json,
                new MyBatisClaimRepository(template.getMapper(ClaimMapper.class), json),
                new MyBatisAuditRepository(template.getMapper(AuditMapper.class)),
                new MyBatisCommandRepository(template.getMapper(CommandMapper.class), json),
                new MyBatisEffectRepository(template.getMapper(EffectMapper.class)), transactions);
    }

    private static void createRole(Connection connection, String role, String password)
            throws SQLException {
        String createRoleSql = "SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', ?, ?)";
        try (var statement = connection.prepareStatement(createRoleSql)) {
            statement.setString(1, role);
            statement.setString(2, password);
            try (var result = statement.executeQuery()) {
                result.next();
                try (var ddl = connection.createStatement()) {
                    ddl.execute(result.getString(1));
                }
            }
        }
    }

    private static DataSource source(PostgreSQLContainer<?> postgres, String user, String password) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(postgres.getJdbcUrl());
        dataSource.setUser(user);
        dataSource.setPassword(password);
        return dataSource;
    }

    public record Harness(
            DataSource dataSource,
            SqlSessionFactory factory,
            ObjectMapper json,
            MyBatisClaimRepository claims,
            MyBatisAuditRepository audits,
            MyBatisCommandRepository commands,
            MyBatisEffectRepository effects,
            TransactionTemplate transactions) {}
}
