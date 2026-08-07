package com.pizzastudio.eum.core.member.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pizzastudio.eum.core.member.api.dto.LoginRequestDto;
import com.pizzastudio.eum.core.member.api.dto.LoginResponseDto;
import com.pizzastudio.eum.core.member.api.dto.MemberResponseDto;
import com.pizzastudio.eum.core.member.service.MemberService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "회원", description = "로그인과 회원 조회")
@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @PostMapping("/api/v1/auth/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto requestDto) {
        return memberService.login(requestDto);
    }

    @GetMapping("/api/v1/auth/me")
    @ResponseStatus(HttpStatus.OK)
    public MemberResponseDto me(java.security.Principal principal) {
        return memberService.findById(principal.getName());
    }

    @GetMapping("/api/v1/members/{memberId}")
    @ResponseStatus(HttpStatus.OK)
    public MemberResponseDto findById(@PathVariable("memberId") String memberId) {
        return memberService.findById(memberId);
    }
}
