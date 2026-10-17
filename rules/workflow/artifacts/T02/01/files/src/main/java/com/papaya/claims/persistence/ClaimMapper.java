package com.papaya.claims.persistence;
import org.apache.ibatis.annotations.*; import java.util.*;
@Mapper public interface ClaimMapper { int insert(ClaimRow row); ClaimRow find(UUID id); ClaimRow findForUpdate(UUID id); List<ClaimRow> list(); int compareAndSet(@Param("row") ClaimRow row,@Param("expectedVersion") long expectedVersion); }
