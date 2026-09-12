package assembly.general.api.service;

import assembly.general.api.dto.BookDetailResponse;
import assembly.general.api.dto.BookSummaryResponse;
import assembly.general.api.dto.PagedResponse;
import assembly.general.api.entity.Book;
import assembly.general.api.exception.ResourceNotFoundException;
import assembly.general.api.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CatalogService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "author", "publicationYear");

    private final BookRepository bookRepository;

    public CatalogService(BookRepository bookRepository) {

        this.bookRepository = bookRepository;
    }

    public PagedResponse<BookSummaryResponse> search(
            int page, int size, String sortBy, String sortOrder,
            String query, String genre, String isbn, boolean availableOnly
    ) {
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "title";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, safeSortBy));

        // empty strings from query params should behave like "not provided"
        String normalizedQuery = blankToNull(query);
        String normalizedGenre = blankToNull(genre);
        String normalizedIsbn = blankToNull(isbn);

        Page<Book> result = bookRepository.search(
                normalizedQuery, normalizedGenre, normalizedIsbn, availableOnly, pageable);

        List<BookSummaryResponse> content = result.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PagedResponse<>(
                content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast()
        );
    }

    public BookDetailResponse getBookDetail(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        return new BookDetailResponse(
                book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getGenre(), book.getPublicationYear(), book.getDescription(),
                book.getPublisher(), book.getPageCount(), book.getLanguage(),
                book.getTotalCopies(), book.getAvailableCopies(), book.getStatus(),
                book.getCreatedAt(), book.getUpdatedAt()
        );
    }

    private BookSummaryResponse toSummary(Book book) {
        return new BookSummaryResponse(
                book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getGenre(), book.getPublicationYear(), book.getDescription(),
                book.getTotalCopies(), book.getAvailableCopies(), book.getStatus()
        );
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}