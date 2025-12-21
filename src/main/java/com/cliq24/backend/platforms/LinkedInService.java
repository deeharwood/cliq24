package com.cliq24.backend.platforms;

import com.cliq24.backend.model.AccountMetrics;
import com.cliq24.backend.model.SocialAccount;
import com.cliq24.backend.repository.SocialAccountRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class LinkedInService {

    private static final Logger logger = LogManager.getLogger(LinkedInService.class);

    private final SocialAccountRepository socialAccountRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public LinkedInService(SocialAccountRepository socialAccountRepository) {
        this.socialAccountRepository = socialAccountRepository;
        this.restTemplate = new RestTemplate();
    }

    public AccountMetrics syncMetrics(SocialAccount account) {
        String accountType = account.getAccountType();

        if ("company".equals(accountType)) {
            return syncCompanyMetrics(account);
        } else {
            return syncPersonalMetrics(account);
        }
    }

    /**
     * Sync metrics for personal LinkedIn accounts (limited API + manual input)
     */
    private AccountMetrics syncPersonalMetrics(SocialAccount account) {
        logger.info("Syncing personal LinkedIn account: {}", account.getUsername());

        AccountMetrics metrics = new AccountMetrics();
        Map<String, Integer> manualMetrics = account.getManualMetrics();

        if (manualMetrics != null && !manualMetrics.isEmpty()) {
            // Use user-provided manual metrics
            metrics.setConnections(manualMetrics.getOrDefault("connections", 0));
            metrics.setPosts(manualMetrics.getOrDefault("posts", 0));
            metrics.setPendingResponses(manualMetrics.getOrDefault("pendingResponses", 0));
            metrics.setNewMessages(manualMetrics.getOrDefault("newMessages", 0));

            // Calculate engagement score based on manual data
            int engagementScore = calculateEngagementScore(metrics);
            metrics.setEngagementScore(engagementScore);
        } else {
            // No manual metrics provided yet - return zeros
            metrics.setEngagementScore(0);
            metrics.setConnections(0);
            metrics.setPosts(0);
            metrics.setPendingResponses(0);
            metrics.setNewMessages(0);
        }

        logger.info("Personal LinkedIn metrics - Connections: {}, Posts: {}",
            metrics.getConnections(), metrics.getPosts());

        return metrics;
    }

    /**
     * Sync metrics for company LinkedIn pages (real API integration)
     */
    private AccountMetrics syncCompanyMetrics(SocialAccount account) {
        logger.info("Syncing company LinkedIn page: {}", account.getUsername());

        String accessToken = account.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            logger.warn("No access token for LinkedIn company account, using defaults");
            return getDefaultMetrics();
        }

        try {
            // Fetch company page statistics from LinkedIn API
            // Note: This requires r_organization_social scope and company page admin access
            Map<String, Object> stats = getCompanyPageStats(account.getPlatformUserId(), accessToken);

            AccountMetrics metrics = new AccountMetrics();
            metrics.setConnections((Integer) stats.getOrDefault("followerCount", 0));
            metrics.setPosts((Integer) stats.getOrDefault("postCount", 0));
            metrics.setPendingResponses(0); // Not available via API
            metrics.setNewMessages(0); // Not available via API

            // Calculate engagement score
            int engagementScore = calculateEngagementScore(metrics);
            metrics.setEngagementScore(engagementScore);

            logger.info("Company LinkedIn metrics - Followers: {}, Posts: {}",
                metrics.getConnections(), metrics.getPosts());

            return metrics;

        } catch (Exception e) {
            logger.error("Failed to fetch LinkedIn company metrics: {}", e.getMessage(), e);
            return getDefaultMetrics();
        }
    }

    /**
     * Get LinkedIn profile information
     */
    public Map<String, Object> getProfile(String userId, String accountId) {
        logger.debug("Getting LinkedIn profile for account: {}", accountId);

        SocialAccount account = socialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (!"LinkedIn".equals(account.getPlatform())) {
            throw new RuntimeException("This endpoint is only for LinkedIn accounts");
        }

        try {
            String accessToken = account.getAccessToken();
            String apiUrl = "https://api.linkedin.com/v2/me?projection=(id,firstName,lastName,profilePicture(displayImage~:playableStreams))";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            org.springframework.http.HttpEntity<String> entity =
                new org.springframework.http.HttpEntity<>(headers);

            Map response = restTemplate.exchange(
                apiUrl,
                org.springframework.http.HttpMethod.GET,
                entity,
                Map.class
            ).getBody();

            Map<String, Object> profile = new HashMap<>();
            profile.put("id", response.get("id"));
            profile.put("firstName", getLocalizedField(response, "firstName"));
            profile.put("lastName", getLocalizedField(response, "lastName"));

            return profile;

        } catch (Exception e) {
            logger.error("Failed to fetch LinkedIn profile: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch profile");
        }
    }

    /**
     * Get company page statistics (requires company page admin access)
     */
    private Map<String, Object> getCompanyPageStats(String organizationId, String accessToken) {
        // Note: This is a placeholder for LinkedIn Marketing API integration
        // Actual implementation requires LinkedIn Marketing Developer Platform approval

        Map<String, Object> stats = new HashMap<>();
        stats.put("followerCount", 0);
        stats.put("postCount", 0);

        // TODO: Implement when Marketing Developer Platform access is granted
        // String apiUrl = "https://api.linkedin.com/v2/organizationalEntityFollowerStatistics?q=organizationalEntity&organizationalEntity=urn:li:organization:" + organizationId;

        return stats;
    }

    /**
     * Get posts for company pages
     */
    public List<Map<String, Object>> getPosts(String userId, String accountId, int limit) {
        logger.debug("Getting posts for LinkedIn account: {}", accountId);

        SocialAccount account = socialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (!"company".equals(account.getAccountType())) {
            // Personal accounts can't fetch posts via API
            return getMockPosts();
        }

        // TODO: Implement company page posts fetching
        // Requires r_organization_social scope
        return getMockPosts();
    }

    /**
     * Update manual metrics for personal accounts
     */
    public void updateManualMetrics(String userId, String accountId, Map<String, Integer> metrics) {
        logger.info("Updating manual metrics for LinkedIn account: {}", accountId);

        SocialAccount account = socialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (!"LinkedIn".equals(account.getPlatform())) {
            throw new RuntimeException("This endpoint is only for LinkedIn accounts");
        }

        // Update manual metrics
        account.setManualMetrics(metrics);
        account.setLastSynced(LocalDateTime.now());

        // Recalculate and update engagement score
        AccountMetrics accountMetrics = syncPersonalMetrics(account);
        account.setMetrics(accountMetrics);

        socialAccountRepository.save(account);
        logger.info("Manual metrics updated successfully");
    }

    /**
     * Calculate engagement score based on available metrics
     */
    private int calculateEngagementScore(AccountMetrics metrics) {
        int score = 0;

        // Connection count (max 30 points)
        int connections = metrics.getConnections();
        if (connections > 0) {
            score += Math.min(30, (connections / 100));
        }

        // Post activity (max 40 points)
        int posts = metrics.getPosts();
        if (posts > 0) {
            score += Math.min(40, (posts / 5) * 2);
        }

        // Responsiveness (max 30 points)
        int pending = metrics.getPendingResponses();
        if (pending == 0) {
            score += 30; // No pending responses = great
        } else if (pending < 5) {
            score += 20; // Few pending = good
        } else {
            score += 10; // Many pending = needs improvement
        }

        return Math.min(100, score);
    }

    /**
     * Helper to extract localized fields from LinkedIn API response
     */
    private String getLocalizedField(Map response, String fieldName) {
        try {
            Map field = (Map) response.get(fieldName);
            Map localized = (Map) field.get("localized");
            // Get first available localization
            return (String) localized.values().iterator().next();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Default metrics when API fails
     */
    private AccountMetrics getDefaultMetrics() {
        AccountMetrics metrics = new AccountMetrics();
        metrics.setEngagementScore(0);
        metrics.setConnections(0);
        metrics.setPosts(0);
        metrics.setPendingResponses(0);
        metrics.setNewMessages(0);
        return metrics;
    }

    /**
     * Mock posts for fallback
     */
    private List<Map<String, Object>> getMockPosts() {
        List<Map<String, Object>> posts = new ArrayList<>();
        Map<String, Object> post = new HashMap<>();
        post.put("id", "mock_1");
        post.put("text", "Connect your LinkedIn Company Page for real post analytics.");
        post.put("createdAt", LocalDateTime.now().toString());
        posts.add(post);
        return posts;
    }
}
