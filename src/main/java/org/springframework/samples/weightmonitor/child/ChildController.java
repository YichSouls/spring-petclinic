package org.springframework.samples.weightmonitor.child;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.weightmonitor.model.Gender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/child")
public class ChildController {
    private final ChildRepository children;

    public ChildController(ChildRepository children) {
        this.children = children;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Child> getById(@PathVariable UUID id) {
        Child child = children.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        return ResponseEntity.ok(child);
    }

    @GetMapping("search")
    public ResponseEntity<Page<ChildResponse>> findByLastName(
            @RequestParam(defaultValue = "") String lastName,
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        Page<ChildResponse> result = children
                .findByLastNameStartingWith(lastName, pageable)
                .map(ChildResponse::new);
        return ResponseEntity.ok(result);
    }

    @PostMapping("create")
    public ResponseEntity<ChildResponse> createChild(@RequestBody @Valid CreateChildRequest request) {
        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(request.birthDate());
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid birthDate format, expected YYYY-MM-DD");
        }

        Gender gender;
        try {
            gender = Gender.valueOf(request.gender().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid gender");
        }

        boolean exists = children.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDate(
                request.firstName(), request.lastName(), birthDate);

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Child already exists");
        }

        Child saved = children.save(
                Child.create(request.firstName(), request.lastName(), birthDate, gender));

        return ResponseEntity.status(HttpStatus.CREATED).body(new ChildResponse(saved));
    }
}

record CreateChildRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String birthDate, // ISO format: YYYY-MM-DD,
        @NotBlank String gender) {
}

record ChildResponse(UUID id) {
    ChildResponse(Child c) {
        this(c.getId());
    }
}
