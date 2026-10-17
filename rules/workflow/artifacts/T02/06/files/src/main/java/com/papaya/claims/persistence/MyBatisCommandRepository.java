package com.papaya.claims.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.contract.CommandRepository;
import com.papaya.claims.contract.Model.CommandReceipt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisCommandRepository implements CommandRepository {
    private final CommandMapper mapper;
    private final ObjectMapper json;

    public MyBatisCommandRepository(CommandMapper mapper, ObjectMapper json) {
        this.mapper = mapper;
        this.json = json;
    }

    @Override
    public Optional<CommandReceipt> find(UUID claimId, String userId, String idempotencyKey) {
        ReceiptRow row = mapper.find(claimId, userId, idempotencyKey);
        return row == null ? Optional.empty() : Optional.of(PersistenceJson.receipt(json, row.result()));
    }

    @Override
    public void insert(UUID claimId, String userId, String idempotencyKey, CommandReceipt receipt) {
        mapper.insert(claimId, userId, idempotencyKey, receipt.requestHash(),
                PersistenceJson.write(json, receipt));
    }
}
