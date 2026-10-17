package com.papaya.claims.claim.domain.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.claim.domain.*;
import java.util.Map;

final class PersistenceMapping {
    private static final TypeReference<Map<String,Object>> MAP = new TypeReference<>() {};
    private PersistenceMapping() {}
    static String json(ObjectMapper mapper, Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Could not encode persistence JSON", e); }
    }
    static Map<String,Object> map(ObjectMapper mapper, String value) {
        try { return mapper.readValue(value, MAP); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Could not decode persisted JSON", e); }
    }
    static ClaimAggregate aggregate(ObjectMapper mapper, ClaimRow r) {
        Claim c = new Claim(r.id, r.memberId, r.state, r.policyLimit, r.currency, r.version,
                new ClaimFacts(map(mapper, r.factsJson)), r.createdAt, r.updatedAt);
        return new ClaimAggregate(c, new ClaimWorkflowData(r.informationRequests, r.lastInformationRequestedAt,
                r.lastInformationReceivedAt, r.assessmentStartedAt, r.rejectedAt, r.memberAcknowledgedRejectionAt));
    }
    static ClaimRow row(ObjectMapper mapper, ClaimAggregate a) {
        Claim c = a.claim(); ClaimWorkflowData w = a.workflow(); ClaimRow r = new ClaimRow();
        r.id=c.id(); r.memberId=c.memberId(); r.state=c.state(); r.policyLimit=c.policyLimit(); r.currency=c.currency();
        r.version=c.version(); r.factsJson=json(mapper,c.facts().values()); r.informationRequests=w.informationRequests();
        r.lastInformationRequestedAt=w.lastInformationRequestedAt(); r.lastInformationReceivedAt=w.lastInformationReceivedAt();
        r.assessmentStartedAt=w.assessmentStartedAt(); r.rejectedAt=w.rejectedAt();
        r.memberAcknowledgedRejectionAt=w.memberAcknowledgedRejectionAt(); r.createdAt=c.createdAt(); r.updatedAt=c.updatedAt(); return r;
    }
}
