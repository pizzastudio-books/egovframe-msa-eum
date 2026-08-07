package com.pizzastudio.eum.core.member.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzastudio.eum.core.member.api.dto.LoginRequestDto;

/**
 * 로그인과 토큰.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String 로그인_본문(String memberId, String password) throws Exception {
        return objectMapper.writeValueAsString(
            LoginRequestDto.builder().memberId(memberId).password(password).build());
    }

    @Test
    @DisplayName("바른 비밀번호로 로그인하면 토큰을 받는다")
    void 로그인_성공() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(로그인_본문("user1", "eum12345!")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value("user1"))
            .andExpect(jsonPath("$.roleId").value("ROLE_USER"))
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("담당자는 ADMIN 권한으로 로그인한다")
    void 담당자_로그인() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(로그인_본문("admin", "eum12345!")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roleId").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 400 이고 어느 쪽이 틀렸는지 알려주지 않는다")
    void 로그인_실패() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(로그인_본문("user1", "틀린비밀번호")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 맞지 않습니다."));
    }

    @Test
    @DisplayName("토큰 없이 보호된 자원을 부르면 401")
    void 토큰_없이_접근() throws Exception {
        mockMvc.perform(get("/api/v1/applications/mine"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("발급받은 토큰으로 보호된 자원을 부를 수 있다")
    void 토큰으로_접근() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(로그인_본문("user1", "eum12345!")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("token").asText();

        mockMvc.perform(get("/api/v1/applications/mine")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
}
