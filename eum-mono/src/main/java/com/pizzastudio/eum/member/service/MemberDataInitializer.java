package com.pizzastudio.eum.member.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pizzastudio.eum.member.domain.Member;
import com.pizzastudio.eum.member.domain.MemberRepository;
import com.pizzastudio.eum.member.domain.Role;

import lombok.extern.slf4j.Slf4j;

/**
 * 실습용 회원을 넣는다.
 *
 * <p>비밀번호는 인코딩해서 넣어야 하므로 SQL 로 두지 않는다.</p>
 */
@Slf4j
@Configuration
public class MemberDataInitializer {

    @Bean
    ApplicationRunner initMembers(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
        @Value("${eum.sample.password:eum12345!}") String samplePassword) {
        return args -> {
            if (memberRepository.count() > 0) {
                return;
            }
            String encoded = passwordEncoder.encode(samplePassword);
            memberRepository.save(Member.builder()
                .memberId("admin").password(encoded).memberName("기관 담당자")
                .contactNo("02-000-0000").emailAddr("admin@eum.go.kr").role(Role.ADMIN).build());
            memberRepository.save(Member.builder()
                .memberId("user1").password(encoded).memberName("가나다상회")
                .businessNo("123-45-67890").contactNo("010-1111-2222")
                .emailAddr("user1@example.com").role(Role.USER).build());
            memberRepository.save(Member.builder()
                .memberId("user2").password(encoded).memberName("라마바식당")
                .businessNo("234-56-78901").contactNo("010-3333-4444")
                .emailAddr("user2@example.com").role(Role.USER).build());
            log.info("실습용 회원 3명을 넣었습니다.");
        };
    }
}
