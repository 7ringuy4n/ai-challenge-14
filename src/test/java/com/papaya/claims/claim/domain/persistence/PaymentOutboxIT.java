package com.papaya.claims.claim.domain.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.papaya.claims.claim.domain.OutboxRepository.Message;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentOutboxIT extends PersistenceITSupport {
    @Test
    void paymentRepositoryCreatesChecksAndConfirmsPayment() throws Exception {
        UUID claimId=rawClaim(); UUID request=UUID.randomUUID();
        try(var s=session()) {
            var payments=paymentRepository(s);
            payments.create(claimId,request,"USD",new BigDecimal("125.00"));
            assertThat(payments.exists(claimId)).isTrue();
            assertThat(payments.confirm(claimId,"PAY-123")).isTrue();
            var confirmation=payments.confirmation(claimId).orElseThrow();
            assertThat(confirmation.reference()).isEqualTo("PAY-123"); assertThat(confirmation.confirmedAt()).isNotNull();
            assertThat(payments.confirm(claimId,"PAY-456")).isFalse();
        }
    }

    @Test
    void outboxRepositoryUsesConfiguredLeaseAndSupportsRetryAndDelivery() throws Exception {
        UUID claimId=rawClaim(); UUID id=UUID.randomUUID(); Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        try(var s=session()) {
            var outbox=outboxRepository(s,5000);
            outbox.enqueue(new Message(id,claimId,"notify","{\"event\":\"ready\"}",0,now));
            var claimed=outbox.claimDue(now.plusMillis(1),10);
            assertThat(claimed).hasSize(1);
            assertThat(claimed.get(0).id()).isEqualTo(id);
            assertThat(claimed.get(0).attempts()).isEqualTo(1);
            assertThat(claimed.get(0).availableAt()).isAfter(now.plusMillis(4000));
            Instant retry=now.plusSeconds(10);
            assertThat(outbox.markFailed(id,"temporary delivery failure",retry)).isTrue();
            var retried=outbox.claimDue(retry,10);
            assertThat(retried).hasSize(1); assertThat(retried.get(0).attempts()).isEqualTo(2);
            assertThat(outbox.markDelivered(id,retry.plusSeconds(1))).isTrue();
            assertThat(outbox.claimDue(retry.plusSeconds(2),10)).isEmpty();
        }
    }
}
