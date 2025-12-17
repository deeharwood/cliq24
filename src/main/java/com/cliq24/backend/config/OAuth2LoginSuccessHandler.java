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

    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;

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

        // Set JWT as HttpOnly cookie (works on iOS)
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("cliq24_jwt", jwtToken);
        cookie.setHttpOnly(true); // JavaScript can't access (more secure)
        cookie.setSecure(cookieSecure); // true in production HTTPS, false in local HTTP dev
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 24 hours
        response.addCookie(cookie);

        logger.info("JWT cookie set (secure={}) redirecting to /", cookieSecure);
        // Redirect to dashboard (no token in URL needed, it's in cookie)
        getRedirectStrategy().sendRedirect(request, response, "/");
    }

    private User createOrUpdateUser(String googleId, String email, String name, String picture) {
        logger.debug("Creating or updating user: {}", email);

        return userRepository.findByGoogleId(googleId)
                .map(existingUser -> {
                    logger.info("Updating existing user: {}", email);
                    existingUser.setEmail(email);
                    existingUser.setName(name);

                    // Only update picture from Google if user hasn't uploaded a custom picture
                    // Custom pictures: data URLs (data:) or local files (/uploads/)
                    String currentPicture = existingUser.getPicture();
                    boolean hasCustomPicture = currentPicture != null &&
                        (currentPicture.startsWith("data:") || currentPicture.startsWith("/uploads/"));

                    if (!hasCustomPicture) {
                        // User hasn't uploaded a custom picture, use Google's
                        existingUser.setPicture(picture);
                        logger.info("Using Google profile picture for user: {}", email);
                    } else {
                        // User has a custom uploaded picture, keep it
                        logger.info("Preserving custom profile picture (type: {}) for user: {}",
                            currentPicture.startsWith("data:") ? "data URL" : "file", email);
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
