package com.pizzastudio.eum;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

/**
 * 모듈 경계를 빌드가 검증합니다(13.3).
 *
 * <p>한 번에 전부 지키게 만들 수는 없습니다. 지금은 91건이 남아 있고 4부에서 하나씩
 * 없앱니다. 그래서 "전부 지켰는가"가 아니라 <b>늘지 않았는가</b>를 봅니다.</p>
 */
class ModuleBoundaryTest {

    /** 지금 남아 있는 위반 수. 줄일 수는 있어도 늘릴 수는 없다. */
    private static final int KNOWN_VIOLATIONS = 91;

    static final ApplicationModules MODULES = ApplicationModules.of(EumApplication.class);

    @Test
    @DisplayName("모듈 목록과 남은 위반 수를 출력한다")
    void 목록() {
        MODULES.forEach(System.out::println);
    }

    @Test
    @DisplayName("경계 위반이 늘지 않는다")
    void 위반이_늘지_않는다() {
        int actual = countViolations();
        assertThat(actual)
            .as("모듈 경계 위반이 %d 건에서 %d 건으로 늘었습니다. 새 직접 호출을 넣었는지"
                + " 확인하십시오. 줄였다면 KNOWN_VIOLATIONS 를 %<d 로 낮추십시오.%n%s",
                KNOWN_VIOLATIONS, actual, violationText())
            .isLessThanOrEqualTo(KNOWN_VIOLATIONS);
    }

    @Test
    @DisplayName("아무 모듈도 알림을 직접 부르지 않는다 — 13.4 이벤트 전환")
    void 알림은_직접_부르지_않는다() {
        // 어긴 줄을 그대로 뽑아 둔다. 이것이 없으면 실패했을 때 AssertionError 만 남아
        // 어느 클래스가 무엇을 불렀는지 알 수 없다. 실제로 겪었다(13.3).
        String offending = violationText().lines()
            .filter(line -> line.contains("module 'notification'"))
            .collect(java.util.stream.Collectors.joining("\n  "));

        assertThat(offending)
            .as("알림 모듈을 직접 부르는 곳이 생겼습니다. 이벤트로 바꾸십시오(13.4).%n  %s",
                offending)
            .isEmpty();
    }

    private int countViolations() {
        String text = violationText();
        return text.isEmpty() ? 0 : text.split("depends on", -1).length - 1;
    }

    private String violationText() {
        try {
            MODULES.verify();
            return "";
        } catch (Violations e) {
            return e.getMessage();
        }
    }
}
