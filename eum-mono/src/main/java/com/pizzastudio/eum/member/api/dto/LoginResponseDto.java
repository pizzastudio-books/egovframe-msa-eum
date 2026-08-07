package com.pizzastudio.eum.member.api.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginResponseDto {

    private String memberId;
    private String memberName;
    private String roleId;
    private String token;

    @Builder
    public LoginResponseDto(String memberId, String memberName, String roleId, String token) {
        this.memberId = memberId;
        this.memberName = memberName;
        this.roleId = roleId;
        this.token = token;
    }
}
