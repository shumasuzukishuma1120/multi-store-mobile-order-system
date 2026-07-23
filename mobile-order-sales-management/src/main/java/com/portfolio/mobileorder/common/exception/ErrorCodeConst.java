package com.portfolio.mobileorder.common.exception;

/**
 * 業務エラーコードを定義するクラス。
 */
public final class ErrorCodeConst {

    private ErrorCodeConst() {
    }

    public static final String TABLE_NOT_FOUND = "TABLE_NOT_FOUND";
    public static final String TABLE_NOT_OCCUPIED = "TABLE_NOT_OCCUPIED";
    public static final String VISIT_SESSION_ACCESS_DENIED = "VISIT_SESSION_ACCESS_DENIED";
    public static final String VISIT_SESSION_NOT_ACTIVE = "VISIT_SESSION_NOT_ACTIVE";
    public static final String VISIT_SESSION_EXPIRED = "VISIT_SESSION_EXPIRED";
    public static final String STORE_ACCESS_DENIED = "STORE_ACCESS_DENIED";
    public static final String TABLE_NOT_AVAILABLE = "TABLE_NOT_AVAILABLE";
    public static final String ACTIVE_VISIT_SESSION_EXISTS = "ACTIVE_VISIT_SESSION_EXISTS";
    public static final String OPTIMISTIC_LOCK_CONFLICT = "OPTIMISTIC_LOCK_CONFLICT";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

}