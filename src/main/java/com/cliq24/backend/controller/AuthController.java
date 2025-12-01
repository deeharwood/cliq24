package com.cliq24.backend.controller;

import com.cliq24.backend.dto.LoginRequestDTO;
import com.cliq24.backend.dto.LoginResponseDTO;
import com.cliq24.backend.dto.RegisterRequestDTO;
import com.cliq24.backend.dto.UserDTO;
import com.cliq24.backend.service.AuthService;
import com.cliq24.backend.service.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "https://localhost:8443", "https://cliq24.app"})
public class AuthController {

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
     * Usage: GET /auth/me with Authorization: Bearer <token>
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@RequestHeader("Authorization") String token) {
        UserDTO user = authService.getUserFromToken(token);
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
     * Usage: POST /auth/me/picture/upload with Authorization: Bearer <token>
     * Form Data: file (multipart/form-data)
     */
    @PostMapping("/me/picture/upload")
    public ResponseEntity<UserDTO> uploadProfilePicture(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file) {
        try {
            String userId = authService.validateAndExtractUserId(token);
            String pictureUrl = fileStorageService.storeProfilePicture(file, userId);
            UserDTO updatedUser = authService.updateUserPicture(token, pictureUrl);
            return ResponseEntity.ok(updatedUser);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Register new user with email and password
     * Usage: POST /auth/register
     * Body: { "name": "John Doe", "email": "john@example.com", "password": "password123" }
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            LoginResponseDTO response = authService.registerWithEmail(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Login with email and password
     * Usage: POST /auth/login
     * Body: { "email": "john@example.com", "password": "password123" }
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            LoginResponseDTO response = authService.loginWithEmail(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
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
