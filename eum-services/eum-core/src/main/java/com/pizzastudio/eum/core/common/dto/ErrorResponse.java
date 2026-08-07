package com.pizzastudio.eum.core.common.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 오류 응답 본문.
 */
@Getter
@NoArgsConstructor
public class ErrorResponse {

    private String code;
    private String message;
    private List<FieldErrorDto> errors = new ArrayList<>();

    @Builder
    public ErrorResponse(String code, String message, List<FieldErrorDto> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors == null ? new ArrayList<>() : errors;
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return ErrorResponse.builder()
            .code(errorCode.getCode())
            .message(message)
            .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        List<FieldErrorDto> fieldErrors = new ArrayList<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            fieldErrors.add(new FieldErrorDto(
                error.getField(),
                error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                error.getDefaultMessage()));
        }
        return ErrorResponse.builder()
            .code(errorCode.getCode())
            .message(errorCode.getMessage())
            .errors(fieldErrors)
            .build();
    }
}
