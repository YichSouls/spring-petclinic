package org.springframework.samples.weightmonitor.child;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.weightmonitor.model.Gender;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ChildRepositoryTests {

    @Autowired
    private ChildRepository children;

    @BeforeEach
    @Transactional
    void setUp() {
        children.save(Child.create("John", "Doe", LocalDate.of(2010, 1, 15), Gender.MALE));
        children.save(Child.create("Jane", "Doe", LocalDate.of(2012, 5, 20), Gender.FEMALE));
        children.save(Child.create("Alice", "Smith", LocalDate.of(2011, 3, 10), Gender.FEMALE));
    }

    // ── findByLastNameStartingWith ─────────────────────────────────────

    @Test
    void findByLastName_shouldReturnMatches() {
        Page<Child> result = children.findByLastNameStartingWith("Do", Pageable.unpaged());
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByLastName_fullMatch_shouldWork() {
        Page<Child> result = children.findByLastNameStartingWith("Smith", Pageable.unpaged());
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("Alice");
    }

    @Test
    void findByLastName_noMatch_shouldReturnEmpty() {
        Page<Child> result = children.findByLastNameStartingWith("Unknown", Pageable.unpaged());
        assertThat(result).isEmpty();
    }

    @Test
    void findByLastName_emptyPrefix_shouldReturnAll() {
        Page<Child> result = children.findByLastNameStartingWith("", Pageable.unpaged());
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    // ── findById ──────────────────────────────────────────────────────

    @Test
    void findById_shouldReturnChild_whenExists() {
        Child saved = children.save(Child.create("Tom", "Brown", LocalDate.of(2015, 6, 1), Gender.MALE));
        Optional<Child> result = children.findById(saved.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("Tom");
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        Optional<Child> result = children.findById(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    // ── existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDate ──

    @Test
    void existsBy_shouldReturnTrue_whenExactMatch() {
        boolean exists = children.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDate(
                "John", "Doe", LocalDate.of(2010, 1, 15));
        assertThat(exists).isTrue();
    }

    @Test
    void existsBy_shouldReturnTrue_whenCaseDiffers() {
        boolean exists = children.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDate(
                "JOHN", "DOE", LocalDate.of(2010, 1, 15));
        assertThat(exists).isTrue();
    }

    @Test
    void existsBy_shouldReturnFalse_whenBirthDateDiffers() {
        boolean exists = children.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDate(
                "John", "Doe", LocalDate.of(2010, 1, 16)); // one day off
        assertThat(exists).isFalse();
    }

    @Test
    void existsBy_shouldReturnFalse_whenNameDiffers() {
        boolean exists = children.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDate(
                "John", "Smith", LocalDate.of(2010, 1, 15));
        assertThat(exists).isFalse();
    }
}