package assembly.general.api.repository;

import assembly.general.api.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {

    Optional<Book> findByIsbn(String isbn);

    // Combines the query/genre/isbn/availableOnly filters from US-005 into one
    // query — nulls just skip the corresponding condition. You'll wire this up
    // fully in Milestone 3, but the repository shape is decided here.
    @Query("""
        SELECT b FROM Book b
        WHERE (:query IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))
                              OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:genre IS NULL OR b.genre = :genre)
          AND (:isbn IS NULL OR b.isbn = :isbn)
          AND (:availableOnly = false OR b.availableCopies > 0)
        """)
    Page<Book> search(
            @Param("query") String query,
            @Param("genre") String genre,
            @Param("isbn") String isbn,
            @Param("availableOnly") boolean availableOnly,
            Pageable pageable
    );
}