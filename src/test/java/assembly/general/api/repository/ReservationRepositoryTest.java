package assembly.general.api.repository;

import assembly.general.api.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("dev")
class ReservationRepositoryTest {

    @Autowired private ReservationRepository reservationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BookRepository bookRepository;

    private User user;
    private Book book;

    @BeforeEach
    void seed() {
        user = new User();
        user.setEmail("patron@test.com");
        user.setPasswordHash("hash");
        user.setFirstName("Pat");
        user.setLastName("Ron");
        user.setRole(Role.PATRON);
        user.setMembershipStatus(MembershipStatus.ACTIVE);
        user.setMemberSince(LocalDateTime.now());
        user = userRepository.save(user);

        book = new Book();
        book.setIsbn("999");
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setTotalCopies(5);
        book.setAvailableCopies(3);
        book = bookRepository.save(book);
    }

    private Reservation makeReservation(ReservationStatus status) {
        Reservation r = new Reservation();
        r.setBook(book);
        r.setUser(user);
        r.setStatus(status);
        r.setReservedAt(LocalDateTime.now());
        r.setExpiresAt(LocalDateTime.now().plusDays(7));
        r.setRenewalCount(0);
        return reservationRepository.save(r);
    }

    @Test
    void countByUserIdAndStatusIn_countsOnlyActiveStatuses() {
        makeReservation(ReservationStatus.RESERVED);
        makeReservation(ReservationStatus.CHECKED_OUT);
        makeReservation(ReservationStatus.RETURNED); // should NOT count
        makeReservation(ReservationStatus.CANCELLED); // should NOT count

        long active = reservationRepository.countByUserIdAndStatusIn(
                user.getId(), List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT));

        assertThat(active).isEqualTo(2);
    }

    @Test
    void findByUserIdAndStatusIn_returnsOnlyMatchingStatuses() {
        makeReservation(ReservationStatus.RESERVED);
        makeReservation(ReservationStatus.RETURNED);

        List<Reservation> active = reservationRepository.findByUserIdAndStatusIn(
                user.getId(), List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT));

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void findByUserId_includesAllStatusesForHistory() {
        makeReservation(ReservationStatus.RESERVED);
        makeReservation(ReservationStatus.RETURNED);
        makeReservation(ReservationStatus.CANCELLED);

        var history = reservationRepository.findByUserId(user.getId(), PageRequest.of(0, 20));

        assertThat(history.getTotalElements()).isEqualTo(3);
    }

    @Test
    void reservation_resolvesBookAndUserRelationships() {
        Reservation saved = makeReservation(ReservationStatus.RESERVED);

        Reservation reloaded = reservationRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getBook().getTitle()).isEqualTo("Test Book");
        assertThat(reloaded.getUser().getEmail()).isEqualTo("patron@test.com");
    }

    @Test
    void findByUserId_emptyForUnknownUser_returnsEmptyPage() {
        var result = reservationRepository.findByUserId(UUID.randomUUID(), PageRequest.of(0, 20));
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}