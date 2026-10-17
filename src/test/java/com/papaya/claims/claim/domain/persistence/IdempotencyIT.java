package com.papaya.claims.claim.domain.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.papaya.claims.claim.domain.IdempotencyRepository.Receipt;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdempotencyIT extends PersistenceITSupport {
    @Test
    void repositoryRoundTripsReceiptAndPropagatesUniqueConflict() throws Exception {
        String fingerprint="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String result="{\"claimId\":\""+UUID.randomUUID()+"\",\"state\":\"approved\",\"version\":7}";
        Receipt receipt=new Receipt(UUID.randomUUID(),"actor-1","claim.transition","request-1",fingerprint,result);
        try(var s=session()) {
            var repo=idempotencyRepository(s); repo.insert(receipt);
            var loaded = repo.find("actor-1","claim.transition","request-1").orElseThrow();
            assertThat(loaded.id()).isEqualTo(receipt.id());
            assertThat(loaded.actorId()).isEqualTo(receipt.actorId());
            assertThat(loaded.scope()).isEqualTo(receipt.scope());
            assertThat(loaded.idempotencyKey()).isEqualTo(receipt.idempotencyKey());
            assertThat(loaded.fingerprint()).isEqualTo(receipt.fingerprint());
            assertThat(JSON.readTree(loaded.resultJson())).isEqualTo(JSON.readTree(receipt.resultJson()));
            assertThat(catchThrowable(()->repo.insert(new Receipt(UUID.randomUUID(),"actor-1","claim.transition","request-1","different-fingerprint","{}")))).isNotNull();
            assertThat(repo.find("actor-1","claim.transition","request-1")).contains(loaded);
        }
    }
}
