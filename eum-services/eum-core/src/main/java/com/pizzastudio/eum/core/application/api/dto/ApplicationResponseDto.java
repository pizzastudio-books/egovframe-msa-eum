package com.pizzastudio.eum.core.application.api.dto;

import java.time.LocalDateTime;

import com.pizzastudio.eum.core.application.domain.Application;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApplicationResponseDto {

    private String applicationId;
    private Long programId;
    private String programName;
    private String applicantId;
    private Long amount;
    private String purposeContent;
    private String attachmentCode;
    private String statusId;
    private String rejectReason;
    private String bizNo;
    private String bizName;
    private String ownerName;
    private String industryCode;
    private String regionCode;
    private String statusLabel;
    private String applicantContactNo;
    private String applicantEmailAddr;
    private String accountNo;
    private LocalDateTime createDate;

    @Builder
    public ApplicationResponseDto(Application entity, String statusLabel) {
        this.applicationId = entity.getApplicationId();
        this.programId = entity.getProgramId();
        this.programName = entity.getProgram() == null ? null : entity.getProgram().getProgramName();
        this.applicantId = entity.getApplicantId();
        this.amount = entity.getAmount();
        this.purposeContent = entity.getPurposeContent();
        this.attachmentCode = entity.getAttachmentCode();
        this.statusId = entity.getStatusId();
        this.rejectReason = entity.getRejectReason();
        this.bizNo = entity.getBizNo();
        this.bizName = entity.getBizName();
        this.ownerName = entity.getOwnerName();
        this.industryCode = entity.getIndustryCode();
        this.regionCode = entity.getRegionCode();
        this.statusLabel = statusLabel;
        this.applicantContactNo = entity.getApplicantContactNo();
        this.applicantEmailAddr = entity.getApplicantEmailAddr();
        this.accountNo = entity.getAccountNo();
        this.createDate = entity.getCreateDate();
    }
}
