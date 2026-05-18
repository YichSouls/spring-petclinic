package org.springframework.samples.weightmonitor.provider;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.weightmonitor.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/provider")
public class HealthcareProviderController {

    private final HealthcareProviderRepository providers;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public HealthcareProviderController(
            HealthcareProviderRepository providers,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.providers = providers;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<ProviderResponse> register(
            @RequestBody @Valid RegisterRequest request) {

        if (providers.findByProfessionalId(request.professionalId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Professional ID already exists");
        }

        HealthcareProvider provider = HealthcareProvider.create(
                request.professionalId(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName());

        HealthcareProvider saved = providers.save(provider);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProviderResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProviderResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateRequest request) {

        HealthcareProvider provider = providers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider not found"));

        provider.applyUpdate(
                passwordEncoder.encode(request.newPassword()),
                request.firstName(),
                request.lastName());
        return ResponseEntity.ok(new ProviderResponse(providers.save(provider)));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        HealthcareProvider provider = providers
                .findByProfessionalId(request.professionalId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), provider.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtService.generateToken(provider);
        return ResponseEntity.ok(Map.of("token", token));
    }

}

// DTO
record RegisterRequest(
        @NotBlank String professionalId,
        @NotBlank String password,
        @NotBlank String firstName,
        @NotBlank String lastName) {
}

record UpdateRequest(
        @NotBlank String newPassword,
        @NotBlank String firstName,
        @NotBlank String lastName) {
}

record LoginRequest(
        @NotBlank String professionalId,
        @NotBlank String password) {
}

record ProviderResponse(UUID id, String professionalId) {
    ProviderResponse(HealthcareProvider p) {
        this(p.getId(), p.getProfessionalId());
    }
}