package com.pizzastudio.eum.program.api.dto;

import java.time.LocalDateTime;

import com.pizzastudio.eum.program.domain.Program;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProgramSaveRequestDto {

    @NotNull
    @Size(max = 200)
    private String programName;

    @NotNull
    private String categoryId;

    @NotNull
    @Positive
    private Long totalBudget;

    private Long maxAmountPerCase;

    @NotNull
    private LocalDateTime requestStartDate;

    @NotNull
    private LocalDateTime requestEndDate;

    private LocalDateTime operationStartDate;
    private LocalDateTime operationEndDate;
    private String selectionMeansId;
    private String purposeContent;
    private String managerDeptName;
    private String contactNo;

    @Builder
    public ProgramSaveRequestDto(String programName, String categoryId, Long totalBudget,
        Long maxAmountPerCase, LocalDateTime requestStartDate, LocalDateTime requestEndDate,
        LocalDateTime operationStartDate, LocalDateTime operationEndDate, String selectionMeansId,
        String purposeContent, String managerDeptName, String contactNo) {
        this.programName = programName;
        this.categoryId = categoryId;
        this.totalBudget = totalBudget;
        this.maxAmountPerCase = maxAmountPerCase;
        this.requestStartDate = requestStartDate;
        this.requestEndDate = requestEndDate;
        this.operationStartDate = operationStartDate;
        this.operationEndDate = operationEndDate;
        this.selectionMeansId = selectionMeansId;
        this.purposeContent = purposeContent;
        this.managerDeptName = managerDeptName;
        this.contactNo = contactNo;
    }

    public Program toEntity() {
        return Program.builder()
            .programName(programName)
            .categoryId(categoryId)
            .totalBudget(totalBudget)
            .remainBudget(totalBudget)
            .maxAmountPerCase(maxAmountPerCase)
            .requestStartDate(requestStartDate)
            .requestEndDate(requestEndDate)
            .operationStartDate(operationStartDate)
            .operationEndDate(operationEndDate)
            .selectionMeansId(selectionMeansId)
            .purposeContent(purposeContent)
            .managerDeptName(managerDeptName)
            .contactNo(contactNo)
            .useAt(true)
            .build();
    }
}
