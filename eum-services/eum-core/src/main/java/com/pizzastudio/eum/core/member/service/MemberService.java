package com.pizzastudio.eum.core.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.core.common.config.JwtTokenProvider;
import com.pizzastudio.eum.core.common.exception.BusinessMessageException;
import com.pizzastudio.eum.core.common.exception.EntityNotFoundException;
import com.pizzastudio.eum.core.member.api.dto.LoginRequestDto;
import com.pizzastudio.eum.core.member.api.dto.LoginResponseDto;
import com.pizzastudio.eum.core.member.api.dto.MemberResponseDto;
import com.pizzastudio.eum.core.member.domain.Member;
import com.pizzastudio.eum.core.member.domain.MemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * 회원과 로그인.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public LoginResponseDto login(LoginRequestDto requestDto) {
        Member member = memberRepository.findByMemberIdAndUseAtTrue(requestDto.getMemberId())
            .orElseThrow(() -> new BusinessMessageException("아이디 또는 비밀번호가 맞지 않습니다."));

        if (!passwordEncoder.matches(requestDto.getPassword(), member.getPassword())) {
            throw new BusinessMessageException("아이디 또는 비밀번호가 맞지 않습니다.");
        }

        return LoginResponseDto.builder()
            .memberId(member.getMemberId())
            .memberName(member.getMemberName())
            .roleId(member.getRole().getKey())
            .token(tokenProvider.issue(member.getMemberId(), member.getRole().getKey()))
            .build();
    }

    public MemberResponseDto findById(String memberId) {
        return MemberResponseDto.builder().entity(findEntity(memberId)).build();
    }

    public Member findEntity(String memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new EntityNotFoundException("회원이 없습니다. ID=" + memberId));
    }
}
