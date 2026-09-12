package assembly.general.api.service;

import assembly.general.api.entity.*;
import assembly.general.api.exception.ResourceNotFoundException;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ReservationRepository reservationRepository;
    @InjectMocks private UserService userService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("patron@test.com");
        user.setFirstName("Pat");
        user.setLastName("Ron");
        user.setPhoneNumber("+1-555-0000");
        user.setRole(Role.PATRON);
        user.setMembershipStatus(MembershipStatus.ACTIVE);
        user.setMemberSince(LocalDateTime.now());
    }

    @Test
    void getProfile_returnsCorrectActiveAndHistoryCounts() {
        when(userRepository.findByEmail("patron@test.com")).thenReturn(Optional.of(user));
        when(reservationRepository.countByUserIdAndStatusIn(any(), any())).thenReturn(2L);

        Page<Reservation> historyPage = new PageImpl<>(List.of(new Reservation(), new Reservation()));
        when(reservationRepository.findByUserId(any(), any(Pageable.class))).thenReturn(historyPage);

        var profile = userService.getProfile("patron@test.com");

        assertThat(profile.getEmail()).isEqualTo("patron@test.com");
        assertThat(profile.getActiveReservations()).isEqualTo(2);
        assertThat(profile.getBorrowingHistory()).isEqualTo(2);
        assertThat(profile.getRole()).isEqualTo(Role.PATRON);
    }

    @Test
    void getProfile_throws_whenAuthenticatedUserNotFound() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("ghost@test.com"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateRole_promotesUserToLibrarian() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = userService.updateRole(user.getId(), Role.LIBRARIAN);

        assertThat(response.getRole()).isEqualTo(Role.LIBRARIAN);
        assertThat(response.getUserId()).isEqualTo(user.getId());
        assertThat(user.getRole()).isEqualTo(Role.LIBRARIAN); // entity itself was mutated
    }

    @Test
    void updateRole_throwsNotFound_whenTargetUserMissing() {
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateRole(missingId, Role.LIBRARIAN))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(missingId.toString());
    }
}