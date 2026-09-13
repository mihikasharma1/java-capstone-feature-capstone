package assembly.general.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookCreateRequest {
    @NotBlank
    private String isbn;

    @NotBlank
    private String title;

    @NotBlank
    private String author;

    private String genre;

    private Integer publicationYear;

    private String description;

    private String publisher;

    private Integer pageCount;

    private String language;

    @NotNull
    @Positive
    private Integer totalCopies;
}