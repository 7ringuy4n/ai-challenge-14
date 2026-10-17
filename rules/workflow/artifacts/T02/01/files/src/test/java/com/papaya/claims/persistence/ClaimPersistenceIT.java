package com.papaya.claims.persistence;

import com.papaya.claims.contract.Model.*; import org.junit.jupiter.api.*; import org.testcontainers.containers.PostgreSQLContainer; import org.testcontainers.junit.jupiter.*;
import java.math.BigDecimal; import java.time.Instant; import java.util.*; import static org.assertj.core.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = false) class ClaimPersistenceIT {
 @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("claims").withUsername("postgres").withPassword("integration-test-password");
 static PostgresPersistenceSupport.Harness h;
 @BeforeAll static void setup() throws Exception { h=PostgresPersistenceSupport.start(postgres); }
 private ClaimSnapshot claim(UUID id){return new ClaimSnapshot(id,"member-001","RECEIVED",new BigDecimal("100.00"),"USD",Map.of("proof",true),0,0,null,null);}
 @Test void persistsJsonbAndReadsClaim() { var c=claim(UUID.randomUUID()); h.claims().insert(c); assertThat(h.claims().find(c.id())).contains(c); }
 @Test void locksClaimInsideTransactionAndCasChecksVersion() { var c=claim(UUID.randomUUID()); h.claims().insert(c); var updated=c.updateFacts(Map.of("proof",false)); assertThat(h.transactions().execute(status->{assertThat(h.claims().findForUpdate(c.id())).contains(c);return h.claims().compareAndSet(updated,0);})).isTrue(); assertThat(h.claims().compareAndSet(c,0)).isFalse(); assertThat(h.claims().find(c.id()).orElseThrow().facts()).containsEntry("proof",false); }
 @Test void transactionRollbackRemovesClaim() { var c=claim(UUID.randomUUID()); assertThatThrownBy(()->h.transactions().executeWithoutResult(status->{h.claims().insert(c);throw new IllegalStateException("rollback");})).isInstanceOf(IllegalStateException.class); assertThat(h.claims().find(c.id())).isEmpty(); }
 @Test void receiptUniqueByClaimUserAndIdempotencyKeyAndStoresSnapshot() { var c=claim(UUID.randomUUID()); h.claims().insert(c); var receipt=new CommandReceipt("sha256",c); h.commands().insert(c.id(),"clerk","key-1",receipt); assertThat(h.commands().find(c.id(),"clerk","key-1")).contains(receipt); assertThatThrownBy(()->h.commands().insert(c.id(),"clerk","key-1",receipt)).hasMessageContaining("command_receipts"); h.commands().insert(c.id(),"lead","key-1",receipt); }
 @Test void paymentRequestIsUniquePerClaimAndOutboxRetries() { var c=claim(UUID.randomUUID()); h.claims().insert(c); h.effects().createPaymentRequest(c.id(),UUID.randomUUID()); h.effects().createPaymentRequest(c.id(),UUID.randomUUID()); assertThat(h.effects().paymentRequestExists(c.id())).isTrue(); var id=UUID.randomUUID(); h.effects().enqueue(List.of(new OutboxMessage(id,c.id(),"notify_approval","{}",0))); assertThat(h.effects().pending()).extracting(OutboxMessage::id).contains(id); h.effects().markFailed(id,"network",Instant.now().plusSeconds(30)); assertThat(h.effects().pending()).extracting(OutboxMessage::id).doesNotContain(id); }
}
