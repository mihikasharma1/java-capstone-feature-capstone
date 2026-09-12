package assembly.general.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@AllArgsConstructor
public class ReturnResponse {
    private UUID reservationId;
    private LocalDateTime returnedAt;
    private LocalDateTime dueDate;
    private int lateDays;
    private BigDecimal lateFee;
    private String message;
}