package assembly.general.api.service;

import assembly.general.api.dto.LoginRequest;
import assembly.general.api.dto.RegisterRequest;
import assembly.general.api.entity.Role;
import assembly.general.api.entity.User;
import assembly.general.api.repository.UserRepository;
import assembly.general.api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    @Test
    void register_throws_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("dup@example.com");
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_defaultsToPatronRoleAndActiveStatus() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("New");
        request.setLastName("User");
        request.setPhoneNumber("+1-555-0000");

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        var response = authService.register(request);

        assertThat(response.getRole()).isEqualTo(Role.PATRON);
        assertThat(response.getMembershipStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void login_throwsGenericMessage_onBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nope@example.com");
        request.setPassword("wrong");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password"); // never leaks *why* it failed
    }
}