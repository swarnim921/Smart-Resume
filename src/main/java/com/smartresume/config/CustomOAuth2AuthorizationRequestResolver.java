package com.smartresume.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom OAuth2AuthorizationRequestResolver to handle LinkedIn OIDC nonces.
 * LinkedIn requires a nonce in the authorization request to validate the ID
 * Token (OIDC).
 * Since we don't use issuer-uri (to avoid startup crashes on Render),
 * we must manually add the nonce to the request.
 */
@Component
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;
    private final StringKeyGenerator nonceGenerator = new Base64StringKeyGenerator(
            Base64.getUrlEncoder().withoutPadding(), 16);

    public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository repo) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authRequest = this.defaultResolver.resolve(request);
        return process(authRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authRequest = this.defaultResolver.resolve(request, clientRegistrationId);
        return process(authRequest);
    }

    private OAuth2AuthorizationRequest process(OAuth2AuthorizationRequest authRequest) {
        if (authRequest == null) {
            return null;
        }

        // Only add nonce for LinkedIn (if it hasn't been added already)
        if (authRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID).equals("linkedin")) {
            return addNonce(authRequest);
        }

        return authRequest;
    }

    private OAuth2AuthorizationRequest addNonce(OAuth2AuthorizationRequest authRequest) {
        Map<String, Object> attributes = new HashMap<>(authRequest.getAttributes());
        Map<String, Object> additionalParameters = new HashMap<>(authRequest.getAdditionalParameters());

        // Generate nonce
        String nonce = this.nonceGenerator.generateKey();

        // Standard OIDC nonce attribute (used for validation later)
        attributes.put("nonce", nonce);

        // Add nonce to the URL parameters sent to LinkedIn
        additionalParameters.put("nonce", nonce);

        return OAuth2AuthorizationRequest.from(authRequest)
                .attributes(attributes)
                .additionalParameters(additionalParameters)
                .build();
    }
}
