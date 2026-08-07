package com.pizzastudio.eum.program.api.dto;

import java.time.LocalDateTime;

import com.pizzastudio.eum.program.domain.Program;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProgramResponseDto {

    private Long programId;
    private String programName;
    private String categoryId;
    private String categoryName;
    private Long totalBudget;
    private Long remainBudget;
    private Long maxAmountPerCase;
    private LocalDateTime requestStartDate;
    private LocalDateTime requestEndDate;
    private String purposeContent;
    private String managerDeptName;
    private String contactNo;
    private Boolean useAt;

    @Builder
    public ProgramResponseDto(Program entity) {
        this.programId = entity.getProgramId();
        this.programName = entity.getProgramName();
        this.categoryId = entity.getCategoryId();
        this.categoryName = entity.getCategoryName();
        this.totalBudget = entity.getTotalBudget();
        this.remainBudget = entity.getRemainBudget();
        this.maxAmountPerCase = entity.getMaxAmountPerCase();
        this.requestStartDate = entity.getRequestStartDate();
        this.requestEndDate = entity.getRequestEndDate();
        this.purposeContent = entity.getPurposeContent();
        this.managerDeptName = entity.getManagerDeptName();
        this.contactNo = entity.getContactNo();
        this.useAt = entity.getUseAt();
    }
}
