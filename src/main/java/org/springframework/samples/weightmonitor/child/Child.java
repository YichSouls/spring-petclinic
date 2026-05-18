package org.springframework.samples.weightmonitor.child;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.samples.weightmonitor.measurement.Measurement;
import org.springframework.samples.weightmonitor.model.Gender;
import org.springframework.samples.weightmonitor.model.Person;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "children")
public class Child extends Person {
    @Column
    @NotBlank
    private LocalDate birthDate;

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    @Column
    @NotBlank
    private Gender gender;

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    @OrderBy("measuredAt DESC")
    private final List<Measurement> measurements = new ArrayList<>();

    public List<Measurement> getMeasurements() {
        return measurements;
    }

    public void addMeasurement(Measurement measurement) {
        if (measurement.isNew()) {
            this.measurements.add(measurement);
        }
    }
}
