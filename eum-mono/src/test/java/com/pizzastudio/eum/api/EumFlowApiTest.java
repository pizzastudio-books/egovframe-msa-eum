package com.pizzastudio.eum.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzastudio.eum.application.api.dto.ApplicationCancelRequestDto;
import com.pizzastudio.eum.application.api.dto.ApplicationSaveRequestDto;
import com.pizzastudio.eum.review.api.dto.ReviewRequestDto;
import com.pizzastudio.eum.review.service.ReviewService;

/**
 * API 를 통해 접수 → 심사 → 조회까지 한 바퀴 돈다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EumFlowApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String 접수한다(Long amount, String accountNo) throws Exception {
        String body = mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ApplicationSaveRequestDto.builder()
                    .programId(1L)
                    .amount(amount)
                    .purposeContent("운영자금이 필요합니다")
                    .accountNo(accountNo)
                    // 담당자가 부를 때는 대리 접수가 된다. 신청자가 부르면 이 값은 무시된다.
                    .applicantId("user1")
                    .build())))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("applicationId").asText();
    }

    @Test
    @DisplayName("지원 사업 목록은 누구나 볼 수 있다")
    @WithMockUser(username = "user1", roles = "USER")
    void 지원사업_목록() throws Exception {
        mockMvc.perform(get("/api/v1/programs?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("신청자는 접수하고 자기 신청을 본다")
    @WithMockUser(username = "user1", roles = "USER")
    void 접수하고_내_신청_조회() throws Exception {
        String applicationId = 접수한다(1_000_000L, "110-000-000000");

        mockMvc.perform(get("/api/v1/applications/{id}", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicantId").value("user1"))
            .andExpect(jsonPath("$.statusId").value("request"))
            .andExpect(jsonPath("$.programName").value("2026년 소상공인 운영자금 지원"));

        mockMvc.perform(get("/api/v1/applications/mine"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("신청자는 전체 신청 목록을 볼 수 없다")
    @WithMockUser(username = "user1", roles = "USER")
    void 신청자는_전체목록_접근불가() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("신청자는 지원 사업을 만들 수 없다")
    @WithMockUser(username = "user1", roles = "USER")
    void 신청자는_사업등록_불가() throws Exception {
        // 본문이 올바라야 검증이 아니라 인가에서 막히는 것을 확인할 수 있다
        mockMvc.perform(post("/api/v1/programs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    com.pizzastudio.eum.program.api.dto.ProgramSaveRequestDto.builder()
                        .programName("몰래 만든 사업")
                        .categoryId("operating")
                        .totalBudget(1000L)
                        .requestStartDate(java.time.LocalDateTime.now())
                        .requestEndDate(java.time.LocalDateTime.now().plusDays(30))
                        .build())))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("입력값이 빠지면 400 과 함께 어떤 항목인지 알려준다")
    @WithMockUser(username = "user1", roles = "USER")
    void 입력값_검증_실패() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"programId\":1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("C001"))
            .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("예산을 넘겨 신청하면 400 과 사유가 온다")
    @WithMockUser(username = "user1", roles = "USER")
    void 한도_초과_접수() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ApplicationSaveRequestDto.builder()
                    .programId(1L)
                    .amount(9_999_999_999L)
                    .purposeContent("과다 신청")
                    .accountNo("110-000-000000")
                    .build())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("C003"))
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("건당 최대 신청 금액")));
    }

    @Test
    @DisplayName("공통코드 목록을 화면이 가져갈 수 있다")
    @WithMockUser(username = "user1", roles = "USER")
    void 공통코드_조회() throws Exception {
        mockMvc.perform(get("/api/v1/common-codes/{group}", "support-category"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("현재 로그인 사용자를 알려준다")
    @WithMockUser(username = "user1", roles = "USER")
    void 내_정보() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value("user1"))
            .andExpect(jsonPath("$.businessNo").value("123-45-67890"));
    }

    @Test
    @DisplayName("담당자는 상태별 건수를 본다")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void 상태별_건수() throws Exception {
        접수한다(1_000_000L, "110-000-000000");

        mockMvc.perform(get("/api/v1/applications/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.request").value(1))
            .andExpect(jsonPath("$.approve").value(0));
    }

    @Test
    @DisplayName("증빙을 올리고 목록으로 확인한다")
    @WithMockUser(username = "user1", roles = "USER")
    void 증빙_첨부() throws Exception {
        String applicationId = 접수한다(1_000_000L, "110-000-000000");

        org.springframework.mock.web.MockMultipartFile file =
            new org.springframework.mock.web.MockMultipartFile("file", "사업자등록증.pdf",
                "application/pdf", "증빙 내용".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .multipart("/api/v1/applications/{id}/files", applicationId).file(file))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.originalName").value("사업자등록증.pdf"));

        mockMvc.perform(get("/api/v1/applications/{id}/files", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("체납액이 한도를 넘는 신청자는 접수되지 않는다")
    @WithMockUser(username = "user2", roles = "USER")
    void 체납자_접수_거부() throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ApplicationSaveRequestDto.builder()
                    .programId(1L)
                    .amount(1_000_000L)
                    .purposeContent("운영자금")
                    .accountNo("110-000-000000")
                    .build())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("국세 체납액")));
    }

    @Test
    @DisplayName("없는 신청을 찾으면 404")
    @WithMockUser(username = "user1", roles = "USER")
    void 없는_신청_조회() throws Exception {
        mockMvc.perform(get("/api/v1/applications/{id}", "없는아이디"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("C002"));
    }

    @Test
    @DisplayName("담당자가 선정하면 상태가 바뀌고 심사 이력이 남는다")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void 심사_선정() throws Exception {
        String applicationId = 접수한다(1_000_000L, "110-000-000000");

        mockMvc.perform(post("/api/v1/applications/{id}/reviews", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ReviewRequestDto.builder()
                    .resultId(ReviewService.RESULT_APPROVE)
                    .opinion("적격")
                    .build())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultId").value("approve"));

        mockMvc.perform(get("/api/v1/applications/{id}", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusId").value("approve"));

        mockMvc.perform(get("/api/v1/applications/{id}/reviews", applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].opinion").value("적격"));
    }

    @Test
    @DisplayName("취소한 신청은 상태가 취소로 남는다")
    @WithMockUser(username = "user1", roles = "USER")
    void 접수_취소() throws Exception {
        String applicationId = 접수한다(1_000_000L, "110-000-000000");

        mockMvc.perform(put("/api/v1/applications/{id}/cancel", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    ApplicationCancelRequestDto.builder().reason("사정이 생겼습니다").build())))
            .andExpect(status().isNoContent());

        String body = mockMvc.perform(get("/api/v1/applications/{id}", applicationId))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(body).get("statusId").asText()).isEqualTo("cancel");
    }
}
