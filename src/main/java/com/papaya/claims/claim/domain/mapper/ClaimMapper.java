package com.papaya.claims.claim.domain.mapper;

import com.papaya.claims.claim.domain.persistence.ClaimRow;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClaimMapper {
    int insert(ClaimRow row);
    ClaimRow find(@Param("id") UUID id);
    ClaimRow findForUpdate(@Param("id") UUID id);
    List<ClaimRow> pageAll(@Param("limit") int limit, @Param("offset") long offset);
    long countAll();
    List<ClaimRow> pageMember(@Param("memberId") String memberId, @Param("limit") int limit, @Param("offset") long offset);
    long countMember(@Param("memberId") String memberId);
    int compareAndSet(@Param("row") ClaimRow row, @Param("expectedVersion") long expectedVersion);
}
