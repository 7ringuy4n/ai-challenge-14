package com.papaya.claims.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.contract.*;
import com.papaya.claims.contract.Model.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
@Repository public final class MyBatisAuditRepository implements AuditRepository { private final AuditMapper mapper; public MyBatisAuditRepository(AuditMapper mapper){this.mapper=mapper;} public void append(AuditEntry a){mapper.append(new AuditRow(a.auditId(),a.claimId(),a.sequence(),a.timestamp(),a.fromState(),a.toState(),a.userId(),a.role(),a.notes(),a.configVersion(),a.traceId()));} public List<AuditEntry> findByClaimId(UUID id){return mapper.findByClaimId(id).stream().map(PersistenceJson::audit).toList();} }
