package org.springframework.samples.weightmonitor.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class HealthProviderRepositoryTests {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private HealthcareProviderRepository repository;

    private HealthcareProvider provider;

    @BeforeEach
    void setUp() {
        provider = HealthcareProvider.create("P001", "$2a$10$hashedpw", "John", "Doe");
        em.persistAndFlush(provider);
    }

    // ── findByProfessionalId ──────────────────────────────────────

    @Test
    void findByProfessionalId_whenExists_returnsProvider() {
        Optional<HealthcareProvider> result = repository.findByProfessionalId("P001");

        assertThat(result).isPresent();
        assertThat(result.get().getProfessionalId()).isEqualTo("P001");
        assertThat(result.get().getFirstName()).isEqualTo("John");
    }

    @Test
    void findByProfessionalId_whenNotExists_returnsEmpty() {
        Optional<HealthcareProvider> result = repository.findByProfessionalId("UNKNOWN");

        assertThat(result).isEmpty();
    }

    // ── findById ─────────────────────────────────────────────────

    @Test
    void findById_whenExists_returnsProvider() {
        UUID id = provider.getId();

        Optional<HealthcareProvider> result = repository.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
    }

    @Test
    void findById_whenNotExists_returnsEmpty() {
        Optional<HealthcareProvider> result = repository.findById(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    // ── findByLastNameStartingWith ────────────────────────────────

    @Test
    void findByLastNameStartingWith_matchesPrefix() {
        em.persistAndFlush(HealthcareProvider.create("P002", "$2a$10$pw2", "Jane", "Donnelly"));

        Page<HealthcareProvider> result = repository.findByLastNameStartingWith("Do", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);   // Doe + Donnelly
    }

    @Test
    void findByLastNameStartingWith_noMatch_returnsEmpty() {
        Page<HealthcareProvider> result = repository.findByLastNameStartingWith("XYZ", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
    }

    // ── uniqueness constraint ─────────────────────────────────────

    @Test
    void save_duplicateProfessionalId_throwsException() {
        HealthcareProvider duplicate = HealthcareProvider.create("P001", "$2a$10$other", "Alice", "Smith");

        assertThatThrownBy(() -> em.persistAndFlush(duplicate))
                .isInstanceOf(Exception.class);  // DataIntegrityViolationException
    }
}