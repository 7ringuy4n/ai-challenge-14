package com.papaya.claims.persistence;
import org.apache.ibatis.annotations.*; import java.util.*;
@Mapper public interface AuditMapper { int append(AuditRow row); List<AuditRow> findByClaimId(UUID id); }
