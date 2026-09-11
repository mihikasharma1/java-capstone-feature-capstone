package assembly.general.api.service;

import assembly.general.api.dto.ProfileResponse;
import assembly.general.api.entity.ReservationStatus;
import assembly.general.api.entity.User;
import assembly.general.api.repository.ReservationRepository;
import assembly.general.api.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    public UserService(UserRepository userRepository, ReservationRepository reservationRepository) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
    }

    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        long active = reservationRepository.countByUserIdAndStatusIn(
                user.getId(), List.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_OUT));


        long history = reservationRepository.findByUserId(user.getId(),
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        return new ProfileResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getPhoneNumber(), user.getRole(), user.getMembershipStatus(),
                user.getMemberSince(), active, history
        );
    }
}