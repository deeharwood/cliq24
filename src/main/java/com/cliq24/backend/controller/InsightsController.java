package com.cliq24.backend.controller;

import com.cliq24.backend.model.SocialAccount;
import com.cliq24.backend.repository.SocialAccountRepository;
import com.cliq24.backend.service.AIInsightsService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "https://localhost:8443", "https://cliq24.app"})
public class InsightsController {

    private static final Logger logger = LogManager.getLogger(InsightsController.class);

    private final AIInsightsService aiInsightsService;
    private final SocialAccountRepository socialAccountRepository;

    @Autowired
    public InsightsController(AIInsightsService aiInsightsService,
                             SocialAccountRepository socialAccountRepository) {
        this.aiInsightsService = aiInsightsService;
        this.socialAccountRepository = socialAccountRepository;
    }

    /**
     * Get AI-generated insights for a specific social account
     * Usage: GET /api/insights/{accountId}
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<?> getInsights(@PathVariable String accountId) {
        try {
            String userId = getCurrentUserId();

            // Get the social account
            SocialAccount account = socialAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

            // Verify the account belongs to the user
            if (!account.getUserId().equals(userId)) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied"));
            }

            // Generate insights
            String insight = aiInsightsService.generateInsights(userId, account);

            return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "platform", account.getPlatform(),
                "insight", insight
            ));

        } catch (Exception e) {
            logger.error("Error getting insights for account {}: {}", accountId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Refresh insights for an account (clears cache and regenerates)
     * Usage: POST /api/insights/{accountId}/refresh
     */
    @PostMapping("/{accountId}/refresh")
    public ResponseEntity<?> refreshInsights(@PathVariable String accountId) {
        try {
            String userId = getCurrentUserId();

            // Get the social account
            SocialAccount account = socialAccountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

            // Verify the account belongs to the user
            if (!account.getUserId().equals(userId)) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied"));
            }

            // Clear cache and regenerate
            aiInsightsService.clearCache(userId, accountId);
            String insight = aiInsightsService.generateInsights(userId, account);

            logger.info("Refreshed insights for account: {}", accountId);

            return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "platform", account.getPlatform(),
                "insight", insight,
                "refreshed", true
            ));

        } catch (Exception e) {
            logger.error("Error refreshing insights for account {}: {}", accountId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
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
