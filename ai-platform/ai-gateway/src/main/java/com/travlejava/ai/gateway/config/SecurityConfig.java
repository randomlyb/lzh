package com.travlejava.ai.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable) // Disable CSRF for stateless API
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/auth/**").permitAll() // Allow auth service paths
                .pathMatchers("/swagger/**", "/v3/api-docs/**").permitAll() // Allow swagger paths
                .anyExchange().authenticated() // All other requests require authentication
            );
        return http.build();
    }
}
