package com.pizzastudio.eum.payment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.pizzastudio.eum.payment.domain.Payment;
import com.pizzastudio.eum.payment.domain.PaymentRepository;

/**
 * 지급 조회 API.
 *
 * <p>3부에는 {@code /api/v1/applications/{id}/payment} 가 있었는데 서비스를 나누면서
 * 사라졌다. 담당자가 지급 현황을 볼 방법이 없어졌고, 아무도 알아채지 못했다 —
 * 시험이 없었기 때문이다(17.1).</p>
 *
 * <p>그래서 되살리면서 시험부터 만든다. 여기서 못박는 것은 셋이다.</p>
 * <ul>
 *   <li>담당자만 볼 수 있다 — 지급은 계좌와 금액이다</li>
 *   <li>계좌번호가 원본으로 나가지 않는다(17.2)</li>
 *   <li>신청 한 건으로 조회된다 — 3부가 하던 일</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestChannelBinderConfiguration.class)
@ActiveProfiles("test")
// 이 시험이 넣은 지급 건이 남으면 사가 시험의 셈이 어긋난다. 실제로 겪었다 —
// 아웃박스 이벤트가 1건이어야 하는데 2건이 됐다. 시험마다 되돌린다.
@org.springframework.transaction.annotation.Transactional
@DisplayName("지급 조회")
class PaymentQueryApiTest {

    private static final String APPLICATION_ID = "app-query-1";
    private static final String ACCOUNT_NO = "110-000-123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void prepare() {
        paymentRepository.deleteAll();
        paymentRepository.save(Payment.builder()
            .eventId("evt-query-1")
            .applicationId(APPLICATION_ID)
            .applicantId("user1")
            .amount(1_000_000L)
            .accountNo(ACCOUNT_NO)
            .build());
    }

    @Test
    @DisplayName("토큰이 없으면 못 본다")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/payments"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("신청자 권한으로는 못 본다 — 남의 계좌가 보이면 안 된다")
    @WithMockUser(username = "user1", roles = "USER")
    void applicantIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/payments"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("담당자는 신청번호로 지급 현황을 본다")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminFindsByApplication() throws Exception {
        mockMvc.perform(get("/api/v1/payments").param("applicationId", APPLICATION_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].applicationId").value(APPLICATION_ID))
            .andExpect(jsonPath("$[0].amount").value(1_000_000));
    }

    @Test
    @DisplayName("계좌번호는 뒷자리만 나간다")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void accountNumberIsMasked() throws Exception {
        mockMvc.perform(get("/api/v1/payments").param("applicationId", APPLICATION_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].accountNo").value("**********3456"));
    }
}
