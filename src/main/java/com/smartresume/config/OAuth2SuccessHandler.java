package com.smartresume.config;

import com.smartresume.model.User;
import com.smartresume.security.JwtUtil;
import com.smartresume.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;

import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        log.info("OAuth2 Success Handler triggered!");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract user info from Google
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // Get userType from original request (if available)
        String userType = request.getParameter("userType");

        // Check if user exists, create if not
        User user = userService.findByEmail(email);

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setPassword(""); // OAuth users don't have passwords

            // Set role based on userType
            if ("recruiter".equalsIgnoreCase(userType)) {
                user.setRole("ROLE_RECRUITER");
            } else {
                user.setRole("ROLE_USER");
            }

            user = userService.register(user);
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());

        // Determine role for frontend
        String role = user.getRole().equals("ROLE_RECRUITER") ? "recruiter" : "candidate";

        // Redirect to HTTPS success page with token
        String redirectUrl = String.format(
                "https://www.talentsynctech.in/oauth-success.html?token=%s&name=%s&email=%s&role=%s",
                URLEncoder.encode(token, "UTF-8"),
                URLEncoder.encode(user.getName(), "UTF-8"),
                URLEncoder.encode(user.getEmail(), "UTF-8"),
                role);

        response.sendRedirect(redirectUrl);
    }
}
