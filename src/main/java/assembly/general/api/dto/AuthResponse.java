package assembly.general.api.dto;

import assembly.general.api.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UserSummary user;

    @Getter
    @AllArgsConstructor
    public static class UserSummary {
        private UUID userId;
        private String email;
        private String firstName;
        private String lastName;
        private Role role;
    }
}