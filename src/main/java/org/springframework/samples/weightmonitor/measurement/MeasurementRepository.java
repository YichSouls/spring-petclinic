package org.springframework.samples.weightmonitor.measurement;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeasurementRepository extends JpaRepository<Measurement, UUID> {
    Page<Measurement> findByChildId(UUID childId, Pageable pageable);

    Optional<Measurement> findById(UUID id);
}
