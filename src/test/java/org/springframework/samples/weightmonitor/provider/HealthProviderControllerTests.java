package org.springframework.samples.weightmonitor.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.samples.weightmonitor.security.JwtAuthFilter;
import org.springframework.samples.weightmonitor.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthcareProviderController.class)
class HealthProviderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthcareProviderRepository providers;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private HealthcareProvider provider;
    private UUID providerId;

    @BeforeEach
    void setUp() throws Exception {
        providerId = UUID.randomUUID();
        provider = HealthcareProvider.create("P001", "$2a$10$hashed", "John", "Doe");

        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2))
                    .doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter)
                .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    // ── POST /provider/register ───────────────────────────────────

    @Test
    @WithMockUser
    void register_validRequest_returns201() throws Exception {
        given(providers.findByProfessionalId("P001")).willReturn(Optional.empty());
        given(passwordEncoder.encode("secret")).willReturn("$2a$10$hashed");
        given(providers.save(any())).willReturn(provider);

        mockMvc.perform(post("/provider/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"professionalId":"P001","password":"secret",
                         "firstName":"John","lastName":"Doe"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.professionalId").value("P001"));
    }

    @Test
    @WithMockUser
    void register_duplicateProfessionalId_returns409() throws Exception {
        given(providers.findByProfessionalId("P001")).willReturn(Optional.of(provider));

        mockMvc.perform(post("/provider/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"professionalId":"P001","password":"secret",
                         "firstName":"John","lastName":"Doe"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void register_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/provider/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /provider/login ──────────────────────────────────────

    @Test
    @WithMockUser
    void login_validCredentials_returnsToken() throws Exception {
        given(providers.findByProfessionalId("P001")).willReturn(Optional.of(provider));
        given(passwordEncoder.matches("secret", "$2a$10$hashed")).willReturn(true);
        given(jwtService.generateToken(provider)).willReturn("jwt.token.here");

        mockMvc.perform(post("/provider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"professionalId":"P001","password":"secret"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"));
    }

    @Test
    @WithMockUser
    void login_wrongPassword_returns401() throws Exception {
        given(providers.findByProfessionalId("P001")).willReturn(Optional.of(provider));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        mockMvc.perform(post("/provider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"professionalId":"P001","password":"wrong"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void login_unknownUser_returns401() throws Exception {
        given(providers.findByProfessionalId("UNKNOWN")).willReturn(Optional.empty());

        mockMvc.perform(post("/provider/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"professionalId":"UNKNOWN","password":"secret"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /provider/{id} ───────────────────────────────────────

    @Test
    @WithMockUser
    void getById_whenExists_returns200() throws Exception {
        given(providers.findById(providerId)).willReturn(Optional.of(provider));

        mockMvc.perform(get("/provider/{id}", providerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professionalId").value("P001"));
    }

    @Test
    @WithMockUser
    void getById_whenNotFound_returns404() throws Exception {
        given(providers.findById(any(UUID.class))).willReturn(Optional.empty());

        mockMvc.perform(get("/provider/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ── PUT /provider/{id} ───────────────────────────────────────

    @Test
    @WithMockUser
    void update_whenExists_returns200() throws Exception {
        given(providers.findById(providerId)).willReturn(Optional.of(provider));
        given(passwordEncoder.encode("newpass")).willReturn("$2a$10$newhash");
        given(providers.save(any())).willReturn(provider);

        mockMvc.perform(put("/provider/{id}", providerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"newPassword":"newpass","firstName":"John","lastName":"Doe"}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void update_whenNotFound_returns404() throws Exception {
        given(providers.findById(any(UUID.class))).willReturn(Optional.empty());

        mockMvc.perform(put("/provider/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"newPassword":"newpass","firstName":"John","lastName":"Doe"}
                        """))
                .andExpect(status().isNotFound());
    }

    // ── Unauthenticated ───────────────────────────────────────────

    @Nested
    class UnauthenticatedTests {

        @Test
        void getById_noToken_returns404() throws Exception {
            mockMvc.perform(get("/provider/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void update_noToken_returns404() throws Exception {
            mockMvc.perform(put("/provider/{id}", UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"newPassword":"x","firstName":"A","lastName":"B"}
                            """))
                    .andExpect(status().isNotFound());
        }
    }
}