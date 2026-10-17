package com.papaya.claims.persistence;

import com.papaya.claims.contract.Model.*; import org.junit.jupiter.api.*; import org.testcontainers.containers.PostgreSQLContainer; import org.testcontainers.junit.jupiter.*;
import java.math.BigDecimal; import java.time.Instant; import java.util.*; import static org.assertj.core.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = false) class AuditImmutabilityIT {
 @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("claims").withUsername("postgres").withPassword("integration-test-password"); static PostgresPersistenceSupport.Harness h;
 @BeforeAll static void setup() throws Exception { h=PostgresPersistenceSupport.start(postgres); }
 @Test void runtimeCanInsertAndReadAuditButCannotUpdateOrDelete() throws Exception {
  var c=new ClaimSnapshot(UUID.randomUUID(),"member-001","RECEIVED",new BigDecimal("50.00"),"USD",Map.of(),0,0,null,null); h.claims().insert(c);
  var a=new AuditEntry(UUID.randomUUID(),c.id(),1,Instant.now(),null,"RECEIVED","clerk","document_clerk","created","v1","trace-1"); h.audits().append(a); assertThat(h.audits().findByClaimId(c.id())).containsExactly(a);
  try(var con=h.dataSource().getConnection();var st=con.createStatement()){assertThatThrownBy(()->st.executeUpdate("UPDATE claim_audit SET notes='tampered' WHERE audit_id='"+a.auditId()+"'")).hasMessageContaining("permission denied"); assertThatThrownBy(()->st.executeUpdate("DELETE FROM claim_audit WHERE audit_id='"+a.auditId()+"'")).hasMessageContaining("permission denied");}
 }
 @Test void auditCannotBeMutatedEvenByTableOwnerBecauseTriggerDefendsIt() throws Exception {
  var c=new ClaimSnapshot(UUID.randomUUID(),"member-001","RECEIVED",new BigDecimal("50.00"),"USD",Map.of(),0,0,null,null); h.claims().insert(c); var a=new AuditEntry(UUID.randomUUID(),c.id(),1,Instant.now(),null,"RECEIVED","clerk","document_clerk","created","v1","trace-2"); h.audits().append(a);
  try(var con=DriverManager.getConnection(postgres.getJdbcUrl(),"claims_owner","integration-test-password");var st=con.createStatement()){assertThatThrownBy(()->st.executeUpdate("UPDATE claim_audit SET notes='tampered' WHERE audit_id='"+a.auditId()+"'")).hasMessageContaining("claim audit is immutable");}
 }
}
