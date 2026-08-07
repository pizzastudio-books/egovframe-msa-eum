package com.pizzastudio.eum.member.api.dto;

import com.pizzastudio.eum.member.domain.Member;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberResponseDto {

    private String memberId;
    private String memberName;
    private String businessNo;
    private String contactNo;
    private String emailAddr;
    private String roleId;

    @Builder
    public MemberResponseDto(Member entity) {
        this.memberId = entity.getMemberId();
        this.memberName = entity.getMemberName();
        this.businessNo = entity.getBusinessNo();
        this.contactNo = entity.getContactNo();
        this.emailAddr = entity.getEmailAddr();
        this.roleId = entity.getRole().getKey();
    }
}
