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

import java.util.Map;

@Service
public class TwitterService {

    private static final Logger logger = LogManager.getLogger(TwitterService.class);

    @Value("${spring.security.oauth2.client.registration.twitter.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.twitter.client-secret}")
    private String clientSecret;

    @Value("${twitter.redirect.uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Exchange authorization code for access token with PKCE
     */
    public Map<String, Object> exchangeCodeForToken(String code, String codeVerifier) {
        String tokenUrl = "https://api.twitter.com/2/oauth2/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", redirectUri);
        body.add("code_verifier", codeVerifier);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to exchange Twitter code for token", e);
            throw new RuntimeException("Failed to authenticate with Twitter: " + e.getMessage());
        }
    }

    /**
     * Fetch Twitter user profile using access token
     */
    public Map<String, Object> getUserProfile(String accessToken) {
        String userUrl = "https://api.twitter.com/2/users/me?user.fields=id,name,username,profile_image_url,public_metrics";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                userUrl,
                HttpMethod.GET,
                request,
                Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to fetch Twitter user profile", e);
            throw new RuntimeException("Failed to fetch Twitter profile: " + e.getMessage());
        }
    }

    /**
     * Sync metrics from Twitter API
     */
    public AccountMetrics syncMetrics(SocialAccount account) {
        try {
            String accessToken = account.getAccessToken();
            Map<String, Object> userProfile = getUserProfile(accessToken);

            Map<String, Object> data = (Map<String, Object>) userProfile.get("data");
            Map<String, Object> publicMetrics = (Map<String, Object>) data.get("public_metrics");

            AccountMetrics metrics = new AccountMetrics();
            metrics.setConnections((Integer) publicMetrics.get("followers_count"));
            metrics.setPosts((Integer) publicMetrics.get("tweet_count"));
            metrics.setPendingResponses(0); // Twitter doesn't provide this

            logger.info("Successfully synced Twitter metrics for account: {}", account.getAccountName());
            return metrics;
        } catch (Exception e) {
            logger.error("Failed to sync Twitter metrics", e);
            return new AccountMetrics();
        }
    }
}
