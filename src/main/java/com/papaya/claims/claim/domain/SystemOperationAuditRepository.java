package com.papaya.claims.claim.domain;

/** Append-only system-operation audit port. */
public interface SystemOperationAuditRepository { void append(SystemOperationAuditRecord record); }
