package assembly.general.api.controllers;

import assembly.general.api.dto.ProfileResponse;
import assembly.general.api.dto.RoleUpdateRequest;
import assembly.general.api.dto.RoleUpdateResponse;
import assembly.general.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ProfileResponse getProfile(Authentication authentication) {

        return userService.getProfile(authentication.getName());
    }

    @PatchMapping("/{userId}/role")
    public RoleUpdateResponse updateRole(
            @PathVariable UUID userId,
            @Valid @RequestBody RoleUpdateRequest request) {
        return userService.updateRole(userId, request.getRole());
    }
}