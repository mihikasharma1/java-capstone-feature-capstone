package assembly.general.api.dto;

import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ProfileResponse {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Role role;
    private MembershipStatus membershipStatus;
    private LocalDateTime memberSince;
    private long activeReservations;
    private long borrowingHistory;
}