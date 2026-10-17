package com.papaya.claims.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.papaya.claims.contract.*;
import liquibase.*; import liquibase.database.*; import liquibase.database.jvm.JdbcConnection; import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.ibatis.session.*; import org.mybatis.spring.SqlSessionFactoryBean; import org.mybatis.spring.SqlSessionTemplate;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager; import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource; import java.sql.*; import java.util.*;

public final class PostgresPersistenceSupport {
 private PostgresPersistenceSupport() {}
 public static Harness start(org.testcontainers.containers.PostgreSQLContainer<?> pg) throws Exception {
  try(Connection admin=DriverManager.getConnection(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword())) {
   createRole(admin,"claims_owner",pg.getPassword()); createRole(admin,"claims_app",pg.getPassword());
   try(var grant=admin.createStatement()){grant.execute("GRANT CREATE ON SCHEMA public TO claims_owner");}
  }
  var owner=source(pg,"claims_owner",pg.getPassword());
  try(Connection c=owner.getConnection()) {
   var database=DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
   try(var liquibase=new Liquibase("db/db.changelog-master.yml",new ClassLoaderResourceAccessor(),database)) { liquibase.update(new Contexts(),new LabelExpression()); }
  }
  var runtime=source(pg,"claims_app",pg.getPassword());
  var factoryBean=new SqlSessionFactoryBean(); factoryBean.setDataSource(runtime);
  factoryBean.setMapperLocations(new org.springframework.core.io.support.PathMatchingResourcePatternResolver().getResources("classpath*:mappers/*.xml"));
  var factory=factoryBean.getObject();
  var template=new SqlSessionTemplate(factory);
  var json=JsonMapper.builder().findAndAddModules().build();
  var tx=new TransactionTemplate(new DataSourceTransactionManager(runtime));
  return new Harness(runtime,factory,json,new MyBatisClaimRepository(template.getMapper(ClaimMapper.class),json),new MyBatisAuditRepository(template.getMapper(AuditMapper.class)),new MyBatisCommandRepository(template.getMapper(CommandMapper.class),json),new MyBatisEffectRepository(template.getMapper(EffectMapper.class),json),tx);
 }
 private static void createRole(Connection c,String role,String password) throws SQLException { try(var s=c.prepareStatement("SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', ?, ?)");){s.setString(1,role);s.setString(2,password);try(var r=s.executeQuery()){r.next();try(var ddl=c.createStatement()){ddl.execute(r.getString(1));}}} }
 private static DataSource source(org.testcontainers.containers.PostgreSQLContainer<?> pg,String user,String pass){var ds=new PGSimpleDataSource();ds.setURL(pg.getJdbcUrl());ds.setUser(user);ds.setPassword(pass);return ds;}
 public record Harness(DataSource dataSource,SqlSessionFactory factory,ObjectMapper json,MyBatisClaimRepository claims,MyBatisAuditRepository audits,MyBatisCommandRepository commands,MyBatisEffectRepository effects,TransactionTemplate transactions) {}
}
