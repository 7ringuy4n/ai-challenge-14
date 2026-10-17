package com.papaya.claims.claim.domain.mapper;

import com.papaya.claims.claim.domain.persistence.SystemAuditRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemAuditMapper { int append(SystemAuditRow row); }
