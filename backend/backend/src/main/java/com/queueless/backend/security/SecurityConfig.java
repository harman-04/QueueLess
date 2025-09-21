package com.queueless.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://localhost:5173", "https://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "X-Requested-With", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                // 🆕 Update headers configuration with the new policies
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; img-src 'self' data: https:; font-src 'self' https://fonts.gstatic.com https://fonts.googleapis.com;")
                        )
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                        )
                        .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
                )

                // 🟢 Place the rate limit filter at the very start of the chain
                .addFilterBefore(rateLimitFilter, SecurityContextHolderFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public endpoints
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/password/**",
                                "/ws/**",
                                "/api/payment/create-order",
                                "/api/payment/confirm",
                                "/api/payment/confirm-provider",
                                "/api/payment/confirm-provider-bulk",
                                "/api/search/**"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/queues/*/add-token-with-details"
                        ).hasRole("USER")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/queues/*/token/*/user-details"
                        ).hasAnyRole("USER", "PROVIDER", "ADMIN")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/queues/*/reset-with-options"
                        ).hasAnyRole("PROVIDER", "ADMIN")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/user/favorites",
                                "/api/user/favorites/details"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/user/favorites/**"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/user/favorites/**"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/feedback/**"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/feedback"
                        ).hasRole("USER")
                        .requestMatchers("/api/export/**").hasAnyRole("PROVIDER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/exports/**").hasAnyRole("PROVIDER", "ADMIN")
                        // Public read-only endpoints
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/places",
                                "/api/places/{id}",
                                "/api/places/type/**",
                                "/api/places/nearby",
                                "/api/services",
                                "/api/services/{id}",
                                "/api/services/place/**",
                                "/abapi/queues/all",
                                "/api/queues/by-place/**",
                                "/api/queues/by-service/**",
                                "/api/queues/{queueId}"
                        ).permitAll()
                        // User endpoints
                        .requestMatchers("/api/queues/*/add-token").hasRole("USER")
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN", "PROVIDER")
                        // Provider endpoints
                        .requestMatchers("/api/providers/**").hasRole("PROVIDER")
                        .requestMatchers("/api/queues/create").hasRole("PROVIDER")
                        .requestMatchers("/api/queues/by-provider").hasRole("PROVIDER")
                        .requestMatchers("/api/queues/*/serve-next").hasRole("PROVIDER")
                        .requestMatchers("/api/queues/*/complete-token").hasRole("PROVIDER")
                        .requestMatchers("/api/queues/*/activate").hasAnyRole("PROVIDER","ADMIN")
                        .requestMatchers("/api/queues/*/deactivate").hasAnyRole("PROVIDER", "ADMIN")
                        // Admin endpoints - ensure proper authorization
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/places").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/places/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/places/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/services").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/services/**").hasRole("ADMIN")
                        // All other requests need authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}