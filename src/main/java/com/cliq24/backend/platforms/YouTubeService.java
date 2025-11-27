package com.cliq24.backend.platforms;

import com.cliq24.backend.model.AccountMetrics;
import com.cliq24.backend.model.SocialAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class YouTubeService {

    private static final Logger logger = LogManager.getLogger(YouTubeService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.youtube.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.youtube.client-secret}")
    private String clientSecret;

    @Value("${youtube.redirect.uri}")
    private String redirectUri;

    /**
     * Exchange authorization code for access token (uses Google OAuth)
     */
    public Map<String, Object> exchangeCodeForToken(String code) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to exchange YouTube/Google code for token", e);
            throw new RuntimeException("Failed to authenticate with Google/YouTube: " + e.getMessage());
        }
    }

    /**
     * Fetch YouTube channel info using OAuth access token
     */
    public Map<String, Object> getChannelInfo(String accessToken) {
        String url = "https://www.googleapis.com/youtube/v3/channels?part=snippet,statistics&mine=true";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to fetch YouTube channel info", e);
            throw new RuntimeException("Failed to fetch YouTube channel: " + e.getMessage());
        }
    }

    /**
     * Sync metrics from YouTube API
     */
    public AccountMetrics syncMetrics(SocialAccount account) {
        try {
            String accessToken = account.getAccessToken();
            Map<String, Object> channelData = getChannelInfo(accessToken);

            List<Map<String, Object>> items = (List<Map<String, Object>>) channelData.get("items");

            if (items == null || items.isEmpty()) {
                logger.warn("No YouTube channel found for account");
                return new AccountMetrics();
            }

            Map<String, Object> channel = items.get(0);
            Map<String, Object> statistics = (Map<String, Object>) channel.get("statistics");

            AccountMetrics metrics = new AccountMetrics();

            if (statistics != null) {
                // YouTube API returns numbers as strings or integers, handle both
                Object subscriberCountObj = statistics.get("subscriberCount");
                int subscriberCount = 0;
                if (subscriberCountObj instanceof String) {
                    subscriberCount = Integer.parseInt((String) subscriberCountObj);
                } else if (subscriberCountObj instanceof Number) {
                    subscriberCount = ((Number) subscriberCountObj).intValue();
                }
                metrics.setConnections(subscriberCount);

                Object videoCountObj = statistics.get("videoCount");
                int videoCount = 0;
                if (videoCountObj instanceof String) {
                    videoCount = Integer.parseInt((String) videoCountObj);
                } else if (videoCountObj instanceof Number) {
                    videoCount = ((Number) videoCountObj).intValue();
                }
                metrics.setPosts(videoCount);

                logger.info("YouTube metrics - Subscribers: {}, Videos: {}",
                    subscriberCount, videoCount);
            }

            logger.info("Successfully synced YouTube metrics for account: {}", account.getUsername());
            return metrics;
        } catch (Exception e) {
            logger.error("Failed to sync YouTube metrics", e);
            return new AccountMetrics();
        }
    }
}
