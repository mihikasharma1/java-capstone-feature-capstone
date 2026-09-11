package assembly.general.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class BookDetailResponse {
    private UUID bookId;
    private String isbn;
    private String title;
    private String author;
    private String genre;
    private Integer publicationYear;
    private String description;
    private String publisher;
    private Integer pageCount;
    private String language;
    private Integer totalCopies;
    private Integer availableCopies;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}