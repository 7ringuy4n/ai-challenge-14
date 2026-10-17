package com.papaya.claims.persistence;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditMapper {
    int append(AuditRow row);
    List<AuditRow> findByClaimId(UUID id);
}
