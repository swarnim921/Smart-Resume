package com.smartresume.config;

import com.smartresume.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtFilter jwtFilter;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;
        private final CookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

                http.csrf(csrf -> csrf.disable())
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth

                                                // PUBLIC ENDPOINTS
                                                .requestMatchers(
                                                                "/actuator/**",
                                                                "/api/auth/**",
                                                                "/api/oauth2/**",
                                                                "/oauth2/**",
                                                                "/login/oauth2/**",

                                                                // Static files - using valid patterns
                                                                "/*.html",
                                                                "/*.css",
                                                                "/*.js",
                                                                "/*.png",
                                                                "/*.jpg",
                                                                "/*.svg",
                                                                "/*.ico",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/images/**",

                                                                "/api/admin/create-initial",
                                                                "/error")
                                                .permitAll()

                                                // ADMIN PROTECTED ENDPOINTS
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                                                // ALL OTHER SECURED ENDPOINTS
                                                .anyRequest().authenticated())
                                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(authorization -> authorization
                                                                .baseUri("/oauth2/authorization")
                                                                .authorizationRequestRepository(
                                                                                cookieAuthorizationRequestRepository))
                                                .redirectionEndpoint(redirection -> redirection
                                                                .baseUri("/login/oauth2/code/*"))
                                                .successHandler(oAuth2SuccessHandler))
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Allow HTTPS origins for production
                configuration.setAllowedOrigins(java.util.Arrays.asList(
                                "https://www.talentsynctech.in",
                                "https://talentsynctech.in",
                                "https://api.talentsynctech.in", // API subdomain for cross-origin requests
                                "http://localhost:3000" // For local development
                ));

                configuration.setAllowedMethods(java.util.Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

                configuration.setAllowedHeaders(java.util.Arrays.asList("*"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        // @Bean
        // public AuthenticationManager
        // authenticationManager(AuthenticationConfiguration config) throws Exception {
        // return config.getAuthenticationManager();
        // }

}
