package com.cliq24.backend.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cliq24.backend.platforms.LinkedInService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/linkedin")
@CrossOrigin(origins = {"http://localhost:3000", "https://localhost:3000", "https://localhost:8443", "https://cliq24.app"})
public class LinkedInController {

    private static final Logger logger = LogManager.getLogger(LinkedInController.class);

    private final LinkedInService linkedInService;

    @Autowired
    public LinkedInController(LinkedInService linkedInService) {
        this.linkedInService = linkedInService;
    }

    /**
     * Get LinkedIn profile information
     * GET /api/linkedin/{accountId}/profile
     */
    @GetMapping("/{accountId}/profile")
    public ResponseEntity<?> getProfile(@PathVariable String accountId) {
        try {
            logger.info("Getting profile for LinkedIn account: {}", accountId);

            // Get userId from SecurityContext
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Unauthorized", "message", "Please login first"));
            }

            String userId = auth.getName();

            // Get LinkedIn profile
            Map<String, Object> profile = linkedInService.getProfile(userId, accountId);

            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            logger.error("Failed to get profile: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get profile", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error getting profile: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Server error", "message", e.getMessage()));
        }
    }

    /**
     * Get posts from LinkedIn (company pages only)
     * GET /api/linkedin/{accountId}/posts
     */
    @GetMapping("/{accountId}/posts")
    public ResponseEntity<?> getPosts(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            logger.info("Getting posts for LinkedIn account: {}", accountId);

            // Get userId from SecurityContext
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Unauthorized", "message", "Please login first"));
            }

            String userId = auth.getName();
            List<Map<String, Object>> posts = linkedInService.getPosts(userId, accountId, limit);

            return ResponseEntity.ok(posts);
        } catch (RuntimeException e) {
            logger.error("Failed to get posts: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get posts", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error getting posts: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Server error", "message", e.getMessage()));
        }
    }

    /**
     * Update manual metrics for personal LinkedIn accounts
     * POST /api/linkedin/{accountId}/manual-metrics
     * Body: { "connections": 500, "posts": 25, "pendingResponses": 3, "newMessages": 10 }
     */
    @PostMapping("/{accountId}/manual-metrics")
    public ResponseEntity<?> updateManualMetrics(
            @PathVariable String accountId,
            @RequestBody Map<String, Integer> metrics) {
        try {
            logger.info("Updating manual metrics for LinkedIn account: {}", accountId);

            // Get userId from SecurityContext
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Unauthorized", "message", "Please login first"));
            }

            String userId = auth.getName();

            // Validate metrics
            if (metrics == null || metrics.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid request", "message", "metrics are required"));
            }

            // Update manual metrics
            linkedInService.updateManualMetrics(userId, accountId, metrics);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Manual metrics updated successfully"
            ));
        } catch (RuntimeException e) {
            logger.error("Failed to update manual metrics: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to update metrics", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating manual metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Server error", "message", e.getMessage()));
        }
    }
}
