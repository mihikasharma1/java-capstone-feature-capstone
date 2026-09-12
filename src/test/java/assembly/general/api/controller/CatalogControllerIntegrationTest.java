package assembly.general.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CatalogControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void browse_isPublic_noAuthRequired() throws Exception {
        mockMvc.perform(get("/api/catalog/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void browse_appliesDefaultSortAndPagination() throws Exception {
        mockMvc.perform(get("/api/catalog/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getBookDetail_nonexistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/catalog/books/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void search_noMatches_returnsEmptyContentNotError() throws Exception {
        mockMvc.perform(get("/api/catalog/books").param("query", "zzz_no_such_book_zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}