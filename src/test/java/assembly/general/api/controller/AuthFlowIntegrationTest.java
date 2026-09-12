package assembly.general.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String registerBody(String email) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "SecurePass123!",
                "firstName", "Test", "lastName", "User", "phoneNumber", "+1-555-0000"
        ));
    }

    @Test
    void register_returns201_withPatronRoleAndActiveStatus() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("flow1@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATRON"))
                .andExpect(jsonPath("$.membershipStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("dup2@example.com")));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("dup2@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "weak@example.com", "password", "weak",
                "firstName", "T", "lastName", "U", "phoneNumber", "+1-555-0000"
        ));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fullFlow_registerThenLoginThenAccessProfile() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("flowuser@example.com")))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                Map.of("email", "flowuser@example.com", "password", "SecurePass123!"));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

        mockMvc.perform(get("/api/users/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flowuser@example.com"))
                .andExpect(jsonPath("$.activeReservations").value(0));
    }

    @Test
    void login_wrongPassword_returns401_genericMessage() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("wrongpass@example.com")));

        String badLogin = objectMapper.writeValueAsString(
                Map.of("email", "wrongpass@example.com", "password", "WrongPass1!"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(badLogin))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void profile_withoutToken_returns401AsJson() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED")); // proves JwtAuthenticationEntryPoint works
    }
}