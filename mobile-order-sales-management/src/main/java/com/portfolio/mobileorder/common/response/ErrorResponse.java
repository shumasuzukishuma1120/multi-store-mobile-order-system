package com.portfolio.mobileorder.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {

    private final String error;

    private final String message;
}
