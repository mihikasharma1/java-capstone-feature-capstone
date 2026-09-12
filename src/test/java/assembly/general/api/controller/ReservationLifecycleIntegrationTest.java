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
class ReservationLifecycleIntegrationTest {

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

    // Promotes a freshly registered patron to LIBRARIAN using the seeded
    // librarian@example.com account (bootstrapped via DataSeeder), then
    // re-logs-in so the returned token actually carries the LIBRARIAN role claim.
    private String registerAndPromoteToLibrarian(String email) throws Exception {
        String patronToken = register(email);
        String targetUserId = getUserId(patronToken);

        String seedLibrarianToken = login("librarian@example.com", SEEDED_LIBRARIAN_PASSWORD);

        mockMvc.perform(patch("/api/users/" + targetUserId + "/role")
                        .header("Authorization", "Bearer " + seedLibrarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"LIBRARIAN\"}"))
                .andExpect(status().isOk());

        return login(email, TEST_PASSWORD); // fresh token, now carrying ROLE_LIBRARIAN
    }

    private String getFirstAvailableBookId() throws Exception {
        String response = mockMvc.perform(get("/api/catalog/books")
                        .param("availableOnly", "true").param("size", "1"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("content").get(0).get("bookId").asText();
    }

    @Test
    void fullLifecycle_reserveThenCheckoutThenReturn() throws Exception {
        String patronToken = register("lifecyclepatron@example.com");
        String librarianToken = registerAndPromoteToLibrarian("lifecyclelibrarian@example.com");

        String bookId = getFirstAvailableBookId();

        // Reserve
        String reserveResponse = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":\"" + bookId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andReturn().getResponse().getContentAsString();

        String reservationId = objectMapper.readTree(reserveResponse).get("reservationId").asText();

        // Active reservations reflect it
        mockMvc.perform(get("/api/reservations").header("Authorization", "Bearer " + patronToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActive").value(1));

        // Patron cannot checkout their own reservation
        mockMvc.perform(post("/api/reservations/" + reservationId + "/checkout")
                        .header("Authorization", "Bearer " + patronToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"test\"}"))
                .andExpect(status().isForbidden());

        // Librarian checks it out
        mockMvc.perform(post("/api/reservations/" + reservationId + "/checkout")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Book condition: Good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"));

        // Re-checkout attempt fails with INVALID_STATUS
        mockMvc.perform(post("/api/reservations/" + reservationId + "/checkout")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS"));

        // Librarian returns it, on time
        mockMvc.perform(post("/api/reservations/" + reservationId + "/return")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"condition\":\"GOOD\",\"notes\":\"Returned in good condition\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lateDays").value(0))
                .andExpect(jsonPath("$.lateFee").value(0.00));

        // History now shows it as RETURNED, not late
        mockMvc.perform(get("/api/reservations/history").header("Authorization", "Bearer " + patronToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("RETURNED"))
                .andExpect(jsonPath("$.content[0].wasLate").value(false));
    }

    @Test
    void profile_returnsCorrectActiveReservationCount() throws Exception {
        String token = register("profiletest2@example.com");

        mockMvc.perform(get("/api/users/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PATRON"))
                .andExpect(jsonPath("$.activeReservations").value(0));
    }
}