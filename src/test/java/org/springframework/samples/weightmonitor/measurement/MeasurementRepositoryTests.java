package org.springframework.samples.weightmonitor.measurement;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.weightmonitor.child.Child;
import org.springframework.samples.weightmonitor.child.ChildRepository;
import org.springframework.samples.weightmonitor.model.Gender;
import org.springframework.samples.weightmonitor.provider.HealthcareProvider;
import org.springframework.samples.weightmonitor.provider.HealthcareProviderRepository;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class MeasurementRepositoryTests {

    @Autowired
    private MeasurementRepository measurements;

    @Autowired
    private ChildRepository children;

    @Autowired
    private HealthcareProviderRepository providers;

    private Child child1;
    private Child child2;
    private HealthcareProvider provider;

    @BeforeEach
    @Transactional
    void setUp() {
        child1 = children.save(Child.create("Anna", "Repo", LocalDate.of(2015, 3, 1), Gender.FEMALE));
        child2 = children.save(Child.create("Bob", "Repo", LocalDate.of(2016, 7, 15), Gender.MALE));
        provider = providers.save(HealthcareProvider.create("doc-repo", "hashed", "Dr", "Repo"));

        measurements.save(Measurement.create(child1, Set.of(provider), new BigDecimal("10.5"), LocalTime.of(9, 0)));
        measurements.save(Measurement.create(child1, Set.of(provider), new BigDecimal("11.0"), LocalTime.of(10, 0)));
        measurements.save(Measurement.create(child2, Set.of(provider), new BigDecimal("12.0"), LocalTime.of(11, 0)));
    }

    // ── findByChildId ─────────────────────────────────────────────────

    @Test
    void findByChildId_twoMeasurements_returnsBoth() {
        Page<Measurement> result = measurements.findByChildId(child1.getId(), Pageable.unpaged());
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByChildId_singleMeasurement_returnsCorrectWeight() {
        Page<Measurement> result = measurements.findByChildId(child2.getId(), Pageable.unpaged());
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getWeight()).isEqualByComparingTo("12.0");
    }

    @Test
    void findByChildId_unknownId_returnsEmpty() {
        Page<Measurement> result = measurements.findByChildId(UUID.randomUUID(), Pageable.unpaged());
        assertThat(result).isEmpty();
    }

    @Test
    void findByChildId_paginationLimitOne_returnsTotalTwoPageSizeOne() {
        Page<Measurement> page = measurements.findByChildId(child1.getId(), PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
    }

    // ── findById ──────────────────────────────────────────────────────

    @Test
    void findById_existingId_returnsMeasurement() {
        Measurement saved = measurements.save(
                Measurement.create(child1, Set.of(provider), new BigDecimal("9.5"), LocalTime.of(8, 0)));

        Optional<Measurement> result = measurements.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getWeight()).isEqualByComparingTo("9.5");
        assertThat(result.get().getMeasuredAt()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void findById_randomId_returnsEmpty() {
        Optional<Measurement> result = measurements.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    // ── data integrity ────────────────────────────────────────────────

    @Test
    void savedMeasurement_persistsAllFields() {
        Measurement m = measurements.save(
                Measurement.create(child1, Set.of(provider), new BigDecimal("14.2"), LocalTime.of(14, 30)));
        measurements.flush();

        Measurement found = measurements.findById(m.getId()).orElseThrow();
        assertThat(found.getWeight()).isEqualByComparingTo("14.2");
        assertThat(found.getMeasuredAt()).isEqualTo(LocalTime.of(14, 30));
    }

    @Test
    void savedMeasurement_hasProvider() {
        Measurement m = measurements.save(
                Measurement.create(child1, Set.of(provider), new BigDecimal("8.0"), LocalTime.of(7, 0)));
        measurements.flush();

        Measurement found = measurements.findById(m.getId()).orElseThrow();
        assertThat(found.getProviders()).hasSize(1);
        assertThat(found.getProviders().iterator().next().getProfessionalId()).isEqualTo("doc-repo");
    }
}