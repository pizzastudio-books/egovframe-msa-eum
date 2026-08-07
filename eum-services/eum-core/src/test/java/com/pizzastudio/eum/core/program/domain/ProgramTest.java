package com.pizzastudio.eum.core.program.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 지원 사업 도메인 규칙.
 */
class ProgramTest {

    private Program program(long total, long remain, Long maxPerCase) {
        return Program.builder()
            .programId(1L)
            .programName("운영자금")
            .categoryId("operating")
            .totalBudget(total)
            .remainBudget(remain)
            .maxAmountPerCase(maxPerCase)
            .requestStartDate(LocalDateTime.of(2026, 1, 1, 0, 0))
            .requestEndDate(LocalDateTime.of(2026, 12, 31, 23, 59))
            .build();
    }

    @Test
    @DisplayName("접수 기간 안이면 열려 있다")
    void 접수_기간_판정() {
        Program program = program(1000, 1000, null);

        assertThat(program.isOpen(LocalDateTime.of(2026, 6, 1, 0, 0))).isTrue();
        assertThat(program.isOpen(LocalDateTime.of(2025, 12, 31, 0, 0))).isFalse();
        assertThat(program.isOpen(LocalDateTime.of(2027, 1, 1, 0, 0))).isFalse();
    }

    @Test
    @DisplayName("잔여 예산이 신청 금액 이상이어야 감당할 수 있다")
    void 예산_감당_판정() {
        Program program = program(1000, 300, null);

        assertThat(program.canAfford(300L)).isTrue();
        assertThat(program.canAfford(301L)).isFalse();
    }

    @Test
    @DisplayName("잔여 예산이 0이면 소진된 것이다")
    void 예산_소진_판정() {
        assertThat(program(1000, 1, null).hasRemainBudget()).isTrue();
        assertThat(program(1000, 0, null).hasRemainBudget()).isFalse();
    }

    @Test
    @DisplayName("건당 한도를 두지 않으면 어떤 금액도 통과한다")
    void 건당_한도_없음() {
        assertThat(program(1000, 1000, null).withinLimit(999_999L)).isTrue();
    }

    @Test
    @DisplayName("건당 한도를 넘으면 막힌다")
    void 건당_한도_초과() {
        Program program = program(1000, 1000, 100L);

        assertThat(program.withinLimit(100L)).isTrue();
        assertThat(program.withinLimit(101L)).isFalse();
    }

    @Test
    @DisplayName("예산 차감은 음수를 넣으면 되돌아간다")
    void 예산_차감과_복원() {
        Program program = program(1000, 1000, null);

        program.decreaseBudget(300L);
        assertThat(program.getRemainBudget()).isEqualTo(700L);

        program.decreaseBudget(-300L);
        assertThat(program.getRemainBudget()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("총 예산을 고치면 이미 쓴 만큼을 뺀 값이 잔여 예산이 된다")
    void 총_예산_수정() {
        Program program = program(1000, 700, null);   // 300 사용

        program.update("운영자금", 2000L, null,
            LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 12, 31, 23, 59), null);

        assertThat(program.getRemainBudget()).isEqualTo(1700L);
    }
}
