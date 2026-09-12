package assembly.general.api.config;

import assembly.general.api.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.LinkedHashMap;
import java.util.Map;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidation(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", e.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message, LocalDateTime.now()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleAuthFailure(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("AUTHENTICATION_FAILED", e.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred", LocalDateTime.now()));
    }

    @ExceptionHandler(assembly.general.api.exception.ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(assembly.general.api.exception.ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", e.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(assembly.general.api.exception.ReservationLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleLimitExceeded(assembly.general.api.exception.ReservationLimitExceededException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "RESERVATION_LIMIT_EXCEEDED");
        body.put("message", e.getMessage());
        body.put("currentReservations", e.getCurrentReservations());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(assembly.general.api.exception.BookUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleBookUnavailable(assembly.general.api.exception.BookUnavailableException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "BOOK_UNAVAILABLE");
        body.put("message", e.getMessage());
        body.put("availableCopies", e.getAvailableCopies());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(assembly.general.api.exception.InvalidStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStatus(assembly.general.api.exception.InvalidStatusException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "INVALID_STATUS");
        body.put("message", e.getMessage());
        body.put("currentStatus", e.getCurrentStatus());
        return ResponseEntity.badRequest().body(body);
    }
}