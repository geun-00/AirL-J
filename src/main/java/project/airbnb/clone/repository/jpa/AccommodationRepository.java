package project.airbnb.clone.repository.jpa;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.airbnb.clone.entity.accommodation.Accommodation;

import java.util.List;
import java.util.Optional;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

	Optional<Accommodation> findByContentId(String tourApiId);

    List<Accommodation> findByContentIdIn(List<String> contentIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Accommodation a where a.id = :id")
    Optional<Accommodation> findByIdWithPessimisticLock(@Param("id") Long id);
}
