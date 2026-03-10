package com.smartresume.config;

import com.smartresume.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Map;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtFilter jwtFilter;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;
        private final CookieOAuth2AuthorizationRequestRepository cookieOAuth2Repository;

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
                                                                .authorizationRequestRepository(cookieOAuth2Repository)
                                                                .authorizationRequestResolver(
                                                                                authorizationRequestResolver(null))) // Will
                                                                                                                     // be
                                                                                                                     // injected
                                                                                                                     // by
                                                                                                                     // Spring
                                                .tokenEndpoint(t -> t
                                                                .accessTokenResponseClient(accessTokenResponseClient()))
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

        @Bean
        public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
                DefaultAuthorizationCodeTokenResponseClient accessTokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();

                OAuth2AccessTokenResponseHttpMessageConverter tokenResponseHttpMessageConverter = new OAuth2AccessTokenResponseHttpMessageConverter();

                // Custom converter to handle LinkedIn's missing token_type field
                tokenResponseHttpMessageConverter.setAccessTokenResponseConverter(source -> {
                        String tokenType = (String) source.get("token_type");
                        if (tokenType == null) {
                                // Default to Bearer if missing (LinkedIn often omits this)
                                tokenType = OAuth2AccessToken.TokenType.BEARER.getValue();
                        }

                        Object scopeObj = source.get("scope");
                        java.util.Set<String> scopes = java.util.Collections.emptySet();
                        if (scopeObj instanceof java.util.Collection) {
                                @SuppressWarnings("unchecked")
                                java.util.Collection<String> scopeColl = (java.util.Collection<String>) scopeObj;
                                scopes = new java.util.HashSet<>(scopeColl);
                        } else if (scopeObj instanceof String) {
                                scopes = new java.util.HashSet<>(
                                                java.util.Arrays.asList(((String) scopeObj).split(" ")));
                        }

                        return OAuth2AccessTokenResponse.withToken((String) source.get("access_token"))
                                        .tokenType(OAuth2AccessToken.TokenType.BEARER) // Force Bearer
                                        .expiresIn(Long.parseLong(String.valueOf(source.get("expires_in"))))
                                        .scopes(scopes)
                                        .additionalParameters((Map<String, Object>) source)
                                        .build();
                });

                RestTemplate restTemplate = new RestTemplate(java.util.Arrays.asList(
                                new org.springframework.http.converter.FormHttpMessageConverter(),
                                tokenResponseHttpMessageConverter));

                restTemplate.setErrorHandler(
                                new org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler());

                accessTokenResponseClient.setRestOperations(restTemplate);
                return accessTokenResponseClient;
        }

        @Bean
        public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
                        @Lazy ClientRegistrationRepository repo) {
                return new CustomOAuth2AuthorizationRequestResolver(repo);
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
