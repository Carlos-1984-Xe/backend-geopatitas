package com.geopatitas.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow public access to GET requests for pets
                .requestMatchers(HttpMethod.GET, "/api/v1/pets/**").permitAll()
                // Protect pet creation, update, deletion
                .requestMatchers("/api/v1/pets/**").authenticated()
                // Protect user and matching endpoints by default
                .requestMatchers("/api/v1/users/**").authenticated()
                .requestMatchers("/api/v1/matching/**").authenticated()
                // Require authentication for any other request
                .anyRequest().authenticated()
            )
            // For MVP purposes, using Basic Auth or setting up JWT later.
            // Replace with OAuth2 resource server or custom JWT filter later.
            .httpBasic(basic -> {}); 

        return http.build();
    }
}
