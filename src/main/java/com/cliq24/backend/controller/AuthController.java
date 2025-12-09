package com.cliq24.backend.controller;

import com.cliq24.backend.dto.LoginRequestDTO;
import com.cliq24.backend.dto.LoginResponseDTO;
import com.cliq24.backend.dto.RegisterRequestDTO;
import com.cliq24.backend.dto.UserDTO;
import com.cliq24.backend.service.AuthService;
import com.cliq24.backend.service.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "https://localhost:8443", "https://cliq24.app"})
public class AuthController {

    private static final Logger logger = LogManager.getLogger(AuthController.class);

    private final AuthService authService;
    private final FileStorageService fileStorageService;

    @Autowired
    public AuthController(AuthService authService, FileStorageService fileStorageService) {
        this.authService = authService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Redirect to Google OAuth login
     * Usage: http://localhost:8080/auth/google
     */
    @GetMapping("/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        // Redirect to Spring Security's OAuth2 endpoint
        response.sendRedirect("/oauth2/authorization/google");
    }

    /**
     * Get current user information
     * Usage: GET /auth/me (authenticated via cookie or Authorization header)
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        // Get userId from SecurityContext (set by JWT filter from cookie or header)
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).build();
        }

        String userId = auth.getName();
        UserDTO user = authService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Update user profile picture with URL
     * Usage: PUT /auth/me/picture with Authorization: Bearer <token>
     * Body: { "pictureUrl": "https://..." }
     */
    @PutMapping("/me/picture")
    public ResponseEntity<UserDTO> updateProfilePicture(
            @RequestHeader("Authorization") String token,
            @RequestBody java.util.Map<String, String> body) {
        String pictureUrl = body.get("pictureUrl");
        UserDTO updatedUser = authService.updateUserPicture(token, pictureUrl);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Upload user profile picture file
     * Usage: POST /auth/me/picture/upload (authenticated via cookie or Authorization header)
     * Form Data: file (multipart/form-data)
     */
    @PostMapping("/me/picture/upload")
    public ResponseEntity<?> uploadProfilePicture(
            @RequestParam("file") MultipartFile file) {
        try {
            logger.info("Upload request received - file: {}, size: {} bytes",
                file.getOriginalFilename(), file.getSize());

            // Get userId from SecurityContext (set by JWT filter from cookie or header)
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.warn("Unauthorized upload attempt");
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Unauthorized", "message", "Please login first"));
            }

            String userId = auth.getName();
            logger.info("Upload for user: {}", userId);

            String pictureUrl = fileStorageService.storeProfilePicture(file, userId);

            // Update user in database
            authService.updateUserPictureById(userId, pictureUrl);

            // Get updated user
            UserDTO updatedUser = authService.getUserById(userId);

            logger.info("Profile picture uploaded successfully for user: {}", userId);
            return ResponseEntity.ok(updatedUser);
        } catch (IOException e) {
            logger.error("File upload failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "File upload failed", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during upload: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Server error", "message", e.getMessage()));
        }
    }

    /**
     * Register new user with email and password
     * Usage: POST /auth/register
     * Body: { "name": "John Doe", "email": "john@example.com", "password": "password123" }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            LoginResponseDTO response = authService.registerWithEmail(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    /**
     * Login with email and password
     * Usage: POST /auth/login
     * Body: { "email": "john@example.com", "password": "password123" }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            LoginResponseDTO response = authService.loginWithEmail(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
