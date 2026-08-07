package com.pizzastudio.eum.core.program.domain;

import java.time.LocalDateTime;

import com.pizzastudio.eum.core.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 지원 사업. 공고 한 건과 그 예산을 담는다.
 *
 * <p>표준프레임워크 MSA 템플릿의 예약 물품(ReserveItem)에서 왔다. 총 재고·잔여 재고가
 * 총 예산·잔여 예산이 되고, 재고 차감이 예산 차감이 된다.</p>
 */
@Getter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "program")
public class Program extends BaseEntity {

    /** 예산이 남아 있다고 볼 최소 금액 */
    private static final long MIN_BUDGET = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "program_id")
    private Long programId;

    @NotNull
    @Size(max = 200)
    @Column(name = "program_name", length = 200, nullable = false)
    private String programName;

    /** 지원 유형 — 공통코드 support-category */
    @NotNull
    @Size(max = 30)
    @Column(name = "category_id", length = 30, nullable = false)
    private String categoryId;

    @Transient
    private String categoryName;

    /** 총 예산 */
    @NotNull
    @Column(name = "total_budget", nullable = false)
    private Long totalBudget;

    /** 잔여 예산 */
    @Column(name = "remain_budget")
    private Long remainBudget;

    /** 건당 최대 신청 금액 */
    @Column(name = "max_amount_per_case")
    private Long maxAmountPerCase;

    /** 사업 운영 기간 */
    @Column(name = "operation_start_date")
    private LocalDateTime operationStartDate;

    @Column(name = "operation_end_date")
    private LocalDateTime operationEndDate;

    /** 접수 기간 */
    @NotNull
    @Column(name = "request_start_date", nullable = false)
    private LocalDateTime requestStartDate;

    @NotNull
    @Column(name = "request_end_date", nullable = false)
    private LocalDateTime requestEndDate;

    /** 선정 방법 — 공통코드 selection-means */
    @Column(name = "selection_means_id", length = 30)
    private String selectionMeansId;

    @Column(name = "purpose_content", length = 4000)
    private String purposeContent;

    @Column(name = "manager_dept_name", length = 200)
    private String managerDeptName;

    @Column(name = "contact_no", length = 30)
    private String contactNo;

    @Column(name = "use_at")
    private Boolean useAt;

    @Builder
    public Program(Long programId, String programName, String categoryId, Long totalBudget,
        Long remainBudget, Long maxAmountPerCase, LocalDateTime operationStartDate,
        LocalDateTime operationEndDate, LocalDateTime requestStartDate, LocalDateTime requestEndDate,
        String selectionMeansId, String purposeContent, String managerDeptName, String contactNo,
        Boolean useAt) {
        this.programId = programId;
        this.programName = programName;
        this.categoryId = categoryId;
        this.totalBudget = totalBudget;
        this.remainBudget = remainBudget == null ? totalBudget : remainBudget;
        this.maxAmountPerCase = maxAmountPerCase;
        this.operationStartDate = operationStartDate;
        this.operationEndDate = operationEndDate;
        this.requestStartDate = requestStartDate;
        this.requestEndDate = requestEndDate;
        this.selectionMeansId = selectionMeansId;
        this.purposeContent = purposeContent;
        this.managerDeptName = managerDeptName;
        this.contactNo = contactNo;
        this.useAt = useAt == null || useAt;
    }

    public Program setCategoryName(String categoryName) {
        this.categoryName = categoryName;
        return this;
    }

    /** 접수 기간 안인가 */
    public boolean isOpen(LocalDateTime at) {
        return !at.isBefore(requestStartDate) && !at.isAfter(requestEndDate);
    }

    /** 예산이 남아 있는가 */
    public boolean hasRemainBudget() {
        return remainBudget != null && remainBudget > MIN_BUDGET;
    }

    /** 요청 금액을 감당할 수 있는가 */
    public boolean canAfford(Long amount) {
        return amount != null && remainBudget != null && remainBudget >= amount;
    }

    /** 건당 한도를 넘지 않는가 */
    public boolean withinLimit(Long amount) {
        return maxAmountPerCase == null || (amount != null && amount <= maxAmountPerCase);
    }

    /**
     * 신청 금액만큼 예산을 차감한다. 음수를 넣으면 되돌린다.
     */
    public Program decreaseBudget(Long amount) {
        if (amount == null) {
            return this;
        }
        this.remainBudget = this.remainBudget - amount;
        return this;
    }

    public Program update(String programName, Long totalBudget, Long maxAmountPerCase,
        LocalDateTime requestStartDate, LocalDateTime requestEndDate, String purposeContent) {
        long used = this.totalBudget - this.remainBudget;
        this.programName = programName;
        this.totalBudget = totalBudget;
        this.remainBudget = totalBudget - used;
        this.maxAmountPerCase = maxAmountPerCase;
        this.requestStartDate = requestStartDate;
        this.requestEndDate = requestEndDate;
        this.purposeContent = purposeContent;
        return this;
    }

    public Program updateUseAt(Boolean useAt) {
        this.useAt = useAt;
        return this;
    }
}
