package com.papaya.claims.persistence;

import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClaimMapper {
    int insert(ClaimRow row);
    ClaimRow find(UUID id);
    ClaimRow findForUpdate(UUID id);
    List<ClaimRow> list();
    int compareAndSet(@Param("row") ClaimRow row, @Param("expectedVersion") long expectedVersion);
}
