package com.smartresume.config;

import com.smartresume.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

        @Autowired
        private JwtFilter jwtFilter;
        @Autowired
        private OAuth2SuccessHandler oAuth2SuccessHandler;
        @Autowired
        private CookieOAuth2AuthorizationRequestRepository cookieOAuth2Repository;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http,
                        ClientRegistrationRepository clientRegistrationRepository) throws Exception {



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
                                                                "/",

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
                                                .authorizationEndpoint(a -> a
                                                                .authorizationRequestRepository(cookieOAuth2Repository))
                                                .successHandler(oAuth2SuccessHandler)
                                                .failureUrl("/api/oauth2/failure"))
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
                                "https://api.talentsynctech.in",
                                "http://localhost:3000",
                                "http://localhost:8080"));

                configuration.setAllowedMethods(java.util.Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

                configuration.setAllowedHeaders(java.util.Arrays.asList("*"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}
