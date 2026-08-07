package com.pizzastudio.eum.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.pizzastudio.eum.application.api.dto.ApplicationSaveRequestDto;
import com.pizzastudio.eum.application.domain.ApplicationRepository;
import com.pizzastudio.eum.notification.service.NotificationSender;
import com.pizzastudio.eum.program.api.dto.ProgramSaveRequestDto;
import com.pizzastudio.eum.program.domain.Program;
import com.pizzastudio.eum.program.service.ProgramService;

import java.util.List;

/**
 * 장애 전이 — 알림이 실패하면 접수까지 실패한다.
 *
 * <p>접수는 자격 확인·예산 차감·신청서 저장·알림 발송을 <b>한 트랜잭션</b>으로 처리한다.
 * 그래서 문자 사업자가 답하지 않으면 신청자는 접수 자체를 못 한다. 예산은 줄지 않고
 * 신청서도 남지 않으니 자료는 깨지지 않는다 — 대신 업무가 멈춘다.</p>
 *
 * <p>이 시험은 그 성질을 못 박아 둔다. 4부에서 알림을 다른 서비스로 떼어낸 뒤 같은
 * 상황을 다시 만들면, 그때는 접수가 성공하고 알림만 나중에 나가야 한다. 무엇이 달라지는지
 * 견주려면 지금 상태가 시험으로 남아 있어야 한다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("장애 전이 — 한 트랜잭션의 대가")
class FailurePropagationTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ApplicationRepository applicationRepository;

    /** 문자 사업자가 답하지 않는 상황을 만든다 */
    @MockitoBean
    private NotificationSender notificationSender;

    private Long programId;

    @BeforeEach
    void prepare() {
        loginAs("admin", "ROLE_ADMIN");
        programId = programService.save(ProgramSaveRequestDto.builder()
            .programName("장애 전이 확인")
            .categoryId("CAT001")
            .totalBudget(10_000_000L)
            .maxAmountPerCase(5_000_000L)
            .requestStartDate(LocalDateTime.now().minusDays(1))
            .requestEndDate(LocalDateTime.now().plusDays(30))
            .build()).getProgramId();
        loginAs("user1", "ROLE_USER");
    }

    @Test
    @DisplayName("알림 발송이 실패하면 접수가 통째로 되돌아간다")
    void notificationFailureRollsBackTheWholeApplication() {
        Program before = programService.findEntity(programId);
        long budgetBefore = before.getRemainBudget();
        long countBefore = applicationRepository.count();

        // 문자 사업자가 오류를 냈다
        doThrow(new IllegalStateException("문자 사업자 응답 없음"))
            .when(notificationSender).send(anyString(), anyString(), anyString(), anyString());

        assertThatThrownBy(() -> applicationService.apply(request()))
            .as("접수가 실패해야 한다 — 알림과 한 트랜잭션이기 때문이다")
            .isInstanceOf(IllegalStateException.class);

        assertThat(programService.findEntity(programId).getRemainBudget())
            .as("예산 차감이 되돌아가야 한다")
            .isEqualTo(budgetBefore);

        assertThat(applicationRepository.count())
            .as("신청서도 남지 않아야 한다")
            .isEqualTo(countBefore);
    }

    @Test
    @DisplayName("알림이 정상이면 접수는 성공하고 예산이 줄어든다")
    void baselineSucceeds() {
        long budgetBefore = programService.findEntity(programId).getRemainBudget();

        applicationService.apply(request());

        assertThat(programService.findEntity(programId).getRemainBudget())
            .as("신청 금액만큼 줄어야 한다")
            .isEqualTo(budgetBefore - 1_000_000L);
    }

    private ApplicationSaveRequestDto request() {
        return ApplicationSaveRequestDto.builder()
            .programId(programId)
            .amount(1_000_000L)
            .purposeContent("운영자금")
            .accountNo("110-000-000000")
            .bizNo("123-45-67890")
            .build();
    }

    private void loginAs(String memberId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(memberId, "N/A",
                List.of(new SimpleGrantedAuthority(role))));
    }
}
