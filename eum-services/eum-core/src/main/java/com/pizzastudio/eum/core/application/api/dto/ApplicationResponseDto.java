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
        this.accountNo = maskAccount(entity.getAccountNo());
        this.createDate = entity.getCreateDate();
    }

    /**
     * 계좌는 뒷자리만 내보낸다.
     *
     * <p>지급을 떼어내면서 "계좌 원본은 지급 서비스에만 있다"고 적었는데, 본체 응답이
     * 원본을 그대로 내보내고 있었다. 클러스터에서 확인했다 — 신청 조회 응답에
     * {@code "accountNo":"110-000-123456"} 이 그대로 나왔다(17.2).</p>
     *
     * <p>본체가 계좌를 아예 안 갖는 것이 옳지만, 접수 시점에는 받아야 하므로 지금은
     * 내보내는 것만 막는다. 데이터 자체를 옮기는 것은 별도 과제다.</p>
     */
    private static String maskAccount(String accountNo) {
        if (accountNo == null || accountNo.length() <= 4) {
            return accountNo;
        }
        return "*".repeat(accountNo.length() - 4) + accountNo.substring(accountNo.length() - 4);
    }
}
