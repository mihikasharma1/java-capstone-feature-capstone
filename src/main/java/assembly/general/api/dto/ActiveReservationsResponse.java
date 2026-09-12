package assembly.general.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ActiveReservationsResponse {
    private List<ActiveReservationResponse> reservations;
    private long totalActive;
}