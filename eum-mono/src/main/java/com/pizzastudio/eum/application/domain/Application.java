package com.pizzastudio.eum.application.domain;

import com.pizzastudio.eum.common.domain.BaseEntity;
import com.pizzastudio.eum.common.exception.BusinessMessageException;
import com.pizzastudio.eum.program.domain.Program;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 지원금 신청 한 건.
 *
 * <p>업무 구조는 표준프레임워크 MSA 템플릿(Apache-2.0)의 예약(Reserve)에서 가져왔다. 예약 수량이 신청 금액이 되고,
 * 예약 상태가 신청 상태가 된다.</p>
 */
@Getter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "application")
public class Application extends BaseEntity {

    @Id
    @Column(name = "application_id", length = 40)
    private String applicationId;

    @NotNull
    @Column(name = "program_id", nullable = false)
    private Long programId;

    @ToString.Exclude
    @Transient
    private Program program;

    @NotNull
    @Column(name = "applicant_id", length = 50, nullable = false)
    private String applicantId;

    /** 신청 금액 */
    @NotNull
    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "purpose_content", length = 4000)
    private String purposeContent;

    @Column(name = "attachment_code", length = 50)
    private String attachmentCode;

    /** 신청 상태 — 공통코드 application-status */
    @Column(name = "status_id", length = 30)
    private String statusId;

    @Column(name = "reject_reason", length = 4000)
    private String rejectReason;

    @Column(name = "biz_no", length = 20)
    private String bizNo;

    @Column(name = "biz_name", length = 200)
    private String bizName;

    @Column(name = "owner_name", length = 100)
    private String ownerName;

    @Column(name = "industry_code", length = 30)
    private String industryCode;

    @Column(name = "region_code", length = 30)
    private String regionCode;

    @Column(name = "applicant_contact_no", length = 30)
    private String applicantContactNo;

    @Column(name = "applicant_email_addr", length = 200)
    private String applicantEmailAddr;

    /**
     * 수급 계좌.
     * 접수 시점에는 본체가 들고 있다가 지급을 떼어낼 때 넘긴다(전환 설계 C안).
     */
    @Column(name = "account_no", length = 40)
    private String accountNo;

    @Builder
    public Application(String applicationId, Long programId, String applicantId, Long amount,
        String purposeContent, String attachmentCode, String statusId, String rejectReason,
        String bizNo, String bizName, String ownerName, String industryCode, String regionCode,
        String applicantContactNo, String applicantEmailAddr, String accountNo) {
        this.applicationId = applicationId;
        this.programId = programId;
        this.applicantId = applicantId;
        this.amount = amount;
        this.purposeContent = purposeContent;
        this.attachmentCode = attachmentCode;
        this.statusId = statusId;
        this.rejectReason = rejectReason;
        this.bizNo = bizNo;
        this.bizName = bizName;
        this.ownerName = ownerName;
        this.industryCode = industryCode;
        this.regionCode = regionCode;
        this.applicantContactNo = applicantContactNo;
        this.applicantEmailAddr = applicantEmailAddr;
        this.accountNo = accountNo;
    }

    public Application setProgram(Program program) {
        this.program = program;
        return this;
    }

    public boolean isApplicant(String memberId) {
        return this.applicantId != null && this.applicantId.equals(memberId);
    }

    public boolean isRequest() {
        return ApplicationStatus.REQUEST.isEquals(this.statusId);
    }

    public boolean isApproved() {
        return ApplicationStatus.APPROVE.isEquals(this.statusId);
    }

    public boolean isPaid() {
        return ApplicationStatus.PAID.isEquals(this.statusId);
    }

    public Application updateStatus(String statusId) {
        this.statusId = statusId;
        return this;
    }

    public Application approve() {
        this.statusId = ApplicationStatus.APPROVE.getKey();
        this.rejectReason = null;
        return this;
    }

    public Application reject(String reason) {
        this.statusId = ApplicationStatus.REJECT.getKey();
        this.rejectReason = reason;
        return this;
    }

    /**
     * 취소. 이미 지급된 건은 취소할 수 없다.
     */
    public Application cancel(String reason, String cantCancelMessage) {
        if (isPaid()) {
            throw new BusinessMessageException(cantCancelMessage);
        }
        this.statusId = ApplicationStatus.CANCEL.getKey();
        this.rejectReason = reason;
        return this;
    }

    /**
     * 신청 금액의 부호를 뒤집는다. 취소·반려 때 예산을 되돌리는 데 쓴다.
     */
    public Application conversionAmount() {
        if (this.amount != null) {
            this.amount = this.amount * -1;
        }
        return this;
    }

    /** 신청자가 고칠 수 있는 항목 */
    public Application updateForApplicant(Long amount, String purposeContent, String attachmentCode,
        String applicantContactNo, String applicantEmailAddr, String accountNo) {
        this.amount = amount;
        this.purposeContent = purposeContent;
        this.attachmentCode = attachmentCode;
        this.applicantContactNo = applicantContactNo;
        this.applicantEmailAddr = applicantEmailAddr;
        this.accountNo = accountNo;
        return this;
    }

    /** 기관 담당자가 고칠 수 있는 항목 */
    public Application updateByAdmin(Long amount, String purposeContent, String statusId) {
        this.amount = amount;
        this.purposeContent = purposeContent;
        this.statusId = statusId;
        return this;
    }
}
