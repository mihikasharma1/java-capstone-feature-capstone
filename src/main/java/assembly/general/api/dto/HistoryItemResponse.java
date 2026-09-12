package assembly.general.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class HistoryItemResponse {
    private UUID reservationId;
    private String bookTitle;
    private String bookAuthor;
    private LocalDateTime reservedAt;
    private LocalDateTime checkedOutAt;
    private LocalDateTime returnedAt;
    private LocalDateTime dueDate;
    private String status;
    private boolean wasLate;
}