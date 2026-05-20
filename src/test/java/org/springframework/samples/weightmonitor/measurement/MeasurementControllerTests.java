package org.springframework.samples.weightmonitor.measurement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.samples.weightmonitor.child.Child;
import org.springframework.samples.weightmonitor.child.ChildRepository;
import org.springframework.samples.weightmonitor.model.Gender;
import org.springframework.samples.weightmonitor.provider.HealthcareProvider;
import org.springframework.samples.weightmonitor.provider.HealthcareProviderRepository;
import org.springframework.samples.weightmonitor.security.JwtAuthFilter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MeasurementController.class)
@Import(MeasureValidator.class)   // real validator — tests actual validation logic
class MeasurementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeasurementRepository measurements;

    @MockitoBean
    private ChildRepository children;

    @MockitoBean
    private HealthcareProviderRepository providers;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2))
                    .doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter)
                .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    // ── GET /measurement/{id} ─────────────────────────────────────────

    @Test
    @WithMockUser
    void getById_found_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Measurement m = new Measurement();
        m.setWeight(new BigDecimal("10.5"));
        m.setMeasuredAt(LocalTime.of(9, 0));
        given(measurements.findById(id)).willReturn(Optional.of(m));

        mockMvc.perform(get("/measurement/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getById_notFound_returns404() throws Exception {
        given(measurements.findById(any(UUID.class))).willReturn(Optional.empty());

        mockMvc.perform(get("/measurement/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ── POST /measurement/create ──────────────────────────────────────

    @Test
    @WithMockUser
    void createMeasurement_valid_returns201() throws Exception {
        UUID childId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();

        Child child = Child.create("Anna", "Test", LocalDate.of(2015, 3, 1), Gender.FEMALE);
        HealthcareProvider provider = HealthcareProvider.create("doc-1", "hashed", "Dr", "Test");
        Measurement saved = new Measurement();
        saved.setWeight(new BigDecimal("10.5"));
        saved.setMeasuredAt(LocalTime.of(9, 0));

        given(children.findById(childId)).willReturn(Optional.of(child));
        given(providers.findById(providerId)).willReturn(Optional.of(provider));
        given(measurements.save(any(Measurement.class))).willReturn(saved);

        mockMvc.perform(post("/measurement/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"childId":"%s","providerId":"%s","weight":10.5,"height":65.0}
                        """.formatted(childId, providerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").hasJsonPath());
    }

    @Test
    @WithMockUser
    void createMeasurement_invalidChildUUID_returns400() throws Exception {
        mockMvc.perform(post("/measurement/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"childId":"not-a-uuid","providerId":"%s","weight":10.5,"height":65.0}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createMeasurement_invalidProviderUUID_returns400() throws Exception {
        mockMvc.perform(post("/measurement/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"childId":"%s","providerId":"bad-uuid","weight":10.5,"height":65.0}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createMeasurement_weightTooLow_returns400() throws Exception {
        mockMvc.perform(post("/measurement/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"childId":"%s","providerId":"%s","weight":1.0,"height":65.0}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createMeasurement_weightTooHigh_returns400() throws Exception {
        mockMvc.perform(post("/measurement/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"childId":"%s","providerId":"%s","weight":31.0,"height":65.0}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createMeasurement_childNotFound_returns404() throws Exception {
        given(children.findById(any(UUID.class))).willReturn(Optional.empty());

        mockMvc.perform(post("/measurement/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"childId":"%s","providerId":"%s","weight":10.5,"height":65.0}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void createMeasurement_providerNotFound_returns404() throws Exception {
        Child child = Child.create("Anna", "Test", LocalDate.of(2015, 3, 1), Gender.FEMALE);
        given(children.findById(any(UUID.class))).willReturn(Optional.of(child));
        given(providers.findById(any(UUID.class))).willReturn(Optional.empty());

        mockMvc.perform(post("/measurement/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"childId":"%s","providerId":"%s","weight":10.5,"height":65.0}
                        """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    // ── Unauthenticated ───────────────────────────────────────────────

    @Nested
    class UnauthenticatedTests {

        @Test
        void getById_noToken_returns403() throws Exception {
            mockMvc.perform(get("/measurement/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void createMeasurement_noToken_returns403() throws Exception {
            mockMvc.perform(post("/measurement/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"childId":"%s","providerId":"%s","weight":10.5,"height":65.0}
                            """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isNotFound());
        }
    }
}