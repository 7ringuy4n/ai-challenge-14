package com.papaya.claims.claim.domain.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.claim.domain.IdempotencyRepository;
import com.papaya.claims.claim.domain.mapper.ReceiptMapper;
import java.lang.reflect.Proxy;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdempotencyRepositoryTest {
    @Test
    void mapsAbsentAndPresentReceiptsAndPreservesAllInsertFields() {
        Object[] selected = {null};
        Object[] inserted = {null};
        Object[] lockArgs = {null};
        ReceiptMapper mapper = (ReceiptMapper) Proxy.newProxyInstance(ReceiptMapper.class.getClassLoader(),
                new Class<?>[]{ReceiptMapper.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "find" -> selected[0];
                    case "insert" -> { inserted[0] = args[0]; yield 1; }
                    case "lock" -> { lockArgs[0] = args; yield null; }
                    default -> null;
                });
        var repository = new MyBatisIdempotencyRepository(mapper, new ObjectMapper());
        assertThat(repository.find("a", "scope", "key")).isEmpty();
        ReceiptRow row = new ReceiptRow(); row.id = UUID.randomUUID(); row.actorId = "actor"; row.scope = "scope";
        row.idempotencyKey = "key"; row.fingerprint = "abc"; row.resultJson = "{}"; selected[0] = row;
        assertThat(repository.find("actor", "scope", "key")).contains(new IdempotencyRepository.Receipt(
                row.id, row.actorId, row.scope, row.idempotencyKey, row.fingerprint, row.resultJson));
        repository.lock("actor", "scope", "key");
        assertThat(lockArgs[0]).isNotNull();
        repository.insert(new IdempotencyRepository.Receipt(row.id, row.actorId, row.scope,
                row.idempotencyKey, row.fingerprint, row.resultJson));
        assertThat(inserted[0]).isInstanceOf(ReceiptRow.class).satisfies(value -> {
            ReceiptRow mapped = (ReceiptRow) value;
            assertThat(mapped.id).isEqualTo(row.id);
            assertThat(mapped.actorId).isEqualTo(row.actorId);
            assertThat(mapped.scope).isEqualTo(row.scope);
            assertThat(mapped.idempotencyKey).isEqualTo(row.idempotencyKey);
            assertThat(mapped.fingerprint).isEqualTo(row.fingerprint);
            assertThat(mapped.resultJson).isEqualTo(row.resultJson);
        });
    }
}
