package com.pizzastudio.eum.core.application.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApplicationUpdateRequestDto {

    @NotNull
    @Positive
    private Long amount;

    @NotNull
    private String purposeContent;

    private String attachmentCode;
    private String applicantContactNo;
    private String applicantEmailAddr;
    private String accountNo;

    @Builder
    public ApplicationUpdateRequestDto(Long amount, String purposeContent, String attachmentCode,
        String applicantContactNo, String applicantEmailAddr, String accountNo) {
        this.amount = amount;
        this.purposeContent = purposeContent;
        this.attachmentCode = attachmentCode;
        this.applicantContactNo = applicantContactNo;
        this.applicantEmailAddr = applicantEmailAddr;
        this.accountNo = accountNo;
    }
}
