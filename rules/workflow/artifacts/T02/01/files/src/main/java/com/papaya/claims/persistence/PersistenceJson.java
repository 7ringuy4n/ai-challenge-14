package com.papaya.claims.persistence;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import java.util.*;
final class PersistenceJson {
 private PersistenceJson() {}
 static String write(ObjectMapper mapper,Object value) { try { return mapper.writeValueAsString(value); } catch(JsonProcessingException e) { throw new IllegalStateException("Unable to encode persistence JSON",e); } }
 static Map<String,Object> readMap(ObjectMapper mapper,String value) { try { return mapper.readValue(value, new TypeReference<LinkedHashMap<String,Object>>(){}); } catch(Exception e) { throw new IllegalStateException("Unable to decode persistence JSON",e); } }
 static ClaimSnapshot claim(ObjectMapper m, ClaimRow r) { return new ClaimSnapshot(r.id(),r.memberId(),r.state(),r.policyLimit(),r.currency(),readMap(m,r.facts()),r.informationRequests(),r.version(),r.assessmentStartedAt(),r.lastInformationRequestedAt()); }
 static ClaimRow row(ObjectMapper m, ClaimSnapshot c) { return new ClaimRow(c.id(),c.memberId(),c.state(),c.policyLimit(),c.currency(),write(m,c.facts()),c.informationRequests(),c.version(),c.assessmentStartedAt(),c.lastInformationRequestedAt()); }
 static AuditEntry audit(AuditRow r) { return new AuditEntry(r.auditId(),r.claimId(),r.sequence(),r.timestamp(),r.fromState(),r.toState(),r.userId(),r.role(),r.notes(),r.configVersion(),r.traceId()); }
 static CommandReceipt receipt(ObjectMapper m,String json) { try { return m.readValue(json,CommandReceipt.class); } catch(Exception e) { throw new IllegalStateException("Unable to decode command receipt",e); } }
}
