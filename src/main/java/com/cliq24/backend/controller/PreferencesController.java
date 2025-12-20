package com.cliq24.backend.controller;

import com.cliq24.backend.dto.AvailableGoalsDTO;
import com.cliq24.backend.dto.PlatformGoalsDTO;
import com.cliq24.backend.service.PreferencesService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/preferences")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "https://localhost:8443", "https://cliq24.app"})
public class PreferencesController {

    private static final Logger logger = LogManager.getLogger(PreferencesController.class);

    private final PreferencesService preferencesService;

    @Autowired
    public PreferencesController(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    /**
     * Get available goal options with descriptions
     * Usage: GET /api/preferences/available-goals
     */
    @GetMapping("/available-goals")
    public ResponseEntity<AvailableGoalsDTO> getAvailableGoals() {
        List<AvailableGoalsDTO.GoalOption> goals = Arrays.asList(
            new AvailableGoalsDTO.GoalOption(
                "growth",
                "Grow My Audience",
                "Focus on increasing followers, reach, and impressions",
                "📈"
            ),
            new AvailableGoalsDTO.GoalOption(
                "engagement",
                "Increase Engagement",
                "Boost likes, comments, shares, and interaction rates",
                "💬"
            ),
            new AvailableGoalsDTO.GoalOption(
                "traffic",
                "Drive Traffic",
                "Maximize clicks, website visits, and conversions",
                "🔗"
            ),
            new AvailableGoalsDTO.GoalOption(
                "response",
                "Improve Response Time",
                "Track and respond to messages and comments quickly",
                "⚡"
            ),
            new AvailableGoalsDTO.GoalOption(
                "content",
                "Track Content Performance",
                "Analyze which posts perform best and when to post",
                "📊"
            ),
            new AvailableGoalsDTO.GoalOption(
                "comprehensive",
                "All of the Above",
                "Show me everything - comprehensive dashboard view",
                "🎯"
            )
        );

        return ResponseEntity.ok(new AvailableGoalsDTO(goals));
    }

    /**
     * Get all platform goals for current user
     * Usage: GET /api/preferences
     */
    @GetMapping
    public ResponseEntity<?> getAllPreferences() {
        try {
            String userId = getCurrentUserId();
            Map<String, List<String>> platformGoals = preferencesService.getAllPlatformGoals(userId);
            return ResponseEntity.ok(new PlatformGoalsDTO(platformGoals));
        } catch (Exception e) {
            logger.error("Error getting preferences", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update all platform goals
     * Usage: PUT /api/preferences
     * Body: { "platformGoals": { "facebook": ["growth", "engagement"], "instagram": ["content"] } }
     */
    @PutMapping
    public ResponseEntity<?> updateAllPreferences(@RequestBody PlatformGoalsDTO request) {
        try {
            String userId = getCurrentUserId();
            preferencesService.setAllPlatformGoals(userId, request.getPlatformGoals());
            return ResponseEntity.ok(Map.of("message", "Preferences updated successfully"));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid goals provided", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating preferences", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get goals for specific platform
     * Usage: GET /api/preferences/facebook
     */
    @GetMapping("/{platform}")
    public ResponseEntity<?> getPlatformPreferences(@PathVariable String platform) {
        try {
            String userId = getCurrentUserId();
            List<String> goals = preferencesService.getPlatformGoals(userId, platform);
            return ResponseEntity.ok(Map.of("platform", platform.toLowerCase(), "goals", goals));
        } catch (Exception e) {
            logger.error("Error getting platform preferences", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update goals for specific platform
     * Usage: PUT /api/preferences/facebook
     * Body: { "goals": ["growth", "engagement"] }
     */
    @PutMapping("/{platform}")
    public ResponseEntity<?> updatePlatformPreferences(
            @PathVariable String platform,
            @RequestBody Map<String, List<String>> request) {
        try {
            String userId = getCurrentUserId();
            List<String> goals = request.get("goals");

            if (goals == null || goals.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Goals list cannot be empty"));
            }

            preferencesService.setPlatformGoals(userId, platform, goals);
            return ResponseEntity.ok(Map.of(
                "message", "Goals updated for " + platform,
                "platform", platform.toLowerCase(),
                "goals", goals
            ));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid goals provided", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating platform preferences", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Helper method to get current user ID from security context
     */
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new RuntimeException("User not authenticated");
        }
        return auth.getName();
    }
}
