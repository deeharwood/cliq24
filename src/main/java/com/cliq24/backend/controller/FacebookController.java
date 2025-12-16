package com.cliq24.backend.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cliq24.backend.platforms.FacebookService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/facebook")
@CrossOrigin(origins = {"http://localhost:3000", "https://localhost:3000", "https://localhost:8443", "https://cliq24.app"})
public class FacebookController {

    private static final Logger logger = LogManager.getLogger(FacebookController.class);

    private final FacebookService facebookService;

    @Autowired
    public FacebookController(FacebookService facebookService) {
        this.facebookService = facebookService;
    }

    /**
     * Get recent messages for a Facebook account
     * GET /api/facebook/{accountId}/messages
     */
    @GetMapping("/{accountId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable String accountId) {
        try {
            logger.info("Getting messages for Facebook account: {}", accountId);

            // Get userId from SecurityContext
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Unauthorized", "message", "Please login first"));
            }

            String userId = auth.getName();

            // Verify the account belongs to this user and get messages
            List<Map<String, Object>> messages = facebookService.getRecentMessages(userId, accountId);

            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            logger.error("Failed to get messages: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get messages", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error getting messages: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Server error", "message", e.getMessage()));
        }
    }

    /**
     * Send a message via Facebook
     * POST /api/facebook/{accountId}/messages/send
     * Body: { "recipientId": "...", "message": "..." }
     */
    @PostMapping("/{accountId}/messages/send")
    public ResponseEntity<?> sendMessage(
            @PathVariable String accountId,
            @RequestBody Map<String, String> request) {
        try {
            logger.info("Sending message from Facebook account: {}", accountId);

            // Get userId from SecurityContext
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Unauthorized", "message", "Please login first"));
            }

            String userId = auth.getName();
            String recipientId = request.get("recipientId");
            String message = request.get("message");

            if (recipientId == null || recipientId.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid request", "message", "recipientId is required"));
            }

            if (message == null || message.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid request", "message", "message is required"));
            }

            // Send the message
            Map<String, Object> result = facebookService.sendMessage(userId, accountId, recipientId, message);

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            logger.error("Failed to send message: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to send message", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error sending message: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Server error", "message", e.getMessage()));
        }
    }

    /**
     * Get posts from Facebook Page
     * GET /api/facebook/{accountId}/posts
     */
    @GetMapping("/{accountId}/posts")
    public ResponseEntity<?> getPosts(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            logger.info("Getting posts for Facebook account: {}", accountId);

            // Get userId from SecurityContext
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Unauthorized", "message", "Please login first"));
            }

            String userId = auth.getName();
            List<Map<String, Object>> posts = facebookService.getPosts(userId, accountId, limit);

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
     * Get photos from Facebook Page
     * GET /api/facebook/{accountId}/photos
     */
    @GetMapping("/{accountId}/photos")
    public ResponseEntity<?> getPhotos(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            logger.info("Getting photos for Facebook account: {}", accountId);

            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Unauthorized", "message", "Please login first"));
            }

            String userId = auth.getName();
            List<Map<String, Object>> photos = facebookService.getPhotos(userId, accountId, limit);

            return ResponseEntity.ok(photos);
        } catch (RuntimeException e) {
            logger.error("Failed to get photos: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get photos", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error getting photos: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Server error", "message", e.getMessage()));
        }
    }

    /**
     * Get videos from Facebook Page
     * GET /api/facebook/{accountId}/videos
     */
    @GetMapping("/{accountId}/videos")
    public ResponseEntity<?> getVideos(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            logger.info("Getting videos for Facebook account: {}", accountId);

            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Unauthorized", "message", "Please login first"));
            }

            String userId = auth.getName();
            List<Map<String, Object>> videos = facebookService.getVideos(userId, accountId, limit);

            return ResponseEntity.ok(videos);
        } catch (RuntimeException e) {
            logger.error("Failed to get videos: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get videos", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error getting videos: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Server error", "message", e.getMessage()));
        }
    }
}
