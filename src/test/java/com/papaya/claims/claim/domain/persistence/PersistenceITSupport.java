package com.papaya.claims.claim.domain.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.claim.domain.mapper.AuditMapper;
import com.papaya.claims.claim.domain.mapper.ClaimMapper;
import com.papaya.claims.claim.domain.mapper.EffectsMapper;
import com.papaya.claims.claim.domain.mapper.ReceiptMapper;
import com.papaya.claims.claim.domain.mapper.SystemAuditMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
abstract class PersistenceITSupport {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("claims").withUsername("bootstrap").withPassword("bootstrap-test");
    static SqlSessionFactory sessions;
    static SqlSessionTemplate template;
    static DataSource runtimeSource;
    static final ObjectMapper JSON = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    @org.junit.jupiter.api.BeforeAll
    static void provisionRolesMigrateAndWireProductionMappers() throws Exception {
        try (Connection bootstrap = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement sql = bootstrap.createStatement()) {
            sql.execute("CREATE ROLE claims_runtime LOGIN PASSWORD 'runtime-test' NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT");
            sql.execute("CREATE ROLE claims_migration LOGIN PASSWORD 'migration-test' NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT");
            sql.execute("ALTER SCHEMA public OWNER TO claims_migration");
            sql.execute("GRANT CONNECT ON DATABASE claims TO claims_migration, claims_runtime");
            sql.execute("GRANT USAGE, CREATE ON SCHEMA public TO claims_migration");
        }
        try (Connection migration = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "claims_migration", "migration-test")) {
            var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(migration));
            try (Liquibase liquibase = new Liquibase("db/changelog/db.changelog-master.yml", new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
            try (Connection verification = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "claims_migration", "migration-test");
                 Statement check = verification.createStatement();
                 var result = check.executeQuery("SELECT current_user, nspname, pg_get_userbyid(nspowner) FROM pg_namespace WHERE nspname='public'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("claims_migration");
                assertThat(result.getString(3)).isEqualTo("claims_migration");
            }
        }
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        runtimeSource = createRuntimeDataSource();
        factory.setDataSource(runtimeSource);
        factory.setTypeHandlersPackage("com.papaya.claims.claim.domain.persistence");
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:sql/service/claim/*.xml"));
        sessions = factory.getObject();
        template = new SqlSessionTemplate(sessions);
    }

    static DataSource runtimeDataSource() {
        if (runtimeSource == null) runtimeSource = createRuntimeDataSource();
        return runtimeSource;
    }
    private static DataSource createRuntimeDataSource() {
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), "claims_runtime", "runtime-test");
        source.setDriverClassName("org.postgresql.Driver");
        return source;
    }
    static Connection runtime() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), "claims_runtime", "runtime-test");
    }
    static SqlSession session() { return sessions.openSession(true); }
    static ClaimMapper claims(SqlSession s) { return s.getMapper(ClaimMapper.class); }
    static AuditMapper audits(SqlSession s) { return s.getMapper(AuditMapper.class); }
    static ReceiptMapper receipts(SqlSession s) { return s.getMapper(ReceiptMapper.class); }
    static EffectsMapper effects(SqlSession s) { return s.getMapper(EffectsMapper.class); }
    static ClaimMapper claims() { return template.getMapper(ClaimMapper.class); }
    static AuditMapper audits() { return template.getMapper(AuditMapper.class); }
    static ReceiptMapper receipts() { return template.getMapper(ReceiptMapper.class); }
    static EffectsMapper effects() { return template.getMapper(EffectsMapper.class); }
    static SystemAuditMapper systemAudits() { return template.getMapper(SystemAuditMapper.class); }
    static MyBatisClaimRepository claimRepository(SqlSession s) { return new MyBatisClaimRepository(claims(s), JSON); }
    static MyBatisClaimAuditRepository auditRepository(SqlSession s) { return new MyBatisClaimAuditRepository(audits(s)); }
    static MyBatisIdempotencyRepository idempotencyRepository(SqlSession s) { return new MyBatisIdempotencyRepository(receipts(s), JSON); }
    static MyBatisPaymentRepository paymentRepository(SqlSession s) { return new MyBatisPaymentRepository(effects(s)); }
    static MyBatisOutboxRepository outboxRepository(SqlSession s, long leaseMillis) { return new MyBatisOutboxRepository(effects(s), leaseMillis); }
    static UUID rawClaim() throws SQLException {
        UUID id = UUID.randomUUID();
        try (var c = runtime(); var p = c.prepareStatement("INSERT INTO claims(id,member_id,state,policy_limit,currency) VALUES(?, 'member-1','submitted',125.00,'USD')")) {
            p.setObject(1,id); p.executeUpdate();
        }
        return id;
    }
}
