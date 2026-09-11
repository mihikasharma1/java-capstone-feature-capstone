package assembly.general.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "books")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String isbn;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    private String genre;

    private Integer publicationYear;

    @Column(length = 2000)
    private String description;

    private String publisher;

    private Integer pageCount;

    private String language;

    @Column(nullable = false)
    private Integer totalCopies;

    @Column(nullable = false)
    private Integer availableCopies;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Business rule: "status is derived from available copies, not stored separately."
    // This is intentionally NOT a @Column — it's computed on demand, so it can
    // never drift out of sync with availableCopies. The DTO layer (Milestone 3)
    // will call this when building the API response.
    @Transient
    public String getStatus() {
        return availableCopies != null && availableCopies > 0 ? "AVAILABLE" : "CHECKED_OUT";
    }
}