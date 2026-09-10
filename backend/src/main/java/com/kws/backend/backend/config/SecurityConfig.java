package com.kws.backend.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF if building a stateless API or for local testing
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/api/**", "/css/**", "/js/**").permitAll() // Allow public access to these paths
                        .anyRequest().authenticated() // Require authentication for everything else
                );
        return http.build();
    }
}