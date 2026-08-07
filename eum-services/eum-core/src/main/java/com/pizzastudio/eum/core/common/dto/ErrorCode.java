package com.pizzastudio.eum.core.common.dto;

import lombok.Getter;

/**
 * 응답에 실어 보내는 오류 구분.
 */
@Getter
public enum ErrorCode {

    INVALID_INPUT_VALUE("C001", "입력값이 올바르지 않습니다."),
    ENTITY_NOT_FOUND("C002", "요청한 자료를 찾을 수 없습니다."),
    BUSINESS_MESSAGE("C003", ""),
    ACCESS_DENIED("C004", "권한이 없습니다."),
    INTERNAL_SERVER_ERROR("C005", "서버 오류입니다.");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
