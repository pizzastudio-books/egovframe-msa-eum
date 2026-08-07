package com.pizzastudio.eum.application.api.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApplicationCancelRequestDto {

    private String reason;

    @Builder
    public ApplicationCancelRequestDto(String reason) {
        this.reason = reason;
    }
}
