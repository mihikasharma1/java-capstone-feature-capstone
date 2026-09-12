package assembly.general.api.service;

import assembly.general.api.entity.*;
import assembly.general.api.exception.BookUnavailableException;
import assembly.general.api.exception.InvalidStatusException;
import assembly.general.api.exception.ReservationLimitExceededException;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private BookRepository bookRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private ReservationService reservationService;

    private User user;
    private Book book;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("patron@test.com");

        book = new Book();
        book.setId(UUID.randomUUID());
        book.setTitle("Test Book");
        book.setAvailableCopies(3);
    }

    @Test
    void createReservation_succeeds_whenUnderLimitAndAvailable() {
        when(userRepository.findByEmail("patron@test.com")).thenReturn(Optional.of(user));
        when(reservationRepository.countByUserIdAndStatusIn(any(), any())).thenReturn(2L);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = reservationService.createReservation("patron@test.com", book.getId());

        assertThat(response.getStatus()).isEqualTo("RESERVED");
        assertThat(book.getAvailableCopies()).isEqualTo(2); // decremented
        verify(bookRepository).save(book);
    }

    @Test
    void createReservation_throws_whenAtFiveActiveReservations() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(reservationRepository.countByUserIdAndStatusIn(any(), any())).thenReturn(5L);

        assertThatThrownBy(() -> reservationService.createReservation("patron@test.com", book.getId()))
                .isInstanceOf(ReservationLimitExceededException.class);

        verifyNoInteractions(bookRepository); // should short-circuit before checking the book at all
    }

    @Test
    void createReservation_throws_whenBookHasZeroAvailableCopies() {
        book.setAvailableCopies(0);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(reservationRepository.countByUserIdAndStatusIn(any(), any())).thenReturn(0L);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> reservationService.createReservation("patron@test.com", book.getId()))
                .isInstanceOf(BookUnavailableException.class);
    }

    @Test
    void createReservation_expiresAt_isSevenDaysFromReservedAt() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(reservationRepository.countByUserIdAndStatusIn(any(), any())).thenReturn(0L);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = reservationService.createReservation("patron@test.com", book.getId());

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                response.getReservedAt(), response.getExpiresAt());
        assertThat(daysBetween).isEqualTo(7);
    }

    @Test
    void checkout_throws_whenReservationNotInReservedStatus() {
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setStatus(ReservationStatus.CHECKED_OUT); // already checked out

        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.checkout(reservation.getId(), "notes"))
                .isInstanceOf(InvalidStatusException.class);
    }

    @Test
    void checkout_setsDueDate_fourteenDaysOut() {
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setStatus(ReservationStatus.RESERVED);

        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = reservationService.checkout(reservation.getId(), null);

        long days = java.time.temporal.ChronoUnit.DAYS.between(
                response.getCheckedOutAt(), response.getDueDate());
        assertThat(days).isEqualTo(14);
    }

    @Test
    void returnBook_calculatesZeroFee_whenOnTime() {
        Reservation reservation = buildCheckedOutReservation(LocalDateTime.now().plusDays(2)); // due in future

        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = reservationService.returnBook(reservation.getId(), BookCondition.GOOD, null);

        assertThat(response.getLateDays()).isZero();
        assertThat(response.getLateFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void returnBook_calculatesFee_atOneDollarPerLateDay() {
        Reservation reservation = buildCheckedOutReservation(LocalDateTime.now().minusDays(3)); // 3 days overdue

        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = reservationService.returnBook(reservation.getId(), BookCondition.GOOD, null);

        assertThat(response.getLateDays()).isEqualTo(3);
        assertThat(response.getLateFee()).isEqualByComparingTo(new BigDecimal("3.00"));
    }

    @Test
    void returnBook_incrementsAvailableCopies() {
        book.setAvailableCopies(1);
        Reservation reservation = buildCheckedOutReservation(LocalDateTime.now().plusDays(1));
        reservation.setBook(book);

        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        reservationService.returnBook(reservation.getId(), BookCondition.GOOD, null);

        assertThat(book.getAvailableCopies()).isEqualTo(2);
    }

    @Test
    void returnBook_throws_whenNotCheckedOut() {
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setStatus(ReservationStatus.RESERVED);

        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.returnBook(reservation.getId(), BookCondition.GOOD, null))
                .isInstanceOf(InvalidStatusException.class);
    }

    private Reservation buildCheckedOutReservation(LocalDateTime dueDate) {
        Reservation r = new Reservation();
        r.setId(UUID.randomUUID());
        r.setBook(book);
        r.setStatus(ReservationStatus.CHECKED_OUT);
        r.setCheckedOutAt(LocalDateTime.now().minusDays(5));
        r.setDueDate(dueDate);
        return r;
    }
}