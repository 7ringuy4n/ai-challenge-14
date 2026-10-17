package com.papaya.claims.claim.domain.mapper;

import com.papaya.claims.claim.domain.persistence.ReceiptRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReceiptMapper {
    void lock(@Param("actorId") String actorId, @Param("scope") String scope, @Param("key") String key);
    ReceiptRow find(@Param("actorId") String actorId, @Param("scope") String scope, @Param("key") String key);
    int insert(ReceiptRow row);
}
