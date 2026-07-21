package com.portfolio.mobileorder.common.exception;

import com.portfolio.mobileorder.common.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 共通の例外ハンドリングを行うクラス。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    private static final String VALIDATION_ERROR_MESSAGE = "入力値が不正です。";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "予期しないエラーが発生しました。";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn(
                "Business exception occurred. status={}, errorCode={}",
                ex.getStatus().value(),
                ex.getErrorCode()
        );

        ErrorResponse response = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.warn("Validation exception occurred. type={}", ex.getClass().getSimpleName());

        ErrorResponse response = new ErrorResponse(VALIDATION_ERROR, VALIDATION_ERROR_MESSAGE);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Validation exception occurred. type={}", ex.getClass().getSimpleName());

        ErrorResponse response = new ErrorResponse(VALIDATION_ERROR, VALIDATION_ERROR_MESSAGE);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unexpected exception occurred. type={}", ex.getClass().getName(), ex);

        ErrorResponse response = new ErrorResponse(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_MESSAGE);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex
    ) {
        log.warn("Request body is invalid JSON. type={}", ex.getClass().getName());

        ErrorResponse response = new ErrorResponse(
                "VALIDATION_ERROR",
                "リクエストボディの形式が不正です。"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
