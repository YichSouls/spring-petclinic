package org.springframework.samples.weightmonitor.measurement;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class MeasureValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return CreateMeasurementRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        CreateMeasurementRequest req = (CreateMeasurementRequest) target;

        try {
            UUID.fromString(req.childId());
        } catch (IllegalArgumentException e) {
            errors.rejectValue("childId", "invalid.uuid", "childId must be a valid UUID");
        }

        try {
            UUID.fromString(req.providerId());
        } catch (IllegalArgumentException e) {
            errors.rejectValue("providerId", "invalid.uuid", "providerId must be a valid UUID");
        }

        if (req.weight() < 2.0 || req.weight() > 30.0) {
            errors.rejectValue("weight", "invalid.weight", "Weight must be between 2.0 and 30.0");
        }
    }
}