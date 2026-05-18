package org.springframework.samples.weightmonitor.measurement;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.weightmonitor.child.Child;
import org.springframework.samples.weightmonitor.child.ChildRepository;
import org.springframework.samples.weightmonitor.provider.HealthcareProvider;
import org.springframework.samples.weightmonitor.provider.HealthcareProviderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.constraints.NotBlank;

@Controller
@RequestMapping("/measurement")
public class MeasurementController {
    private final MeasurementRepository measurements;
    private final ChildRepository children;
    private final HealthcareProviderRepository providers;
    private final MeasureValidator validator;

    public MeasurementController(
            MeasurementRepository measurements,
            ChildRepository children,
            HealthcareProviderRepository providers,
            MeasureValidator validator) {
        this.measurements = measurements;
        this.children = children;
        this.providers = providers;
        this.validator = validator;
    }

    @RequestMapping("/create")
    public ResponseEntity<MeasurementResponse> createMeasurement(@RequestBody CreateMeasurementRequest request) {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "request");
        validator.validate(request, errors);
        if (errors.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }

        Child child = children.findById(UUID.fromString(request.childId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));

        HealthcareProvider provider = providers.findById(UUID.fromString(request.providerId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider not found"));

        Measurement m = Measurement.create(child, Set.of(provider), BigDecimal.valueOf(request.weight()), LocalTime.now());
        Measurement saved = measurements.save(m);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MeasurementResponse(saved));
    }
}

// DTOs for request and response
record CreateMeasurementRequest(
        @NotBlank String childId,
        @NotBlank String providerId,
        @NotBlank double weight,
        @NotBlank double height) {
}

record MeasurementResponse(UUID id) {
    MeasurementResponse(Measurement m) {
        this(m.getId());
    }
}
