package assembly.general.api.controllers;

import assembly.general.api.dto.*;
import assembly.general.api.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationCreateResponse> reserve(
            @RequestBody ReservationRequest request, Authentication authentication) {
        ReservationCreateResponse response =
                reservationService.createReservation(authentication.getName(), request.getBookId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ActiveReservationsResponse getActive(Authentication authentication) {
        return reservationService.getActiveReservations(authentication.getName());
    }

    @PostMapping("/{reservationId}/checkout")
    public CheckoutResponse checkout(
            @PathVariable UUID reservationId,
            @RequestBody(required = false) CheckoutRequest request) {
        String notes = request != null ? request.getNotes() : null;
        return reservationService.checkout(reservationId, notes);
    }

    @PostMapping("/{reservationId}/return")
    public ReturnResponse returnBook(
            @PathVariable UUID reservationId,
            @Valid @RequestBody ReturnRequest request) {
        return reservationService.returnBook(reservationId, request.getCondition(), request.getNotes());
    }

    @GetMapping("/history")
    public PagedResponse<HistoryItemResponse> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return reservationService.getHistory(authentication.getName(), page, size);
    }
}