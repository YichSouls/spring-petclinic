package org.springframework.samples.weightmonitor.security;

import java.io.IOException;

import org.springframework.samples.weightmonitor.provider.HealthcareProviderRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final HealthcareProviderRepository providers;

    public JwtAuthFilter(JwtService jwtService, HealthcareProviderRepository providers) {
        this.jwtService = jwtService;
        this.providers = providers;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // 去掉 "Bearer "
        String username = jwtService.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails user = providers.findByProfessionalId(username).orElseThrow();
            if (jwtService.isTokenValid(token, user)) {
                // 把认证信息放入 Security Context
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null,
                        user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
