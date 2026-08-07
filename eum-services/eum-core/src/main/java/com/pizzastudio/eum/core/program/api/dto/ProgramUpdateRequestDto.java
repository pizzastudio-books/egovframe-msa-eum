package com.pizzastudio.eum.core.program.api.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProgramUpdateRequestDto {

    @NotNull
    private String programName;

    @NotNull
    @Positive
    private Long totalBudget;

    private Long maxAmountPerCase;

    @NotNull
    private LocalDateTime requestStartDate;

    @NotNull
    private LocalDateTime requestEndDate;

    private String purposeContent;

    @Builder
    public ProgramUpdateRequestDto(String programName, Long totalBudget, Long maxAmountPerCase,
        LocalDateTime requestStartDate, LocalDateTime requestEndDate, String purposeContent) {
        this.programName = programName;
        this.totalBudget = totalBudget;
        this.maxAmountPerCase = maxAmountPerCase;
        this.requestStartDate = requestStartDate;
        this.requestEndDate = requestEndDate;
        this.purposeContent = purposeContent;
    }
}
