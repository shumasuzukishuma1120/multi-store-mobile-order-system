package com.portfolio.mobileorder.common.exception;

import org.springframework.http.HttpStatus;

/**
 * リソースが存在しない場合に使用される業務例外。
 */
public class NotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String errorCode, String message) {
        super(HttpStatus.NOT_FOUND, errorCode, message);
    }
}
