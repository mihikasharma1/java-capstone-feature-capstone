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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RoleManagementIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static final String TEST_PASSWORD = "SecurePass123!";
    private static final String SEEDED_LIBRARIAN_PASSWORD = "TestPass123!";

    private String register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email, "password", TEST_PASSWORD,
                        "firstName", "T", "lastName", "U", "phoneNumber", "+1-555-0000"))));
        return login(email, TEST_PASSWORD);
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String getUserId(String token) throws Exception {
        String response = mockMvc.perform(get("/api/users/profile").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("userId").asText();
    }

    // librarian@example.com is seeded by DataSeeder
    // this is the "first librarian" bootstrapped outside the API
    private String loginAsSeededLibrarian() throws Exception {
        return login("librarian@example.com", SEEDED_LIBRARIAN_PASSWORD);
    }

    @Test
    void patronCannotPromoteAnyone_returns403() throws Exception {
        String patronToken = register("rolepatron1@example.com");
        String targetUserId = getUserId(register("roletarget1@example.com"));

        mockMvc.perform(patch("/api/users/" + targetUserId + "/role")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"LIBRARIAN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void librarianCanPromotePatronToLibrarian() throws Exception {
        String librarianToken = loginAsSeededLibrarian();
        String targetUserId = getUserId(register("roletarget2@example.com"));

        mockMvc.perform(patch("/api/users/" + targetUserId + "/role")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"LIBRARIAN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("LIBRARIAN"));
    }

    @Test
    void promotingNonexistentUser_returns404() throws Exception {
        String librarianToken = loginAsSeededLibrarian();
        String fakeUserId = UUID.randomUUID().toString();

        mockMvc.perform(patch("/api/users/" + fakeUserId + "/role")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"LIBRARIAN\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void roleUpdateWithoutToken_returns401() throws Exception {
        String targetUserId = getUserId(register("roletarget3@example.com"));

        mockMvc.perform(patch("/api/users/" + targetUserId + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"LIBRARIAN\"}"))
                .andExpect(status().isUnauthorized());
    }
}