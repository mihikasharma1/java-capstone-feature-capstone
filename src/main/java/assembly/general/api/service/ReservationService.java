package assembly.general.api.service;

import assembly.general.api.dto.*;
import assembly.general.api.entity.*;
import assembly.general.api.exception.BookUnavailableException;
import assembly.general.api.exception.InvalidStatusException;
import assembly.general.api.exception.ReservationLimitExceededException;
import assembly.general.api.exception.ResourceNotFoundException;
import assembly.general.api.repository.BookRepository;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT);
    private static final int MAX_ACTIVE_RESERVATIONS = 5;
    private static final DateTimeFormatter DUE_DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              BookRepository bookRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReservationCreateResponse createReservation(String userEmail, UUID bookId) {
        User user = getUserByEmail(userEmail);

        long activeCount = reservationRepository.countByUserIdAndStatusIn(user.getId(), ACTIVE_STATUSES);
        if (activeCount >= MAX_ACTIVE_RESERVATIONS) {
            throw new ReservationLimitExceededException(activeCount);
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw new BookUnavailableException(book.getAvailableCopies() == null ? 0 : book.getAvailableCopies());
        }

        LocalDateTime now = LocalDateTime.now();

        Reservation reservation = new Reservation();
        reservation.setBook(book);
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setReservedAt(now);
        reservation.setExpiresAt(now.plusDays(7));
        reservation.setRenewalCount(0);

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);
        Reservation saved = reservationRepository.save(reservation);

        return new ReservationCreateResponse(
                saved.getId(), book.getId(), user.getId(), book.getTitle(),
                saved.getStatus().name(), saved.getReservedAt(), saved.getExpiresAt(),
                "Book reserved successfully. Please pick up within 7 days."
        );
    }

    public ActiveReservationsResponse getActiveReservations(String userEmail) {
        User user = getUserByEmail(userEmail);

        List<Reservation> active = reservationRepository.findByUserIdAndStatusIn(user.getId(), ACTIVE_STATUSES);
        LocalDateTime now = LocalDateTime.now();

        List<ActiveReservationResponse> mapped = active.stream().map(r -> {
            boolean isReserved = r.getStatus() == ReservationStatus.RESERVED;
            Long daysUntilExpiry = isReserved
                    ? ChronoUnit.DAYS.between(now, r.getExpiresAt()) : null;
            Long daysUntilDue = !isReserved
                    ? ChronoUnit.DAYS.between(now, r.getDueDate()) : null;

            return new ActiveReservationResponse(
                    r.getId(), r.getBook().getId(), r.getBook().getTitle(), r.getBook().getAuthor(),
                    r.getStatus().name(),
                    isReserved ? r.getReservedAt() : null,
                    isReserved ? r.getExpiresAt() : null,
                    daysUntilExpiry,
                    !isReserved ? r.getCheckedOutAt() : null,
                    !isReserved ? r.getDueDate() : null,
                    daysUntilDue
            );
        }).toList();

        return new ActiveReservationsResponse(mapped, mapped.size());
    }

    @Transactional
    public CheckoutResponse checkout(UUID reservationId, String notes) {
        Reservation reservation = getReservationById(reservationId);

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new InvalidStatusException(
                    "Can only checkout reservations with RESERVED status",
                    reservation.getStatus().name());
        }

        LocalDateTime now = LocalDateTime.now();
        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservation.setCheckedOutAt(now);
        reservation.setDueDate(now.plusDays(14));
        reservation.setNotes(notes);
        reservationRepository.save(reservation);

        String message = "Book checked out successfully. Due date: "
                + reservation.getDueDate().format(DUE_DATE_FORMAT);

        return new CheckoutResponse(
                reservation.getId(), reservation.getStatus().name(),
                reservation.getCheckedOutAt(), reservation.getDueDate(), message
        );
    }

    @Transactional
    public ReturnResponse returnBook(UUID reservationId, BookCondition condition, String notes) {
        Reservation reservation = getReservationById(reservationId);

        if (reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new InvalidStatusException(
                    "Can only return books with CHECKED_OUT status",
                    reservation.getStatus().name());
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isLate = now.isAfter(reservation.getDueDate());
        long lateDays = isLate ? ChronoUnit.DAYS.between(reservation.getDueDate(), now) : 0;
        BigDecimal lateFee = BigDecimal.valueOf(lateDays).multiply(BigDecimal.valueOf(1.00));

        reservation.setStatus(ReservationStatus.RETURNED);
        reservation.setReturnedAt(now);
        reservation.setCondition(condition);
        reservation.setNotes(notes);
        reservation.setLateDays((int) lateDays);
        reservation.setLateFee(lateFee);
        reservationRepository.save(reservation);

        Book book = reservation.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        String message = lateDays > 0
                ? String.format("Book returned. Late fee of $%.2f applied to account.", lateFee)
                : "Book returned successfully";

        return new ReturnResponse(
                reservation.getId(), reservation.getReturnedAt(),
                lateDays > 0 ? reservation.getDueDate() : null, // only shown when late, per contract examples
                (int) lateDays, lateFee, message
        );
    }

    public PagedResponse<HistoryItemResponse> getHistory(String userEmail, int page, int size) {
        User user = getUserByEmail(userEmail);

        // Sorting by reservedAt desc as the "most recent first" proxy — every
        // reservation has this field populated regardless of status, unlike
        // returnedAt (null until returned), so it's a safe universal sort key.
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reservedAt"));
        Page<Reservation> result = reservationRepository.findByUserId(user.getId(), pageable);

        List<HistoryItemResponse> content = result.getContent().stream()
                .map(r -> new HistoryItemResponse(
                        r.getId(), r.getBook().getTitle(), r.getBook().getAuthor(),
                        r.getReservedAt(), r.getCheckedOutAt(), r.getReturnedAt(), r.getDueDate(),
                        r.getStatus().name(),
                        r.getReturnedAt() != null && r.getDueDate() != null && r.getReturnedAt().isAfter(r.getDueDate())
                ))
                .toList();

        return new PagedResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private Reservation getReservationById(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + id));
    }
}