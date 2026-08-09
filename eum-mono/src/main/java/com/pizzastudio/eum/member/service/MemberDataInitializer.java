package com.pizzastudio.eum.member.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pizzastudio.eum.member.domain.Member;
import com.pizzastudio.eum.member.domain.MemberRepository;
import com.pizzastudio.eum.member.domain.Role;

import lombok.extern.slf4j.Slf4j;

/**
 * 실습용 회원을 넣는다.
 *
 * <p>비밀번호는 인코딩해서 넣어야 하므로 SQL 로 두지 않는다.</p>
 *
 * <p><b>벌이 둘 이상이면 겹친다.</b> 건수를 세어 비어 있을 때만 넣는데, 두 벌이 동시에 뜨면
 * 둘 다 0 을 보고 둘 다 넣는다. 뒤에 넣은 쪽이 {@code Duplicate entry 'admin'} 으로 죽고,
 * 러너에서 난 예외라 애플리케이션이 통째로 못 뜬다. 빈 데이터베이스에 두 벌을 처음 올릴 때
 * 실제로 겪었다 — 파드가 두세 번 재시작한 뒤에야 자리를 잡았다(11.2).</p>
 *
 * <p>세고 나서 넣기까지 사이가 벌어지는 것은 벌을 늘리면 늘 생긴다. 막는 자리는 데이터베이스다.
 * 기본 키가 이미 겹침을 막고 있으므로, <b>여기서는 진 쪽이 조용히 물러나면 된다.</b></p>
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
            try {
                memberRepository.saveAll(List.of(
                    Member.builder()
                        .memberId("admin").password(encoded).memberName("기관 담당자")
                        .contactNo("02-000-0000").emailAddr("admin@eum.go.kr").role(Role.ADMIN).build(),
                    Member.builder()
                        .memberId("user1").password(encoded).memberName("가나다상회")
                        .businessNo("123-45-67890").contactNo("010-1111-2222")
                        .emailAddr("user1@example.com").role(Role.USER).build(),
                    Member.builder()
                        .memberId("user2").password(encoded).memberName("라마바식당")
                        .businessNo("234-56-78901").contactNo("010-3333-4444")
                        .emailAddr("user2@example.com").role(Role.USER).build()));
                log.info("실습용 회원 3명을 넣었습니다.");
            } catch (DataIntegrityViolationException alreadyInserted) {
                // 다른 벌이 먼저 넣었다. 기본 키가 막아 준 것이므로 여기서 죽을 이유가 없다.
                log.info("실습용 회원은 다른 벌이 이미 넣었습니다.");
            }
        };
    }
}
