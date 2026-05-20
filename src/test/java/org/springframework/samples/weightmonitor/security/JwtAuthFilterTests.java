package org.springframework.samples.weightmonitor.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.samples.weightmonitor.provider.HealthcareProvider;
import org.springframework.samples.weightmonitor.provider.HealthcareProviderRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTests {

    @Mock
    private JwtService jwtService;

    @Mock
    private HealthcareProviderRepository providers;

    @InjectMocks
    private JwtAuthFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── No / invalid Authorization header ────────────────────────────

    @Test
    void noAuthorizationHeader_passesThrough_noAuthentication() throws Exception {
        given(request.getHeader("Authorization")).willReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    void authorizationHeaderNotBearer_passesThrough() throws Exception {
        given(request.getHeader("Authorization")).willReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(anyString());
    }

    // ── Valid token ───────────────────────────────────────────────────

    @Test
    void validToken_setsAuthenticationInContext() throws Exception {
        HealthcareProvider provider = HealthcareProvider.create(
                "doc001", "encoded", "John", "Doe");
        given(request.getHeader("Authorization")).willReturn("Bearer valid.token.here");
        given(jwtService.extractUsername("valid.token.here")).willReturn("doc001");
        given(providers.findByProfessionalId("doc001")).willReturn(Optional.of(provider));
        given(jwtService.isTokenValid("valid.token.here", provider)).willReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("doc001");
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidToken_noAuthenticationSet_chainStillCalled() throws Exception {
        HealthcareProvider provider = HealthcareProvider.create(
                "doc001", "encoded", "John", "Doe");
        given(request.getHeader("Authorization")).willReturn("Bearer expired.token");
        given(jwtService.extractUsername("expired.token")).willReturn("doc001");
        given(providers.findByProfessionalId("doc001")).willReturn(Optional.of(provider));
        given(jwtService.isTokenValid("expired.token", provider)).willReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response); // chain still called
    }

    // ── Edge cases ────────────────────────────────────────────────────

    @Test
    void nullUsername_skipsAuthLookup_chainCalled() throws Exception {
        given(request.getHeader("Authorization")).willReturn("Bearer some.token");
        given(jwtService.extractUsername("some.token")).willReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(providers, never()).findByProfessionalId(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void alreadyAuthenticated_skipsTokenValidation() throws Exception {
        // pre-populate SecurityContext with existing auth
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "existing-user", null,
                        List.of(new SimpleGrantedAuthority("ROLE_PROVIDER"))));

        given(request.getHeader("Authorization")).willReturn("Bearer some.token");
        given(jwtService.extractUsername("some.token")).willReturn("doc001");

        filter.doFilterInternal(request, response, chain);

        verify(providers, never()).findByProfessionalId(any());
        verify(chain).doFilter(request, response);
    }
}