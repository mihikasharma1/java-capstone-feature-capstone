package assembly.general.api.controllers;

import assembly.general.api.dto.BookCreateRequest;
import assembly.general.api.dto.BookDetailResponse;
import assembly.general.api.dto.BookSummaryResponse;
import assembly.general.api.dto.PagedResponse;
import assembly.general.api.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/books")
    public PagedResponse<BookSummaryResponse> browse(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String isbn,
            @RequestParam(defaultValue = "false") boolean availableOnly
    ) {
        return catalogService.search(page, size, sortBy, sortOrder, query, genre, isbn, availableOnly);
    }

    @GetMapping("/books/{bookId}")
    public BookDetailResponse getBook(@PathVariable UUID bookId) {
        return catalogService.getBookDetail(bookId);
    }

    @PostMapping("/books")
    public ResponseEntity<BookDetailResponse> createBook(@Valid @RequestBody BookCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createBook(request));
    }
}