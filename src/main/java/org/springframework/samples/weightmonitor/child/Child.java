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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "children")
public class Child extends Person {
    @Column(nullable = false)
    private LocalDate birthDate;

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    @Column(nullable = false)
    private Gender gender;

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    @OneToMany(mappedBy = "child", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @OrderBy("measuredAt DESC")
    private List<Measurement> measurements = new ArrayList<>();

    public static Child create(String firstName, String lastName, LocalDate birthDate, Gender gender) {
        Child c = new Child();
        c.setFirstName(firstName);
        c.setLastName(lastName);
        c.setBirthDate(birthDate);
        c.setGender(gender);
        return c;
    }
}
