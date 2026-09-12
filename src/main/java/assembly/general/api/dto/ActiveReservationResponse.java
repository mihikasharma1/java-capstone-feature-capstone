package assembly.general.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@AllArgsConstructor
public class ActiveReservationResponse {
    private UUID reservationId;
    private UUID bookId;
    private String bookTitle;
    private String bookAuthor;
    private String status;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private Long daysUntilExpiry;
    private LocalDateTime checkedOutAt;
    private LocalDateTime dueDate;
    private Long daysUntilDue;
}