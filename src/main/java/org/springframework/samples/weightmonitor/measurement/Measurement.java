package org.springframework.samples.weightmonitor.measurement;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.springframework.samples.weightmonitor.child.Child;
import org.springframework.samples.weightmonitor.model.BaseEntity;
import org.springframework.samples.weightmonitor.provider.HealthcareProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

@Entity
@Table(name = "measurements")
public class Measurement extends BaseEntity{

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "measurement_provider",
        joinColumns = @JoinColumn(name = "measurement_id"),
        inverseJoinColumns = @JoinColumn(name = "provider_id"))
    private HealthcareProvider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = true)
    private Child child;

    @Column(nullable = false)
    @DecimalMin("2.0") @DecimalMax("30.0")
    private BigDecimal weight;

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    @Column(nullable = false)
    private LocalTime measuredAt;

    public LocalTime getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(LocalTime measuredAt) {
        this.measuredAt = measuredAt;
    }
}
