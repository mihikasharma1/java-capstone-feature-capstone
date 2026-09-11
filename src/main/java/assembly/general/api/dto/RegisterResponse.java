package assembly.general.api.dto;

import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class RegisterResponse {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private MembershipStatus membershipStatus;
    private LocalDateTime createdAt;
    private String message;
}