package assembly.general.api.service;

import assembly.general.api.dto.*;
import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.Role;
import assembly.general.api.entity.User;
import assembly.general.api.repository.UserRepository;
import assembly.general.api.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(Role.PATRON);
        user.setMembershipStatus(MembershipStatus.ACTIVE);
        user.setMemberSince(LocalDateTime.now());

        User saved = userRepository.save(user);

        return new RegisterResponse(
                saved.getId(), saved.getEmail(), saved.getFirstName(), saved.getLastName(),
                saved.getRole(), saved.getMembershipStatus(), saved.getCreatedAt(),
                "Registration successful"
        );
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            // deliberately identical message
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token, "Bearer", jwtService.getExpirationSeconds(),
                new AuthResponse.UserSummary(user.getId(), user.getEmail(),
                        user.getFirstName(), user.getLastName(), user.getRole())
        );
    }
}