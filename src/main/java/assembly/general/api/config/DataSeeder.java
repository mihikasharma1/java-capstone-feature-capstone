package assembly.general.api.config;

import assembly.general.api.entity.*;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

// @Profile("dev") ensures this NEVER runs against prod/Postgres
@Configuration
@Profile("dev")
public class DataSeeder {

    @Bean
    public CommandLineRunner seedDatabase(
            UserRepository userRepository,
            BookRepository bookRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            seedUsers(userRepository, passwordEncoder);
            seedBooks(bookRepository);
            System.out.println("✅ Dev data seeded: "
                    + userRepository.count() + " users, "
                    + bookRepository.count() + " books.");
        };
    }

    private void seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        // Same password for both, for convenience: TestPass123!
        User patron = new User();
        patron.setEmail("patron@example.com");
        patron.setPasswordHash(passwordEncoder.encode("TestPass123!"));
        patron.setFirstName("Peggy");
        patron.setLastName("Patron");
        patron.setPhoneNumber("+1-555-0100");
        patron.setRole(Role.PATRON);
        patron.setMembershipStatus(MembershipStatus.ACTIVE);
        patron.setMemberSince(LocalDateTime.now().minusMonths(6));
        userRepository.save(patron);

        User librarian = new User();
        librarian.setEmail("librarian@example.com");
        librarian.setPasswordHash(passwordEncoder.encode("TestPass123!"));
        librarian.setFirstName("Larry");
        librarian.setLastName("Librarian");
        librarian.setPhoneNumber("+1-555-0200");
        librarian.setRole(Role.LIBRARIAN);
        librarian.setMembershipStatus(MembershipStatus.ACTIVE);
        librarian.setMemberSince(LocalDateTime.now().minusYears(2));
        userRepository.save(librarian);

        // A second patron, useful for testing the 5-reservation limit
        // without touching your "main" test patron.
        User patron2 = new User();
        patron2.setEmail("patron2@example.com");
        patron2.setPasswordHash(passwordEncoder.encode("TestPass123!"));
        patron2.setFirstName("Paul");
        patron2.setLastName("Reader");
        patron2.setPhoneNumber("+1-555-0300");
        patron2.setRole(Role.PATRON);
        patron2.setMembershipStatus(MembershipStatus.ACTIVE);
        patron2.setMemberSince(LocalDateTime.now().minusDays(30));
        userRepository.save(patron2);
    }

    private void seedBooks(BookRepository bookRepository) {
        saveBook(bookRepository, "978-0-13-468599-1", "Clean Code", "Robert C. Martin",
                "Technology", 2008, "A handbook of agile software craftsmanship",
                "Prentice Hall", 464, "English", 5, 3);

        saveBook(bookRepository, "978-0-13-475759-9", "Refactoring", "Martin Fowler",
                "Technology", 2018, "Improving the design of existing code",
                "Addison-Wesley", 448, "English", 3, 0); // fully checked out — tests BOOK_UNAVAILABLE

        saveBook(bookRepository, "978-0-596-00712-6", "Head First Design Patterns", "Eric Freeman",
                "Technology", 2004, "A brain-friendly guide to design patterns",
                "O'Reilly Media", 694, "English", 4, 4);

        saveBook(bookRepository, "978-0-452-28423-4", "1984", "George Orwell",
                "Fiction", 1949, "A dystopian social science fiction novel",
                "Signet Classics", 328, "English", 6, 2);

        saveBook(bookRepository, "978-0-14-118776-1", "Crime and Punishment", "Fyodor Dostoevsky",
                "Fiction", 1866, "A novel exploring morality and redemption",
                "Penguin Classics", 671, "English", 2, 1);

        saveBook(bookRepository, "978-0-06-231609-7", "Sapiens", "Yuval Noah Harari",
                "Non-Fiction", 2011, "A brief history of humankind",
                "Harper", 443, "English", 5, 5);

        saveBook(bookRepository, "978-0-345-53980-3", "The Martian", "Andy Weir",
                "Science Fiction", 2011, "An astronaut stranded on Mars fights to survive",
                "Broadway Books", 384, "English", 3, 1);
    }

    private void saveBook(BookRepository repo, String isbn, String title, String author,
                          String genre, int year, String description, String publisher,
                          int pageCount, String language, int totalCopies, int availableCopies) {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setGenre(genre);
        book.setPublicationYear(year);
        book.setDescription(description);
        book.setPublisher(publisher);
        book.setPageCount(pageCount);
        book.setLanguage(language);
        book.setTotalCopies(totalCopies);
        book.setAvailableCopies(availableCopies);
        repo.save(book);
    }
}