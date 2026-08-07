package com.pizzastudio.eum.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.core.application.api.dto.ApplicationCancelRequestDto;
import com.pizzastudio.eum.core.application.api.dto.ApplicationResponseDto;
import com.pizzastudio.eum.core.application.api.dto.ApplicationSaveRequestDto;
import com.pizzastudio.eum.core.application.domain.ApplicationStatus;
import com.pizzastudio.eum.core.common.exception.BusinessMessageException;
import com.pizzastudio.eum.core.outbox.domain.OutboxEventRepository;
import com.pizzastudio.eum.core.program.domain.Program;
import com.pizzastudio.eum.core.program.service.ProgramService;

/**
 * 접수는 신청 저장·예산 차감·알림을 한 트랜잭션으로 처리한다.
 * 그 사실을 시험으로 못 박아 둔다. 4부에서 이 전제가 깨진다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApplicationServiceTest {

    private static final Long PROGRAM_ID = 1L;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private ApplicationSaveRequestDto request(Long amount) {
        return ApplicationSaveRequestDto.builder()
            .programId(PROGRAM_ID)
            .amount(amount)
            .purposeContent("운영자금이 필요합니다")
            .applicantContactNo("010-1111-2222")
            .applicantEmailAddr("user1@example.com")
            .accountNo("110-000-000000")
            .build();
    }

    @Test
    @DisplayName("접수하면 예산이 그만큼 줄고 알림 요청이 아웃박스에 쌓인다")
    @WithMockUser(username = "user1", roles = "USER")
    void 접수_성공() {
        Program before = programService.findEntity(PROGRAM_ID);
        long remainBefore = before.getRemainBudget();
        long outboxBefore = outboxEventRepository.count();

        ApplicationResponseDto saved = applicationService.apply(request(1_000_000L));

        assertThat(saved.getApplicationId()).isNotBlank();
        assertThat(saved.getStatusId()).isEqualTo(ApplicationStatus.REQUEST.getKey());
        assertThat(saved.getApplicantId()).isEqualTo("user1");
        assertThat(programService.findEntity(PROGRAM_ID).getRemainBudget())
            .isEqualTo(remainBefore - 1_000_000L);
        // 알림을 직접 보내지 않는다. 아웃박스에 적힐 뿐이다(15장).
        assertThat(outboxEventRepository.count()).isEqualTo(outboxBefore + 1);
    }

    @Test
    @DisplayName("건당 한도를 넘으면 접수되지 않는다")
    @WithMockUser(username = "user1", roles = "USER")
    void 건당_한도_초과() {
        Program program = programService.findEntity(PROGRAM_ID);
        long over = program.getMaxAmountPerCase() + 1;

        assertThatThrownBy(() -> applicationService.apply(request(over)))
            .isInstanceOf(BusinessMessageException.class)
            .hasMessageContaining("건당 최대 신청 금액");
    }

    @Test
    @DisplayName("남은 예산보다 크면 접수되지 않는다")
    @WithMockUser(username = "user1", roles = "USER")
    void 예산_부족() {
        Program program = programService.findEntity(PROGRAM_ID);
        // 한도는 통과하되 잔여 예산은 모자라도록 잔여를 줄여 둔다
        program.decreaseBudget(program.getRemainBudget() - 1L);

        assertThatThrownBy(() -> applicationService.apply(request(program.getMaxAmountPerCase())))
            .isInstanceOf(BusinessMessageException.class)
            .hasMessageContaining("남은 예산이 부족");
    }

    @Test
    @DisplayName("예산이 모두 소진되면 접수되지 않는다")
    @WithMockUser(username = "user1", roles = "USER")
    void 예산_소진() {
        Program program = programService.findEntity(PROGRAM_ID);
        program.decreaseBudget(program.getRemainBudget());

        assertThatThrownBy(() -> applicationService.apply(request(1_000L)))
            .isInstanceOf(BusinessMessageException.class)
            .hasMessageContaining("예산이 모두 소진");
    }

    @Test
    @DisplayName("취소하면 차감했던 예산이 되돌아온다")
    @WithMockUser(username = "user1", roles = "USER")
    void 취소하면_예산_복원() {
        long remainBefore = programService.findEntity(PROGRAM_ID).getRemainBudget();
        ApplicationResponseDto saved = applicationService.apply(request(1_000_000L));

        applicationService.cancel(saved.getApplicationId(),
            ApplicationCancelRequestDto.builder().reason("사정이 생겼습니다").build());

        assertThat(programService.findEntity(PROGRAM_ID).getRemainBudget()).isEqualTo(remainBefore);
        assertThat(applicationService.findById(saved.getApplicationId()).getStatusId())
            .isEqualTo(ApplicationStatus.CANCEL.getKey());
    }

    @Test
    @DisplayName("남의 신청은 취소할 수 없다")
    @WithMockUser(username = "user1", roles = "USER")
    void 남의_신청_취소_불가() {
        ApplicationResponseDto saved = applicationService.apply(request(1_000_000L));

        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user2", null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));

        assertThatThrownBy(() -> applicationService.cancel(saved.getApplicationId(),
            ApplicationCancelRequestDto.builder().reason("남의 것").build()))
            .isInstanceOf(BusinessMessageException.class)
            .hasMessageContaining("취소할 수 없습니다");
    }
}
