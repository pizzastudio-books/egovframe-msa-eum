package com.pizzastudio.eum.core.review.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewRequestDto {

    /** approve 또는 reject */
    @NotNull
    private String resultId;

    private String opinion;

    @Builder
    public ReviewRequestDto(String resultId, String opinion) {
        this.resultId = resultId;
        this.opinion = opinion;
    }
}
