package com.papaya.claims.persistence;
import org.apache.ibatis.annotations.*; import java.util.*;
@Mapper public interface CommandMapper { ReceiptRow find(@Param("claimId") UUID claimId,@Param("userId") String userId,@Param("key") String key); int insert(@Param("claimId") UUID claimId,@Param("userId") String userId,@Param("key") String key,@Param("requestHash") String requestHash,@Param("result") String result); }
