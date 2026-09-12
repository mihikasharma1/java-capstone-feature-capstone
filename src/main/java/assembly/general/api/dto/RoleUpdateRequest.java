package assembly.general.api.dto;

import assembly.general.api.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateRequest {
    @NotNull
    private Role role;
}