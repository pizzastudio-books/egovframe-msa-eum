package com.pizzastudio.eum.code.domain;

import com.pizzastudio.eum.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통코드. 신청 상태·지원 유형·지급 상태 따위를 담는다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "code")
public class Code extends BaseEntity {

    @Id
    @Column(name = "code_id", length = 30)
    private String codeId;

    @Column(name = "code_name", length = 200, nullable = false)
    private String codeName;

    @Column(name = "parent_code_id", length = 30)
    private String parentCodeId;

    @Column(name = "sort_seq")
    private Integer sortSeq;

    @Column(name = "use_at")
    private Boolean useAt;

    @Builder
    public Code(String codeId, String codeName, String parentCodeId, Integer sortSeq, Boolean useAt) {
        this.codeId = codeId;
        this.codeName = codeName;
        this.parentCodeId = parentCodeId;
        this.sortSeq = sortSeq;
        this.useAt = useAt == null || useAt;
    }

    public Code update(String codeName, Integer sortSeq, Boolean useAt) {
        this.codeName = codeName;
        this.sortSeq = sortSeq;
        this.useAt = useAt;
        return this;
    }
}
