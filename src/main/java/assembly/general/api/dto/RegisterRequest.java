package assembly.general.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    // Enforces the 4-part password rule from the contract in one regex:
    //  check for lowercase, uppercase, digit, special char; min 8 chars overall.
    @NotBlank
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$",
            message = "Password must be at least 8 characters and include uppercase, lowercase, number, and special character"
    )
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9\\-() ]{7,20}$", message = "Invalid phone number format")
    private String phoneNumber;
}