package com.papaya.claims.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.contract.Model.AuditEntry;
import com.papaya.claims.contract.Model.ClaimSnapshot;
import com.papaya.claims.contract.Model.CommandReceipt;
import java.util.LinkedHashMap;
import java.util.Map;

final class PersistenceJson {
    private static final TypeReference<LinkedHashMap<String, Object>> FACT_MAP =
            new TypeReference<>() {};

    private PersistenceJson() {}

    static String write(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Unable to encode persistence JSON", error);
        }
    }

    static Map<String, Object> readMap(ObjectMapper mapper, String value) {
        try {
            return mapper.readValue(value, FACT_MAP);
        } catch (Exception error) {
            throw new IllegalStateException("Unable to decode persistence JSON", error);
        }
    }

    static ClaimSnapshot claim(ObjectMapper mapper, ClaimRow row) {
        return new ClaimSnapshot(
                row.id(), row.memberId(), row.state(), row.policyLimit(), row.currency(),
                readMap(mapper, row.facts()), row.informationRequests(), row.version(),
                row.assessmentStartedAt(), row.lastInformationRequestedAt());
    }

    static ClaimRow row(ObjectMapper mapper, ClaimSnapshot claim) {
        return new ClaimRow(
                claim.id(), claim.memberId(), claim.state(), claim.policyLimit(), claim.currency(),
                write(mapper, claim.facts()), claim.informationRequests(), claim.version(),
                claim.assessmentStartedAt(), claim.lastInformationRequestedAt());
    }

    static AuditEntry audit(AuditRow row) {
        return new AuditEntry(
                row.auditId(), row.claimId(), row.sequence(), row.timestamp(), row.fromState(),
                row.toState(), row.userId(), row.role(), row.notes(), row.configVersion(),
                row.traceId());
    }

    static CommandReceipt receipt(ObjectMapper mapper, String json) {
        try {
            return mapper.readValue(json, CommandReceipt.class);
        } catch (Exception error) {
            throw new IllegalStateException("Unable to decode command receipt", error);
        }
    }
}
