package org.springframework.samples.weightmonitor.child;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.samples.weightmonitor.model.Gender;
import org.springframework.samples.weightmonitor.security.JwtAuthFilter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChildController.class)
class ChildControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChildRepository children;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter; // prevents real filter instantiation

    @BeforeEach
    void setup() throws Exception {
        // make the mock filter pass through to the controller
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2))
                    .doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter)
                .doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    // ── POST /child/create ────────────────────────────────────────────

    @Test
    @WithMockUser
    void createChild_success_returns201() throws Exception {
        Child child = Child.create("John", "Doe", LocalDate.of(2010, 1, 15), Gender.MALE);
        given(children.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDate(
                any(), any(), any())).willReturn(false);
        given(children.save(any())).willReturn(child);

        mockMvc.perform(post("/child/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName":"John","lastName":"Doe",
                         "birthDate":"2010-01-15","gender":"MALE"}
                        """))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void createChild_duplicate_returns409() throws Exception {
        given(children.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndBirthDate(
                any(), any(), any())).willReturn(true);

        mockMvc.perform(post("/child/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName":"John","lastName":"Doe",
                         "birthDate":"2010-01-15","gender":"MALE"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void createChild_invalidBirthDate_returns400() throws Exception {
        mockMvc.perform(post("/child/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName":"John","lastName":"Doe",
                         "birthDate":"not-a-date","gender":"MALE"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createChild_invalidGender_returns400() throws Exception {
        mockMvc.perform(post("/child/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName":"John","lastName":"Doe",
                         "birthDate":"2010-01-15","gender":"UNKNOWN"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createChild_blankFirstName_returns400() throws Exception {
        mockMvc.perform(post("/child/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName":"","lastName":"Doe",
                         "birthDate":"2010-01-15","gender":"MALE"}
                        """))
                .andExpect(status().isBadRequest());
    }

    // ── GET /child/{id} ───────────────────────────────────────────────

    @Test
    @WithMockUser
    void getById_found_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Child child = Child.create("Jane", "Smith", LocalDate.of(2012, 3, 10), Gender.FEMALE);
        given(children.findById(id)).willReturn(Optional.of(child));

        mockMvc.perform(get("/child/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getById_notFound_returns404() throws Exception {
        given(children.findById(any(UUID.class))).willReturn(Optional.empty());

        mockMvc.perform(get("/child/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ── GET /child/search ─────────────────────────────────────────────

    @Test
    @WithMockUser
    void search_returnsPage() throws Exception {
        Child child = Child.create("Alice", "Doe", LocalDate.of(2011, 6, 1), Gender.FEMALE);
        given(children.findByLastNameStartingWith(eq("Doe"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(child)));

        mockMvc.perform(get("/child/search").param("lastName", "Doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void search_emptyPrefix_returnsAll() throws Exception {
        given(children.findByLastNameStartingWith(anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/child/search"))
                .andExpect(status().isOk());
    }
}