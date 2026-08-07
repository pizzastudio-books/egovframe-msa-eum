package com.pizzastudio.eum.common.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 목록 조회 조건.
 */
@Getter
@Setter
@NoArgsConstructor
public class PageRequestDto {

    private String keywordType;
    private String keyword;

    @Builder
    public PageRequestDto(String keywordType, String keyword) {
        this.keywordType = keywordType;
        this.keyword = keyword;
    }
}
