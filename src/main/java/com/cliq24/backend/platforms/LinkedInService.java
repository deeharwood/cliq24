package com.cliq24.backend.platforms;

import com.cliq24.backend.model.AccountMetrics;
import com.cliq24.backend.model.SocialAccount;
import com.cliq24.backend.repository.SocialAccountRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        logger.info("Syncing LinkedIn account: {} (type: {})", account.getUsername(), account.getAccountType());

        String accountType = account.getAccountType();

        if ("company".equalsIgnoreCase(accountType)) {
            return syncCompanyPageMetrics(account);
        } else {
            return syncPersonalProfileMetrics(account);
        }
    }

    /**
     * Sync metrics for LinkedIn Company Pages (real API data)
     */
    private AccountMetrics syncCompanyPageMetrics(SocialAccount account) {
        logger.info("Syncing LinkedIn Company Page: {}", account.getUsername());

        String accessToken = account.getAccessToken();
        String organizationId = account.getPlatformUserId();

        if (accessToken == null || organizationId == null) {
            logger.warn("Missing access token or organization ID for company page");
            return getDefaultMetrics();
        }

        try {
            // Fetch follower statistics from LinkedIn API
            Map<String, Object> followerStats = getOrganizationFollowerStats(organizationId, accessToken);

            AccountMetrics metrics = new AccountMetrics();

            // Get follower count
            int followerCount = (Integer) followerStats.getOrDefault("followerCount", 0);
            metrics.setConnections(followerCount); // Use connections field for follower count

            // Get follower gained (last 30 days if available)
            int followersGained = (Integer) followerStats.getOrDefault("followersGained", 0);

            // Calculate engagement score based on follower growth
            int engagementScore = calculateCompanyEngagementScore(followerCount, followersGained);
            metrics.setEngagementScore(engagementScore);

            // Set other metrics to 0 for now (will add post analytics later)
            metrics.setPosts(0);
            metrics.setPendingResponses(0);
            metrics.setNewMessages(0);

            logger.info("Company Page metrics - Followers: {}, Growth: {}, Score: {}",
                followerCount, followersGained, engagementScore);

            return metrics;

        } catch (Exception e) {
            logger.error("Failed to sync company page metrics: {}", e.getMessage(), e);
            return getDefaultMetrics();
        }
    }

    /**
     * Sync metrics for personal LinkedIn profiles (limited data)
     */
    private AccountMetrics syncPersonalProfileMetrics(SocialAccount account) {
        logger.info("Syncing LinkedIn Personal Profile: {}", account.getUsername());

        // Personal profiles have limited API access
        AccountMetrics metrics = new AccountMetrics();
        metrics.setEngagementScore(0);
        metrics.setConnections(0);
        metrics.setPosts(0);
        metrics.setPendingResponses(0);
        metrics.setNewMessages(0);

        logger.info("Personal profile - API limitations prevent detailed analytics");
        return metrics;
    }

    /**
     * Get organization follower statistics from LinkedIn API
     */
    private Map<String, Object> getOrganizationFollowerStats(String organizationId, String accessToken) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // LinkedIn Organization Follower Statistics API
            // https://api.linkedin.com/v2/organizationalEntityFollowerStatistics?q=organizationalEntity&organizationalEntity=urn:li:organization:{id}

            String organizationUrn = "urn:li:organization:" + organizationId;
            String apiUrl = String.format(
                "https://api.linkedin.com/v2/organizationalEntityFollowerStatistics?q=organizationalEntity&organizationalEntity=%s",
                URLEncoder.encode(organizationUrn, StandardCharsets.UTF_8)
            );

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("X-Restli-Protocol-Version", "2.0.0");

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            logger.info("Fetching follower stats from: {}", apiUrl);

            Map response = restTemplate.exchange(
                apiUrl,
                org.springframework.http.HttpMethod.GET,
                entity,
                Map.class
            ).getBody();

            logger.info("LinkedIn API response: {}", response);

            if (response != null && response.containsKey("elements")) {
                List<Map<String, Object>> elements = (List<Map<String, Object>>) response.get("elements");

                if (!elements.isEmpty()) {
                    Map<String, Object> firstElement = elements.get(0);

                    // Extract follower count
                    if (firstElement.containsKey("followerCounts")) {
                        Map<String, Object> followerCounts = (Map<String, Object>) firstElement.get("followerCounts");
                        if (followerCounts.containsKey("organicFollowerCount")) {
                            stats.put("followerCount", followerCounts.get("organicFollowerCount"));
                        }
                    }

                    // Extract followers gained (if time range data available)
                    if (firstElement.containsKey("followerGains")) {
                        Map<String, Object> followerGains = (Map<String, Object>) firstElement.get("followerGains");
                        if (followerGains.containsKey("organicFollowerGain")) {
                            stats.put("followersGained", followerGains.get("organicFollowerGain"));
                        }
                    }
                }
            }

            logger.info("Extracted follower stats: {}", stats);

        } catch (Exception e) {
            logger.error("Error fetching organization follower stats: {}", e.getMessage(), e);
            // Return empty stats on error
        }

        return stats;
    }

    /**
     * Calculate engagement score for company pages based on followers and growth
     */
    private int calculateCompanyEngagementScore(int followerCount, int followersGained) {
        int score = 0;

        // Follower count (max 40 points)
        if (followerCount > 10000) {
            score += 40;
        } else if (followerCount > 5000) {
            score += 35;
        } else if (followerCount > 1000) {
            score += 30;
        } else if (followerCount > 500) {
            score += 20;
        } else if (followerCount > 100) {
            score += 10;
        } else if (followerCount > 0) {
            score += 5;
        }

        // Follower growth (max 60 points)
        if (followersGained > 1000) {
            score += 60;
        } else if (followersGained > 500) {
            score += 50;
        } else if (followersGained > 100) {
            score += 40;
        } else if (followersGained > 50) {
            score += 30;
        } else if (followersGained > 10) {
            score += 20;
        } else if (followersGained > 0) {
            score += 10;
        }

        return Math.min(100, score);
    }

    /**
     * Sync metrics for personal LinkedIn accounts (limited API + manual input)
     */
    private AccountMetrics syncPersonalMetrics(SocialAccount account) {
        logger.info("Syncing personal LinkedIn account: {}", account.getUsername());

        AccountMetrics metrics = new AccountMetrics();
        Map<String, Integer> manualMetrics = account.getManualMetrics();

        // Try to fetch what we can from LinkedIn API
        String accessToken = account.getAccessToken();
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                logger.info("Attempting to fetch LinkedIn profile data from API...");
                Map<String, Object> profileData = fetchPersonalProfileData(accessToken);

                // Extract any available metrics from API
                if (profileData.containsKey("numConnections")) {
                    metrics.setConnections((Integer) profileData.get("numConnections"));
                    logger.info("Fetched connection count from API: {}", metrics.getConnections());
                }

                // If we got data from API, use it and skip manual metrics
                if (metrics.getConnections() > 0) {
                    // Use API data, fill in rest with manual or zeros
                    metrics.setPosts(manualMetrics != null ? manualMetrics.getOrDefault("posts", 0) : 0);
                    metrics.setPendingResponses(manualMetrics != null ? manualMetrics.getOrDefault("pendingResponses", 0) : 0);
                    metrics.setNewMessages(manualMetrics != null ? manualMetrics.getOrDefault("newMessages", 0) : 0);

                    int engagementScore = calculateEngagementScore(metrics);
                    metrics.setEngagementScore(engagementScore);

                    logger.info("Using API data with manual supplements - Connections: {}, Posts: {}",
                        metrics.getConnections(), metrics.getPosts());
                    return metrics;
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch LinkedIn API data, falling back to manual metrics: {}", e.getMessage());
            }
        }

        // Fall back to manual metrics if API didn't provide data
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
     * Try to fetch personal profile data from LinkedIn API
     * This attempts various endpoints to get connection count and other metrics
     */
    private Map<String, Object> fetchPersonalProfileData(String accessToken) {
        Map<String, Object> data = new HashMap<>();

        try {
            // Try the /v2/me endpoint with extended projection
            String apiUrl = "https://api.linkedin.com/v2/me?projection=(id,firstName,lastName,headline,vanityName,profilePicture(displayImage~:playableStreams))";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            Map response = restTemplate.exchange(
                apiUrl,
                org.springframework.http.HttpMethod.GET,
                entity,
                Map.class
            ).getBody();

            logger.info("LinkedIn API response: {}", response);

            // Check if response contains connection count (some scopes may provide this)
            if (response != null && response.containsKey("numConnections")) {
                data.put("numConnections", response.get("numConnections"));
            }

            // Try alternative endpoint for connection count
            try {
                String connectionsUrl = "https://api.linkedin.com/v2/connections?q=viewer&count=0";
                headers = new org.springframework.http.HttpHeaders();
                headers.set("Authorization", "Bearer " + accessToken);
                entity = new org.springframework.http.HttpEntity<>(headers);

                Map connectionsResponse = restTemplate.exchange(
                    connectionsUrl,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class
                ).getBody();

                logger.info("LinkedIn connections API response: {}", connectionsResponse);

                if (connectionsResponse != null && connectionsResponse.containsKey("paging")) {
                    Map paging = (Map) connectionsResponse.get("paging");
                    if (paging.containsKey("total")) {
                        data.put("numConnections", paging.get("total"));
                        logger.info("Found connection count in paging.total: {}", paging.get("total"));
                    }
                }
            } catch (Exception e) {
                logger.debug("Connections endpoint not available: {}", e.getMessage());
            }

        } catch (Exception e) {
            logger.error("Error fetching LinkedIn profile data: {}", e.getMessage());
            throw e;
        }

        return data;
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
     * Get posts for company pages with engagement statistics
     */
    public List<Map<String, Object>> getPosts(String userId, String accountId, int limit) {
        logger.info("Getting posts for LinkedIn account: {}", accountId);

        SocialAccount account = socialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (!"company".equalsIgnoreCase(account.getAccountType())) {
            // Personal accounts can't fetch posts via API
            logger.info("Personal account detected - returning empty posts list");
            return new ArrayList<>();
        }

        String accessToken = account.getAccessToken();
        String organizationId = account.getPlatformUserId();

        if (accessToken == null || organizationId == null) {
            logger.warn("Missing access token or organization ID");
            return new ArrayList<>();
        }

        try {
            // Fetch UGC posts from LinkedIn
            List<Map<String, Object>> posts = fetchUGCPosts(organizationId, accessToken, limit);

            // Enrich each post with statistics
            for (Map<String, Object> post : posts) {
                String shareUrn = (String) post.get("shareUrn");
                if (shareUrn != null) {
                    Map<String, Object> stats = getShareStatistics(shareUrn, accessToken);
                    post.putAll(stats); // Add engagement metrics to post
                }
            }

            logger.info("Successfully fetched {} posts with statistics", posts.size());
            return posts;

        } catch (Exception e) {
            logger.error("Failed to fetch LinkedIn posts: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Fetch UGC posts from LinkedIn API
     */
    private List<Map<String, Object>> fetchUGCPosts(String organizationId, String accessToken, int limit) {
        List<Map<String, Object>> posts = new ArrayList<>();

        try {
            String organizationUrn = "urn:li:organization:" + organizationId;
            String apiUrl = String.format(
                "https://api.linkedin.com/v2/ugcPosts?q=authors&authors=List(%s)&count=%d&sortBy=LAST_MODIFIED",
                URLEncoder.encode(organizationUrn, StandardCharsets.UTF_8),
                limit
            );

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("X-Restli-Protocol-Version", "2.0.0");
            headers.set("LinkedIn-Version", "202301");

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            logger.info("Fetching UGC posts from: {}", apiUrl);

            Map response = restTemplate.exchange(
                apiUrl,
                org.springframework.http.HttpMethod.GET,
                entity,
                Map.class
            ).getBody();

            logger.info("UGC Posts API response received");

            if (response != null && response.containsKey("elements")) {
                List<Map<String, Object>> elements = (List<Map<String, Object>>) response.get("elements");

                for (Map<String, Object> element : elements) {
                    Map<String, Object> post = new HashMap<>();

                    // Extract post ID/URN
                    String id = (String) element.get("id");
                    post.put("id", id);
                    post.put("shareUrn", id); // Use for fetching statistics

                    // Extract post text
                    if (element.containsKey("specificContent")) {
                        Map<String, Object> specificContent = (Map<String, Object>) element.get("specificContent");
                        if (specificContent.containsKey("com.linkedin.ugc.ShareContent")) {
                            Map<String, Object> shareContent = (Map<String, Object>) specificContent.get("com.linkedin.ugc.ShareContent");
                            if (shareContent.containsKey("shareCommentary")) {
                                Map<String, Object> commentary = (Map<String, Object>) shareContent.get("shareCommentary");
                                post.put("text", commentary.get("text"));
                            }
                        }
                    }

                    // Extract created timestamp
                    if (element.containsKey("created")) {
                        Map<String, Object> created = (Map<String, Object>) element.get("created");
                        Long time = ((Number) created.get("time")).longValue();
                        post.put("createdAt", java.time.Instant.ofEpochMilli(time).toString());
                    }

                    // Extract author
                    post.put("author", element.get("author"));

                    posts.add(post);
                }

                logger.info("Parsed {} UGC posts", posts.size());
            }

        } catch (Exception e) {
            logger.error("Error fetching UGC posts: {}", e.getMessage(), e);
            throw e;
        }

        return posts;
    }

    /**
     * Get statistics for a specific share/post
     */
    private Map<String, Object> getShareStatistics(String shareUrn, String accessToken) {
        Map<String, Object> stats = new HashMap<>();

        try {
            String apiUrl = String.format(
                "https://api.linkedin.com/v2/organizationalEntityShareStatistics?q=organizationalEntity&shares=List(%s)",
                URLEncoder.encode(shareUrn, StandardCharsets.UTF_8)
            );

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("X-Restli-Protocol-Version", "2.0.0");

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            logger.debug("Fetching share statistics for: {}", shareUrn);

            Map response = restTemplate.exchange(
                apiUrl,
                org.springframework.http.HttpMethod.GET,
                entity,
                Map.class
            ).getBody();

            if (response != null && response.containsKey("elements")) {
                List<Map<String, Object>> elements = (List<Map<String, Object>>) response.get("elements");

                if (!elements.isEmpty()) {
                    Map<String, Object> shareStats = elements.get(0);

                    // Extract total share statistics
                    if (shareStats.containsKey("totalShareStatistics")) {
                        Map<String, Object> totalStats = (Map<String, Object>) shareStats.get("totalShareStatistics");

                        stats.put("impressionCount", totalStats.getOrDefault("impressionCount", 0));
                        stats.put("likeCount", totalStats.getOrDefault("likeCount", 0));
                        stats.put("commentCount", totalStats.getOrDefault("commentCount", 0));
                        stats.put("shareCount", totalStats.getOrDefault("shareCount", 0));
                        stats.put("clickCount", totalStats.getOrDefault("clickCount", 0));
                        stats.put("engagement", totalStats.getOrDefault("engagement", 0));

                        // Calculate engagement rate
                        int impressions = ((Number) totalStats.getOrDefault("impressionCount", 0)).intValue();
                        int engagementTotal = ((Number) totalStats.getOrDefault("engagement", 0)).intValue();

                        if (impressions > 0) {
                            double engagementRate = (engagementTotal * 100.0) / impressions;
                            stats.put("engagementRate", Math.round(engagementRate * 100.0) / 100.0);
                        } else {
                            stats.put("engagementRate", 0.0);
                        }
                    }

                    logger.debug("Share statistics: {}", stats);
                }
            }

        } catch (Exception e) {
            logger.error("Error fetching share statistics: {}", e.getMessage(), e);
            // Return empty stats on error - don't fail the whole request
        }

        return stats;
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
