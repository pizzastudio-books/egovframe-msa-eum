package com.pizzastudio.eum.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 잘못된 요청이 몇으로 나가는지 못 박는다.
 *
 * <p>서버가 잘못한 것이 아니라 요청이 규격과 다른 것인데도 500 이 나가면, 부르는 쪽은
 * 자기 잘못인 줄 모르고 서버 로그만 뒤진다. 실제로 겪었다 — 파일 파트 이름을 잘못
 * 보냈더니 500 이었고, 없는 경로도 500 이었다.</p>
 *
 * <p>이런 것은 업무 시험으로 잡히지 않는다. 업무 시험은 늘 올바른 요청만 보내기
 * 때문이다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("잘못된 요청의 응답 코드")
class ErrorResponseApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    @DisplayName("없는 경로는 404 다")
    void unknownPathIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/그런것은없다"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    @DisplayName("파일 파트 이름이 틀리면 400 이다")
    void wrongMultipartNameIsBadRequest() throws Exception {
        MockMultipartFile wrongName =
            new MockMultipartFile("files", "proof.txt", "text/plain", "증빙".getBytes());

        mockMvc.perform(multipart("/api/v1/applications/{id}/files", "no-such-id").file(wrongName))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("본문이 JSON 이 아니면 400 이다")
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/programs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{이건 JSON 이 아니다"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    @DisplayName("허용하지 않는 메서드는 405 다")
    void wrongMethodIsNotAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/applications/stats"))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("인증 없이 부르면 401 이다")
    void noTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/applications/mine"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    @DisplayName("권한이 모자라면 403 이다")
    void insufficientRoleIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/programs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"programName\":\"권한 확인\",\"categoryId\":\"CAT001\","
                    + "\"totalBudget\":1000000,\"requestStartDate\":\"2026-01-01T00:00:00\","
                    + "\"requestEndDate\":\"2026-12-31T23:59:59\"}"))
            .andExpect(status().isForbidden());
    }
}
