package org.springframework.samples.weightmonitor.provider;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthcareProviderRepository extends JpaRepository<HealthcareProvider, UUID> {

    Page<HealthcareProvider> findByLastNameStartingWith(String lastName, Pageable pageable);
    
    Optional<HealthcareProvider> findByProfessionalId(String professionalId);

    Optional<HealthcareProvider> findById(UUID id);
}
