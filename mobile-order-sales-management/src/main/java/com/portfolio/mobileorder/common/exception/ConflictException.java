package com.portfolio.mobileorder.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 業務状態の競合が発生した場合に使用する業務例外
 */
public class ConflictException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }
}
