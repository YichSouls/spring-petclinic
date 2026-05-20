package org.springframework.samples.weightmonitor.child;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildRepository extends JpaRepository<Child, UUID> {
    Page<Child> findByLastNameStartingWith(String lastName, Pageable pageable);

    Optional<Child> findById(UUID id);

    boolean existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDate(
            String firstName, String lastName, LocalDate birthDate);
}
