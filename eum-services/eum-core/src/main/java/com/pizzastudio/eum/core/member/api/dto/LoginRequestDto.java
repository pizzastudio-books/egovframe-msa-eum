package com.pizzastudio.eum.core.member.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequestDto {

    @NotNull
    private String memberId;

    @NotNull
    private String password;

    @Builder
    public LoginRequestDto(String memberId, String password) {
        this.memberId = memberId;
        this.password = password;
    }
}
