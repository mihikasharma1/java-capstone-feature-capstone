package assembly.general.api.dto;

import assembly.general.api.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class RoleUpdateResponse {
    private UUID userId;
    private String email;
    private Role role;
    private String message;
}