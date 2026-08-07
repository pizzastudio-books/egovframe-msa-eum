package com.pizzastudio.eum.core.member.domain;

import com.pizzastudio.eum.core.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원. 소상공인(신청자)과 기관 담당자를 함께 담는다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "member")
public class Member extends BaseEntity {

    @Id
    @Column(name = "member_id", length = 50)
    private String memberId;

    @Column(name = "password", length = 200, nullable = false)
    private String password;

    @Column(name = "member_name", length = 100, nullable = false)
    private String memberName;

    @Column(name = "business_no", length = 20)
    private String businessNo;

    @Column(name = "contact_no", length = 30)
    private String contactNo;

    @Column(name = "email_addr", length = 200)
    private String emailAddr;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_id", length = 20, nullable = false)
    private Role role;

    @Column(name = "use_at")
    private Boolean useAt;

    @Builder
    public Member(String memberId, String password, String memberName, String businessNo,
        String contactNo, String emailAddr, Role role, Boolean useAt) {
        this.memberId = memberId;
        this.password = password;
        this.memberName = memberName;
        this.businessNo = businessNo;
        this.contactNo = contactNo;
        this.emailAddr = emailAddr;
        this.role = role == null ? Role.USER : role;
        this.useAt = useAt == null || useAt;
    }

    public boolean isAdmin() {
        return Role.ADMIN == this.role;
    }

    public Member updateProfile(String memberName, String contactNo, String emailAddr) {
        this.memberName = memberName;
        this.contactNo = contactNo;
        this.emailAddr = emailAddr;
        return this;
    }

    public Member changePassword(String encodedPassword) {
        this.password = encodedPassword;
        return this;
    }
}
