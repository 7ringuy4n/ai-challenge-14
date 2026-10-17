package com.papaya.claims.claim.service;

/** Safe application error with a stable code and message suitable for the HTTP boundary. */
public final class BusinessException extends RuntimeException {
    private final String code;
    private final int status;

    public BusinessException(String code, String message, int status) {
        super(message);
        if (code == null || code.isBlank() || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Business errors require a code and message");
        }
        if (status < 400 || status > 599) throw new IllegalArgumentException("HTTP status must be an error status");
        this.code = code;
        this.status = status;
    }

    public String code() { return code; }
    public int status() { return status; }
}
