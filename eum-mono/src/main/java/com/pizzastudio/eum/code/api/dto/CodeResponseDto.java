package com.pizzastudio.eum.code.api.dto;

import com.pizzastudio.eum.code.domain.Code;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CodeResponseDto {

    private String codeId;
    private String codeName;
    private Integer sortSeq;

    @Builder
    public CodeResponseDto(Code entity) {
        this.codeId = entity.getCodeId();
        this.codeName = entity.getCodeName();
        this.sortSeq = entity.getSortSeq();
    }
}
