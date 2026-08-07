package com.pizzastudio.eum.core.application.api.dto;

import java.util.UUID;

import com.pizzastudio.eum.core.application.domain.Application;
import com.pizzastudio.eum.core.application.domain.ApplicationStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class ApplicationSaveRequestDto {

    @NotNull
    private Long programId;

    @NotNull
    @Positive
    private Long amount;

    @NotNull
    private String purposeContent;

    private String attachmentCode;

    @Setter
    private String applicantId;

    private String bizNo;
    private String bizName;
    private String ownerName;
    private String industryCode;
    private String regionCode;

    private String applicantContactNo;

    @Email
    private String applicantEmailAddr;

    @NotNull
    private String accountNo;

    @Builder
    public ApplicationSaveRequestDto(Long programId, Long amount, String purposeContent,
        String attachmentCode, String applicantId, String bizNo, String bizName, String ownerName,
        String industryCode, String regionCode, String applicantContactNo,
        String applicantEmailAddr, String accountNo) {
        this.programId = programId;
        this.amount = amount;
        this.purposeContent = purposeContent;
        this.attachmentCode = attachmentCode;
        this.applicantId = applicantId;
        this.bizNo = bizNo;
        this.bizName = bizName;
        this.ownerName = ownerName;
        this.industryCode = industryCode;
        this.regionCode = regionCode;
        this.applicantContactNo = applicantContactNo;
        this.applicantEmailAddr = applicantEmailAddr;
        this.accountNo = accountNo;
    }

    public Application toEntity() {
        return Application.builder()
            .applicationId(UUID.randomUUID().toString())
            .programId(programId)
            .applicantId(applicantId)
            .amount(amount)
            .purposeContent(purposeContent)
            .attachmentCode(attachmentCode)
            .statusId(ApplicationStatus.REQUEST.getKey())
            .bizNo(bizNo)
            .bizName(bizName)
            .ownerName(ownerName)
            .industryCode(industryCode)
            .regionCode(regionCode)
            .applicantContactNo(applicantContactNo)
            .applicantEmailAddr(applicantEmailAddr)
            .accountNo(accountNo)
            .build();
    }
}
