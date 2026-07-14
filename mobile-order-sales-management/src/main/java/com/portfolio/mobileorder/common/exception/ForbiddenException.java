package com.portfolio.mobileorder.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 権限が不足している場合に使用する業務例外
 */
public class ForbiddenException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String errorCode, String message) {
        super(HttpStatus.FORBIDDEN, errorCode, message);
    }
}
