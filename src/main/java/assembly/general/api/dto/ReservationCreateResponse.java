package assembly.general.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ReservationCreateResponse {
    private UUID reservationId;
    private UUID bookId;
    private UUID userId;
    private String bookTitle;
    private String status;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private String message;
}