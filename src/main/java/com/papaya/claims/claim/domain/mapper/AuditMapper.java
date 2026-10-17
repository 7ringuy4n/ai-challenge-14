package com.papaya.claims.claim.domain.mapper;

import com.papaya.claims.claim.domain.persistence.AuditRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditMapper {
    int append(AuditRow row);
    List<AuditRow> page(@Param("claimId") UUID claimId, @Param("limit") int limit, @Param("offset") long offset);
    long count(@Param("claimId") UUID claimId);
}
