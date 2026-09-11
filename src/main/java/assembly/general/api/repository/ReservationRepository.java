package assembly.general.api.repository;

import assembly.general.api.entity.Reservation;
import assembly.general.api.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {


    List<Reservation> findByUserIdAndStatusIn(UUID userId, List<ReservationStatus> statuses);

    long countByUserIdAndStatusIn(UUID userId, List<ReservationStatus> statuses);


    Page<Reservation> findByUserId(UUID userId, Pageable pageable);
}