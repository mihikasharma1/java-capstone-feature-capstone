package assembly.general.api.repository;

import assembly.general.api.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("dev")
class BookRepositoryTest {

    @Autowired private BookRepository bookRepository;

    @BeforeEach
    void seed() {
        save("111", "Clean Code", "Robert Martin", "Technology", 2008, 5, 2);
        save("222", "Refactoring", "Martin Fowler", "Technology", 2018, 3, 0);
        save("333", "1984", "George Orwell", "Fiction", 1949, 6, 6);
    }

    private void save(String isbn, String title, String author, String genre, int year, int total, int available) {
        Book b = new Book();
        b.setIsbn(isbn);
        b.setTitle(title);
        b.setAuthor(author);
        b.setGenre(genre);
        b.setPublicationYear(year);
        b.setTotalCopies(total);
        b.setAvailableCopies(available);
        bookRepository.save(b);
    }

    @Test
    void search_byQuery_matchesTitleOrAuthor() {
        var result = bookRepository.search("clean", null, null, false, PageRequest.of(0, 20));
        assertThat(result.getContent()).extracting(Book::getTitle).containsExactly("Clean Code");

        var byAuthor = bookRepository.search("fowler", null, null, false, PageRequest.of(0, 20));
        assertThat(byAuthor.getContent()).extracting(Book::getTitle).containsExactly("Refactoring");
    }

    @Test
    void search_byGenre_filtersExactMatch() {
        var result = bookRepository.search(null, "Technology", null, false, PageRequest.of(0, 20));
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void search_byIsbn_filtersExactMatch() {
        var result = bookRepository.search(null, null, "222", false, PageRequest.of(0, 20));
        assertThat(result.getContent()).extracting(Book::getTitle).containsExactly("Refactoring");
    }

    @Test
    void search_availableOnly_excludesZeroCopyBooks() {
        var result = bookRepository.search(null, null, null, true, PageRequest.of(0, 20));
        assertThat(result.getContent()).extracting(Book::getTitle)
                .containsExactlyInAnyOrder("Clean Code", "1984");
    }

    @Test
    void search_combinedFilters_applyTogether() {
        var result = bookRepository.search(null, "Technology", null, true, PageRequest.of(0, 20));
        assertThat(result.getContent()).extracting(Book::getTitle).containsExactly("Clean Code");
    }

    @Test
    void search_noMatches_returnsEmptyNotError() {
        var result = bookRepository.search("nonexistent", null, null, false, PageRequest.of(0, 20));
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void search_sortsByPublicationYearDescending() {
        var result = bookRepository.search(null, null, null, false,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "publicationYear")));
        assertThat(result.getContent()).extracting(Book::getTitle)
                .containsExactly("Refactoring", "Clean Code", "1984");
    }

    @Test
    void search_paginationMetadata_isCorrect() {
        var result = bookRepository.search(null, null, null, false, PageRequest.of(0, 2));
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isLast()).isFalse();
    }

    @Test
    void duplicateIsbn_violatesUniqueConstraint() {
        assertThatThrownBy(() -> {
            save("111", "Duplicate", "Someone", "Fiction", 2020, 1, 1);
            bookRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}