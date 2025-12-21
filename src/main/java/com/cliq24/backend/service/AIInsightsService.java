package com.cliq24.backend.service;

import com.cliq24.backend.model.SocialAccount;
import com.cliq24.backend.model.User;
import com.cliq24.backend.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIInsightsService {

    private static final Logger logger = LogManager.getLogger(AIInsightsService.class);

    @Value("${claude.api.key:placeholder}")
    private String claudeApiKey;

    @Value("${claude.api.url:https://api.anthropic.com/v1/messages}")
    private String claudeApiUrl;

    private final PreferencesService preferencesService;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    // Cache insights for 1 hour to avoid excessive API calls
    private final Map<String, CachedInsight> insightsCache = new HashMap<>();
    private static final long CACHE_DURATION_MS = 60 * 60 * 1000; // 1 hour

    @Autowired
    public AIInsightsService(PreferencesService preferencesService,
                             UserRepository userRepository) {
        this.preferencesService = preferencesService;
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Generate AI insights for a social account based on metrics and user goals
     */
    public String generateInsights(String userId, SocialAccount account) {
        // Check if Claude API is configured
        if (claudeApiKey == null || claudeApiKey.equals("placeholder") || claudeApiKey.isEmpty()) {
            logger.warn("Claude API not configured - returning placeholder insights");
            return getPlaceholderInsight(account);
        }

        // Check cache first
        String cacheKey = userId + ":" + account.getId();
        CachedInsight cached = insightsCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            logger.info("Returning cached insight for account: {}", account.getId());
            return cached.insight;
        }

        try {
            // Get user's goals for this platform
            List<String> goals = preferencesService.getPlatformGoals(userId, account.getPlatform());

            // Build prompt for Claude
            String prompt = buildPrompt(account, goals);

            // Call Claude API
            String insight = callClaudeAPI(prompt);

            // Cache the result
            insightsCache.put(cacheKey, new CachedInsight(insight));

            logger.info("Generated new AI insight for account: {}", account.getId());
            return insight;

        } catch (Exception e) {
            logger.error("Failed to generate AI insights: {}", e.getMessage(), e);
            return getPlaceholderInsight(account);
        }
    }

    /**
     * Build prompt for Claude API based on account metrics and user goals
     */
    private String buildPrompt(SocialAccount account, List<String> goals) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a social media marketing expert. Analyze the following ");
        prompt.append(account.getPlatform());
        prompt.append(" account metrics and provide a brief, actionable insight.\n\n");

        prompt.append("Account: @").append(account.getUsername()).append("\n");
        prompt.append("Platform: ").append(account.getPlatform()).append("\n\n");

        prompt.append("Current Metrics:\n");
        if (account.getMetrics() != null) {
            prompt.append("- Followers: ").append(account.getMetrics().getConnections()).append("\n");
            prompt.append("- Posts: ").append(account.getMetrics().getPosts()).append("\n");
            prompt.append("- Pending Responses: ").append(account.getMetrics().getPendingResponses()).append("\n");
            prompt.append("- Engagement Score: ").append(account.getMetrics().getEngagementScore()).append("/100\n");
        }

        prompt.append("\nUser's Goals: ");
        prompt.append(String.join(", ", goals));
        prompt.append("\n\n");

        prompt.append("Provide ONE specific, actionable insight (2-3 sentences max) that helps achieve their goals. ");
        prompt.append("Focus on what they should DO next. Be encouraging but practical. ");
        prompt.append("Do not use emojis or markdown formatting.");

        return prompt.toString();
    }

    /**
     * Call Claude API to generate insight
     */
    private String callClaudeAPI(String prompt) {
        try {
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "claude-3-5-sonnet-20240620");
            requestBody.put("max_tokens", 150);

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", List.of(message));

            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("x-api-key", claudeApiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Make API call
            ResponseEntity<Map> response = restTemplate.exchange(
                claudeApiUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );

            // Parse response
            if (response.getBody() != null && response.getBody().containsKey("content")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
                if (!content.isEmpty() && content.get(0).containsKey("text")) {
                    return (String) content.get(0).get("text");
                }
            }

            throw new RuntimeException("Invalid response from Claude API");

        } catch (Exception e) {
            logger.error("Error calling Claude API: {}", e.getMessage());
            throw new RuntimeException("Failed to call Claude API", e);
        }
    }

    /**
     * Get placeholder insight when AI is not available
     */
    private String getPlaceholderInsight(SocialAccount account) {
        int score = account.getMetrics() != null ? account.getMetrics().getEngagementScore() : 0;
        int pending = account.getMetrics() != null ? account.getMetrics().getPendingResponses() : 0;

        if (pending > 0) {
            return "You have " + pending + " pending response" + (pending > 1 ? "s" : "") +
                   ". Quick responses help maintain engagement. Try to reply within 24 hours for best results.";
        }

        if (score >= 80) {
            return "Your engagement is excellent! Maintain consistency by posting regularly and interacting with your audience daily.";
        } else if (score >= 60) {
            return "Good engagement! Try posting 2-3 times per week and respond to comments quickly to boost your score.";
        } else if (score >= 40) {
            return "Your account needs attention. Focus on posting quality content consistently and engaging with your audience.";
        } else {
            return "Time to revitalize your presence! Start by posting valuable content this week and responding to all comments.";
        }
    }

    /**
     * Clear cached insights (useful for forcing refresh)
     */
    public void clearCache(String userId, String accountId) {
        String cacheKey = userId + ":" + accountId;
        insightsCache.remove(cacheKey);
        logger.info("Cleared insights cache for account: {}", accountId);
    }

    /**
     * Clear all cached insights for a user
     */
    public void clearUserCache(String userId) {
        insightsCache.keySet().removeIf(key -> key.startsWith(userId + ":"));
        logger.info("Cleared all insights cache for user: {}", userId);
    }

    /**
     * Inner class for caching insights with expiration
     */
    private static class CachedInsight {
        private final String insight;
        private final long timestamp;

        public CachedInsight(String insight) {
            this.insight = insight;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }
}
