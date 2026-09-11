package assembly.general.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class BookSummaryResponse {
    private UUID bookId;
    private String isbn;
    private String title;
    private String author;
    private String genre;
    private Integer publicationYear;
    private String description;
    private Integer totalCopies;
    private Integer availableCopies;
    private String status;
}