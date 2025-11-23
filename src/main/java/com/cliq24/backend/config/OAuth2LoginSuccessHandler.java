package com.cliq24.backend.config;

import com.cliq24.backend.model.User;
import com.cliq24.backend.repository.UserRepository;
import com.cliq24.backend.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LogManager.getLogger(OAuth2LoginSuccessHandler.class);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${cors.allowed.origins:http://localhost:3000}")
    private String frontendUrl;

    @Autowired
    public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        logger.info("OAuth2 login successful");

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract user information from OAuth2User
        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        logger.info("User logged in: {}", email);

        // Create or update user in database
        User user = createOrUpdateUser(googleId, email, name, picture);

        // Generate JWT token
        String jwtToken = jwtUtil.generateToken(user.getId(), user.getEmail());

        // Redirect to dashboard with token (frontend is on same server)
        String redirectUrl = UriComponentsBuilder.fromUriString("/")
                .queryParam("token", jwtToken)
                .build()
                .toUriString();

        logger.info("Redirecting to: {}", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private User createOrUpdateUser(String googleId, String email, String name, String picture) {
        logger.debug("Creating or updating user: {}", email);

        return userRepository.findByGoogleId(googleId)
                .map(existingUser -> {
                    logger.info("Updating existing user: {}", email);
                    existingUser.setEmail(email);
                    existingUser.setName(name);

                    // Only update picture from Google if user hasn't uploaded a custom picture
                    // Custom pictures start with "/uploads/profile-pictures/"
                    String currentPicture = existingUser.getPicture();
                    if (currentPicture == null || !currentPicture.startsWith("/uploads/profile-pictures/")) {
                        // User hasn't uploaded a custom picture, use Google's
                        existingUser.setPicture(picture);
                        logger.info("Using Google profile picture for user: {}", email);
                    } else {
                        // User has a custom uploaded picture, keep it
                        logger.info("Preserving custom profile picture for user: {}", email);
                    }

                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    logger.info("Creating new user: {}", email);
                    User newUser = new User();
                    newUser.setGoogleId(googleId);
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setPicture(picture);
                    newUser.setCreatedAt(LocalDateTime.now());
                    return userRepository.save(newUser);
                });
    }
}
