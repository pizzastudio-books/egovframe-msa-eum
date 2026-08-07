package com.pizzastudio.eum.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 입력값 검증 실패 항목.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FieldErrorDto {

    private String field;
    private String value;
    private String reason;
}
