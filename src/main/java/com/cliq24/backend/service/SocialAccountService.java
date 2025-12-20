package com.cliq24.backend.service;

import com.cliq24.backend.dto.SocialAccountDTO;
import com.cliq24.backend.mapper.SocialAccountMapper;
import com.cliq24.backend.model.AccountMetrics;
import com.cliq24.backend.model.SocialAccount;
import com.cliq24.backend.platforms.*;
import com.cliq24.backend.repository.SocialAccountRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SocialAccountService {

    private static final Logger logger = LogManager.getLogger(SocialAccountService.class);

    private final SocialAccountRepository socialAccountRepository;
    private final SocialAccountMapper socialAccountMapper;
    private final AuthService authService;
    private final FacebookService facebookService;
    private final InstagramService instagramService;
    private final TwitterService twitterService;
    private final LinkedInService linkedInService;
    private final TikTokService tikTokService;
    private final YouTubeService youTubeService;
    private final SnapchatService snapchatService;
    private final SubscriptionService subscriptionService;
    private final com.cliq24.backend.repository.UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String facebookAppId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
    private String facebookAppSecret;

    @Value("${facebook.redirect.uri}")
    private String facebookRedirectUri;

    @Value("${spring.security.oauth2.client.registration.instagram.client-id}")
    private String instagramAppId;

    @Value("${spring.security.oauth2.client.registration.instagram.client-secret}")
    private String instagramAppSecret;

    @Value("${instagram.redirect.uri}")
    private String instagramRedirectUri;

    @Value("${spring.security.oauth2.client.registration.linkedin.client-id}")
    private String linkedInClientId;

    @Value("${spring.security.oauth2.client.registration.linkedin.client-secret}")
    private String linkedInClientSecret;

    @Value("${linkedin.redirect.uri}")
    private String linkedInRedirectUri;

    @Value("${spring.security.oauth2.client.registration.snapchat.client-id}")
    private String snapchatClientId;

    @Value("${spring.security.oauth2.client.registration.snapchat.client-secret}")
    private String snapchatClientSecret;

    @Value("${snapchat.redirect.uri}")
    private String snapchatRedirectUri;

    @Value("${tiktok.client.key}")
    private String tiktokClientKey;

    @Value("${tiktok.client.secret}")
    private String tiktokClientSecret;

    @Value("${tiktok.redirect.uri}")
    private String tiktokRedirectUri;

    @Autowired
    public SocialAccountService(SocialAccountRepository socialAccountRepository,
                               SocialAccountMapper socialAccountMapper,
                               AuthService authService,
                               FacebookService facebookService,
                               InstagramService instagramService,
                               TwitterService twitterService,
                               LinkedInService linkedInService,
                               TikTokService tikTokService,
                               YouTubeService youTubeService,
                               SnapchatService snapchatService,
                               SubscriptionService subscriptionService,
                               com.cliq24.backend.repository.UserRepository userRepository) {
        this.socialAccountRepository = socialAccountRepository;
        this.socialAccountMapper = socialAccountMapper;
        this.authService = authService;
        this.facebookService = facebookService;
        this.instagramService = instagramService;
        this.twitterService = twitterService;
        this.linkedInService = linkedInService;
        this.tikTokService = tikTokService;
        this.youTubeService = youTubeService;
        this.snapchatService = snapchatService;
        this.subscriptionService = subscriptionService;
        this.userRepository = userRepository;
    }

    /**
     * Check if user can add a new social account based on subscription tier
     */
    private void checkAccountLimit(String userId, String platform) {
        // Check if user is updating an existing account
        boolean isUpdate = socialAccountRepository.findByUserIdAndPlatform(userId, platform.toLowerCase()).isPresent();
        if (isUpdate) {
            return; // Allow updates to existing accounts
        }

        // Count current accounts
        long currentAccountCount = socialAccountRepository.countByUserId(userId);

        // Get user and check limit
        com.cliq24.backend.model.User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!subscriptionService.canAddAccount(user, (int) currentAccountCount)) {
            int limit = subscriptionService.getAccountLimit(user);
            throw new RuntimeException(
                "Account limit reached. Free users can connect up to " + limit +
                " accounts. Upgrade to Premium for unlimited accounts."
            );
        }
    }

    public List<SocialAccountDTO> getUserAccounts(String authHeader) {
        // Removed excessive logging - called on every dashboard load
        String userId = authService.validateAndExtractUserId(authHeader);
        List<SocialAccount> accounts = socialAccountRepository.findByUserId(userId);

        return accounts.stream()
                .map(socialAccountMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<SocialAccountDTO> getUserAccountsByUserId(String userId) {
        // Removed excessive logging - called frequently
        List<SocialAccount> accounts = socialAccountRepository.findByUserId(userId);

        return accounts.stream()
                .map(socialAccountMapper::toDTO)
                .collect(Collectors.toList());
    }

    public SocialAccount getAccountById(String accountId) {
        return socialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Social account not found with ID: " + accountId));
    }

    public void disconnectAccount(String accountId, String authHeader) {
        logger.info("Disconnecting account: {}", accountId);

        String userId = authService.validateAndExtractUserId(authHeader);

        SocialAccount account = socialAccountRepository.findById(accountId)
                .orElseThrow(() -> {
                    logger.error("Account not found: {}", accountId);
                    return new RuntimeException("Account not found");
                });

        if (!account.getUserId().equals(userId)) {
            logger.warn("Unauthorized disconnect attempt for account {} by user {}", accountId, userId);
            throw new RuntimeException("Unauthorized");
        }

        socialAccountRepository.deleteById(accountId);
        logger.info("Successfully disconnected account {} for user {}", accountId, userId);
    }

    public void disconnectAccountByUserId(String accountId, String userId) {
        logger.info("Disconnecting account: {} for user: {}", accountId, userId);

        SocialAccount account = socialAccountRepository.findById(accountId)
                .orElseThrow(() -> {
                    logger.error("Account not found: {}", accountId);
                    return new RuntimeException("Account not found");
                });

        if (!account.getUserId().equals(userId)) {
            logger.warn("Unauthorized disconnect attempt for account {} by user {}", accountId, userId);
            throw new RuntimeException("Unauthorized");
        }

        socialAccountRepository.deleteById(accountId);
        logger.info("Successfully disconnected account {} for user {}", accountId, userId);
    }

    public SocialAccountDTO connectAccount(String platform, String authHeader, String code) {
        logger.info("Connecting {} account", platform);

        String userId = authService.validateAndExtractUserId(authHeader);
        logger.debug("User ID extracted: {}", userId);

        // For now, create a placeholder account
        // In production, this would exchange the code for access tokens via OAuth
        SocialAccount account = new SocialAccount();
        account.setUserId(userId);
        account.setPlatform(platform.toLowerCase());
        account.setPlatformUserId("placeholder_" + System.currentTimeMillis());
        account.setUsername("user_" + platform);
        account.setAccessToken("encrypted_access_token");
        account.setConnectedAt(LocalDateTime.now());

        SocialAccount savedAccount = socialAccountRepository.save(account);
        logger.info("Successfully connected {} account for user {}", platform, userId);

        return socialAccountMapper.toDTO(savedAccount);
    }

    public SocialAccountDTO connectAccountByUserId(String platform, String userId, String code) {
        logger.info("Connecting {} account for user: {}", platform, userId);

        // For now, create a placeholder account
        // In production, this would exchange the code for access tokens via OAuth
        SocialAccount account = new SocialAccount();
        account.setUserId(userId);
        account.setPlatform(platform.toLowerCase());
        account.setPlatformUserId("placeholder_" + System.currentTimeMillis());
        account.setUsername("user_" + platform);
        account.setAccessToken("encrypted_access_token");
        account.setConnectedAt(LocalDateTime.now());

        SocialAccount savedAccount = socialAccountRepository.save(account);
        logger.info("Successfully connected {} account for user {}", platform, userId);

        return socialAccountMapper.toDTO(savedAccount);
    }

    public SocialAccountDTO syncMetrics(String accountId, String authHeader) {
        logger.info("Syncing metrics for account: {}", accountId);

        String userId = authService.validateAndExtractUserId(authHeader);

        SocialAccount account = socialAccountRepository.findById(accountId)
                .orElseThrow(() -> {
                    logger.error("Account not found: {}", accountId);
                    return new RuntimeException("Account not found");
                });

        if (!account.getUserId().equals(userId)) {
            logger.warn("Unauthorized sync attempt for account {} by user {}", accountId, userId);
            throw new RuntimeException("Unauthorized");
        }

        // Sync metrics based on platform
        switch (account.getPlatform().toLowerCase()) {
            case "facebook":
                account.setMetrics(facebookService.syncMetrics(account));
                break;
            case "instagram":
                account.setMetrics(instagramService.syncMetrics(account));
                break;
            case "twitter":
                account.setMetrics(twitterService.syncMetrics(account));
                break;
            case "linkedin":
                account.setMetrics(linkedInService.syncMetrics(account));
                break;
            case "tiktok":
                account.setMetrics(tikTokService.syncMetrics(account));
                break;
            case "youtube":
                account.setMetrics(youTubeService.syncMetrics(account));
                break;
            case "snapchat":
                account.setMetrics(snapchatService.syncMetrics(account));
                break;
            default:
                logger.warn("Unsupported platform: {}", account.getPlatform());
                throw new RuntimeException("Unsupported platform: " + account.getPlatform());
        }

        account.setLastSynced(LocalDateTime.now());
        SocialAccount updatedAccount = socialAccountRepository.save(account);

        // Removed excessive info logging - sync can happen frequently
        return socialAccountMapper.toDTO(updatedAccount);
    }

    public SocialAccountDTO syncMetricsByUserId(String accountId, String userId) {
        // Removed excessive info logging - sync can happen frequently

        SocialAccount account = socialAccountRepository.findById(accountId)
                .orElseThrow(() -> {
                    logger.error("Account not found: {}", accountId);
                    return new RuntimeException("Account not found");
                });

        if (!account.getUserId().equals(userId)) {
            logger.warn("Unauthorized sync attempt for account {} by user {}", accountId, userId);
            throw new RuntimeException("Unauthorized");
        }

        // Sync metrics based on platform
        switch (account.getPlatform().toLowerCase()) {
            case "facebook":
                account.setMetrics(facebookService.syncMetrics(account));
                break;
            case "instagram":
                account.setMetrics(instagramService.syncMetrics(account));
                break;
            case "twitter":
                account.setMetrics(twitterService.syncMetrics(account));
                break;
            case "linkedin":
                account.setMetrics(linkedInService.syncMetrics(account));
                break;
            case "tiktok":
                account.setMetrics(tikTokService.syncMetrics(account));
                break;
            case "youtube":
                account.setMetrics(youTubeService.syncMetrics(account));
                break;
            case "snapchat":
                account.setMetrics(snapchatService.syncMetrics(account));
                break;
            default:
                logger.warn("Unsupported platform: {}", account.getPlatform());
                throw new RuntimeException("Unsupported platform: " + account.getPlatform());
        }

        account.setLastSynced(LocalDateTime.now());
        SocialAccount updatedAccount = socialAccountRepository.save(account);

        logger.info("Successfully synced metrics for account {}", accountId);
        return socialAccountMapper.toDTO(updatedAccount);
    }

    public SocialAccountDTO connectFacebookAccount(String authHeader, String code) {
        logger.info("Connecting Facebook account with OAuth code");

        // Extract userId - handle both JWT token and raw userId from cookie auth
        String userId;

        // Remove "Bearer " prefix if present
        String tokenPart = authHeader != null && authHeader.startsWith("Bearer ")
            ? authHeader.substring(7)
            : authHeader;

        // Check if it's a JWT (contains at least 2 dots)
        boolean isJWT = tokenPart != null && tokenPart.split("\\.").length == 3;

        if (isJWT) {
            // It's a JWT token - validate and extract userId
            logger.info("Detected JWT token, validating...");
            try {
                userId = authService.validateAndExtractUserId(authHeader);
            } catch (Exception e) {
                logger.error("JWT validation failed: {}", e.getMessage());
                throw new RuntimeException("Invalid or expired token");
            }
        } else {
            // It's a raw userId from cookie auth
            logger.info("Detected raw userId from cookie auth: {}", tokenPart);
            userId = tokenPart;
        }

        try {
            // Exchange code for access token
            String tokenUrl = String.format(
                "https://graph.facebook.com/v18.0/oauth/access_token?client_id=%s&client_secret=%s&code=%s&redirect_uri=%s",
                facebookAppId,
                facebookAppSecret,
                code,
                facebookRedirectUri
            );

            Map<String, Object> tokenResponse = restTemplate.getForObject(tokenUrl, Map.class);
            String accessToken = (String) tokenResponse.get("access_token");

            logger.info("Successfully obtained Facebook access token");

            // Get user's Facebook profile
            String profileUrl = String.format(
                "https://graph.facebook.com/me?fields=id,name,email&access_token=%s",
                accessToken
            );

            Map<String, Object> profile = restTemplate.getForObject(profileUrl, Map.class);
            String facebookId = (String) profile.get("id");
            String name = (String) profile.get("name");

            logger.info("Retrieved Facebook profile for user: {}", name);

            // Check if account already connected
            SocialAccount existingAccount = socialAccountRepository
                .findByUserIdAndPlatform(userId, "facebook")
                .orElse(null);

            if (existingAccount != null) {
                // Update existing account
                existingAccount.setPlatformUserId(facebookId);
                existingAccount.setUsername(name);
                existingAccount.setAccessToken(accessToken);
                existingAccount.setConnectedAt(LocalDateTime.now());

                // Sync metrics
                existingAccount.setMetrics(facebookService.syncMetrics(existingAccount));
                existingAccount.setLastSynced(LocalDateTime.now());

                SocialAccount saved = socialAccountRepository.save(existingAccount);
                logger.info("Updated existing Facebook account");
                return socialAccountMapper.toDTO(saved);
            } else {
                // Create new account
                SocialAccount account = new SocialAccount();
                account.setUserId(userId);
                account.setPlatform("Facebook");
                account.setPlatformUserId(facebookId);
                account.setUsername(name);
                account.setAccessToken(accessToken);
                account.setConnectedAt(LocalDateTime.now());

                // Initialize with demo metrics (real API integration coming soon)
                account.setMetrics(facebookService.syncMetrics(account));
                account.setLastSynced(LocalDateTime.now());

                SocialAccount savedAccount = socialAccountRepository.save(account);
                logger.info("Successfully connected Facebook account for user {}", userId);

                return socialAccountMapper.toDTO(savedAccount);
            }

        } catch (Exception e) {
            logger.error("Error connecting Facebook account: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to connect Facebook account: " + e.getMessage());
        }
    }

    public SocialAccountDTO connectInstagramAccount(String authHeader, String code) {
        logger.info("Connecting Instagram account with OAuth code");

        String userId = authService.validateAndExtractUserId(authHeader);

        try {
            // Exchange code for Facebook access token (Instagram uses Facebook OAuth)
            String tokenUrl = String.format(
                "https://graph.facebook.com/v18.0/oauth/access_token?client_id=%s&client_secret=%s&code=%s&redirect_uri=%s",
                instagramAppId,
                instagramAppSecret,
                code,
                instagramRedirectUri
            );

            Map<String, Object> tokenResponse = restTemplate.getForObject(tokenUrl, Map.class);
            String facebookAccessToken = (String) tokenResponse.get("access_token");

            logger.info("Successfully obtained Facebook access token for Instagram");

            // Get Facebook user's pages (Instagram Business accounts are linked to Facebook Pages)
            String pagesUrl = String.format(
                "https://graph.facebook.com/me/accounts?fields=instagram_business_account&access_token=%s",
                facebookAccessToken
            );

            Map<String, Object> pagesResponse = restTemplate.getForObject(pagesUrl, Map.class);

            // For demo purposes, if no Instagram Business account found, use demo data
            if (pagesResponse == null || !pagesResponse.containsKey("data")) {
                logger.warn("No Instagram Business account found, using demo mode");
                return createDemoInstagramAccount(userId);
            }

            // Get the first Instagram Business Account ID
            String instagramId = "demo_instagram_" + System.currentTimeMillis();
            String username = "instagram_user";

            // For now, use demo mode since Instagram requires Business account setup
            logger.info("Creating Instagram account in demo mode");
            return createDemoInstagramAccount(userId);

        } catch (Exception e) {
            logger.error("Error connecting Instagram account: {}", e.getMessage(), e);
            logger.info("Falling back to demo mode");
            return createDemoInstagramAccount(userId);
        }
    }

    private SocialAccountDTO createDemoInstagramAccount(String userId) {
        logger.info("Creating demo Instagram account for user {}", userId);

        // Check if account already exists
        SocialAccount existingAccount = socialAccountRepository
            .findByUserIdAndPlatform(userId, "instagram")
            .orElse(null);

        if (existingAccount != null) {
            // Update existing account with new demo data
            existingAccount.setConnectedAt(LocalDateTime.now());
            existingAccount.setMetrics(instagramService.syncMetrics(existingAccount));
            existingAccount.setLastSynced(LocalDateTime.now());

            SocialAccount saved = socialAccountRepository.save(existingAccount);
            logger.info("Updated existing demo Instagram account");
            return socialAccountMapper.toDTO(saved);
        }

        // Create new demo account
        SocialAccount account = new SocialAccount();
        account.setUserId(userId);
        account.setPlatform("Instagram");
        account.setPlatformUserId("demo_instagram_" + System.currentTimeMillis());
        account.setUsername("your_instagram");
        account.setAccessToken("demo_access_token");
        account.setConnectedAt(LocalDateTime.now());

        // Set demo metrics
        AccountMetrics metrics = new AccountMetrics();
        metrics.setEngagementScore(92);
        metrics.setConnections(28300);
        metrics.setPosts(567);
        metrics.setPendingResponses(8);
        metrics.setNewMessages(45);

        account.setMetrics(metrics);
        account.setLastSynced(LocalDateTime.now());

        SocialAccount savedAccount = socialAccountRepository.save(account);
        logger.info("Successfully created demo Instagram account for user {}", userId);

        return socialAccountMapper.toDTO(savedAccount);
    }

    public SocialAccountDTO connectLinkedInAccount(String authHeader, String code) {
        logger.info("Connecting LinkedIn account with OAuth code");

        String userId = authService.validateAndExtractUserId(authHeader);

        try {
            // Exchange code for access token
            String tokenUrl = "https://www.linkedin.com/oauth/v2/accessToken";

            // LinkedIn requires form-encoded POST - URL encode all values
            String formData = String.format(
                "grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s&client_secret=%s",
                URLEncoder.encode(code, StandardCharsets.UTF_8),
                URLEncoder.encode(linkedInRedirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(linkedInClientId, StandardCharsets.UTF_8),
                URLEncoder.encode(linkedInClientSecret, StandardCharsets.UTF_8)
            );

            logger.debug("LinkedIn token request formData: grant_type=authorization_code&code=REDACTED&redirect_uri={}&client_id={}",
                linkedInRedirectUri, linkedInClientId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> request = new HttpEntity<>(formData, headers);

            Map<String, Object> tokenResponse = restTemplate.postForObject(
                tokenUrl,
                request,
                Map.class
            );

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                logger.error("LinkedIn token response is null or missing access_token");
                throw new RuntimeException("Failed to get access token from LinkedIn");
            }

            String accessToken = (String) tokenResponse.get("access_token");
            logger.info("Successfully obtained LinkedIn access token");

            // Get user's LinkedIn profile using OpenID Connect userinfo endpoint
            String profileUrl = "https://api.linkedin.com/v2/userinfo";

            HttpHeaders profileHeaders = new HttpHeaders();
            profileHeaders.setBearerAuth(accessToken);

            HttpEntity<?> profileRequest = new HttpEntity<>(profileHeaders);

            Map<String, Object> profile = restTemplate.exchange(
                profileUrl,
                HttpMethod.GET,
                profileRequest,
                Map.class
            ).getBody();

            String linkedInId = (String) profile.get("sub");
            String name = (String) profile.get("name");
            String email = (String) profile.get("email");

            logger.info("Retrieved LinkedIn profile for user: {}", name);

            // Check if account already connected
            SocialAccount existingAccount = socialAccountRepository
                .findByUserIdAndPlatform(userId, "linkedin")
                .orElse(null);

            if (existingAccount != null) {
                existingAccount.setPlatformUserId(linkedInId);
                existingAccount.setUsername(name != null ? name : email);
                existingAccount.setAccessToken(accessToken);
                existingAccount.setConnectedAt(LocalDateTime.now());
                existingAccount.setMetrics(linkedInService.syncMetrics(existingAccount));
                existingAccount.setLastSynced(LocalDateTime.now());

                SocialAccount saved = socialAccountRepository.save(existingAccount);
                logger.info("Updated existing LinkedIn account");
                return socialAccountMapper.toDTO(saved);
            } else {
                SocialAccount account = new SocialAccount();
                account.setUserId(userId);
                account.setPlatform("LinkedIn");
                account.setPlatformUserId(linkedInId);
                account.setUsername(name != null ? name : email);
                account.setAccessToken(accessToken);
                account.setConnectedAt(LocalDateTime.now());
                account.setMetrics(linkedInService.syncMetrics(account));
                account.setLastSynced(LocalDateTime.now());

                SocialAccount savedAccount = socialAccountRepository.save(account);
                logger.info("Successfully connected LinkedIn account for user {}", userId);
                return socialAccountMapper.toDTO(savedAccount);
            }

        } catch (Exception e) {
            logger.error("Error connecting LinkedIn account: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to connect LinkedIn account: " + e.getMessage());
        }
    }

    public SocialAccountDTO connectTwitterAccount(String authHeader, String code, String codeVerifier) {
        logger.info("Connecting Twitter account with OAuth code and PKCE");

        String userId = authService.validateAndExtractUserId(authHeader);

        // Check account limit before connecting
        checkAccountLimit(userId, "twitter");

        try {
            // Exchange code for access token with PKCE
            Map<String, Object> tokenResponse = twitterService.exchangeCodeForToken(code, codeVerifier);
            String accessToken = (String) tokenResponse.get("access_token");

            logger.info("Successfully obtained Twitter access token");

            // Get user's Twitter profile
            Map<String, Object> profileResponse = twitterService.getUserProfile(accessToken);
            Map<String, Object> profile = (Map<String, Object>) profileResponse.get("data");

            String twitterId = (String) profile.get("id");
            String username = (String) profile.get("username");
            String name = (String) profile.get("name");

            logger.info("Retrieved Twitter profile for user: {}", username);

            // Check if account already connected
            SocialAccount existingAccount = socialAccountRepository
                .findByUserIdAndPlatform(userId, "twitter")
                .orElse(null);

            if (existingAccount != null) {
                existingAccount.setPlatformUserId(twitterId);
                existingAccount.setUsername(username);
                existingAccount.setAccountName(name);
                existingAccount.setAccessToken(accessToken);
                existingAccount.setConnectedAt(LocalDateTime.now());
                existingAccount.setMetrics(twitterService.syncMetrics(existingAccount));
                existingAccount.setLastSynced(LocalDateTime.now());

                SocialAccount saved = socialAccountRepository.save(existingAccount);
                logger.info("Updated existing Twitter account");
                return socialAccountMapper.toDTO(saved);
            } else {
                SocialAccount account = new SocialAccount();
                account.setUserId(userId);
                account.setPlatform("Twitter");
                account.setPlatformUserId(twitterId);
                account.setUsername(username);
                account.setAccountName(name);
                account.setAccessToken(accessToken);
                account.setConnectedAt(LocalDateTime.now());
                account.setMetrics(twitterService.syncMetrics(account));
                account.setLastSynced(LocalDateTime.now());

                SocialAccount savedAccount = socialAccountRepository.save(account);
                logger.info("Successfully connected Twitter account for user {}", userId);
                return socialAccountMapper.toDTO(savedAccount);
            }

        } catch (Exception e) {
            logger.error("Error connecting Twitter account: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to connect Twitter account: " + e.getMessage());
        }
    }

    public SocialAccountDTO connectYouTubeAccount(String authHeader, String code) {
        logger.info("Connecting YouTube account with OAuth code");

        String userId = authService.validateAndExtractUserId(authHeader);

        // Check account limit before connecting
        checkAccountLimit(userId, "youtube");

        try {
            // Exchange code for access token using YouTubeService
            Map<String, Object> tokenResponse = youTubeService.exchangeCodeForToken(code);
            String accessToken = (String) tokenResponse.get("access_token");

            logger.info("Successfully obtained YouTube/Google access token");

            // Get YouTube channel info
            Map<String, Object> channelData = youTubeService.getChannelInfo(accessToken);
            List<Map<String, Object>> items = (List<Map<String, Object>>) channelData.get("items");

            if (items == null || items.isEmpty()) {
                throw new RuntimeException("No YouTube channel found for this Google account");
            }

            Map<String, Object> channel = items.get(0);
            Map<String, Object> snippet = (Map<String, Object>) channel.get("snippet");
            String channelId = (String) channel.get("id");
            String channelTitle = (String) snippet.get("title");

            logger.info("Retrieved YouTube channel: {}", channelTitle);

            // Check if account already connected
            SocialAccount existingAccount = socialAccountRepository
                .findByUserIdAndPlatform(userId, "youtube")
                .orElse(null);

            if (existingAccount != null) {
                existingAccount.setPlatformUserId(channelId);
                existingAccount.setUsername(channelTitle);
                existingAccount.setAccountName(channelTitle);
                existingAccount.setAccessToken(accessToken);
                existingAccount.setConnectedAt(LocalDateTime.now());
                existingAccount.setMetrics(youTubeService.syncMetrics(existingAccount));
                existingAccount.setLastSynced(LocalDateTime.now());

                SocialAccount saved = socialAccountRepository.save(existingAccount);
                logger.info("Updated existing YouTube account");
                return socialAccountMapper.toDTO(saved);
            } else {
                SocialAccount account = new SocialAccount();
                account.setUserId(userId);
                account.setPlatform("YouTube");
                account.setPlatformUserId(channelId);
                account.setUsername(channelTitle);
                account.setAccountName(channelTitle);
                account.setAccessToken(accessToken);
                account.setConnectedAt(LocalDateTime.now());
                account.setMetrics(youTubeService.syncMetrics(account));
                account.setLastSynced(LocalDateTime.now());

                SocialAccount savedAccount = socialAccountRepository.save(account);
                logger.info("Successfully connected YouTube account for user {}", userId);
                return socialAccountMapper.toDTO(savedAccount);
            }

        } catch (Exception e) {
            logger.error("Error connecting YouTube account: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to connect YouTube account: " + e.getMessage());
        }
    }

    public SocialAccountDTO connectSnapchatAccount(String authHeader, String code, String codeVerifier) {
        logger.info("Connecting Snapchat account with OAuth code and PKCE");

        String userId = authService.validateAndExtractUserId(authHeader);

        try {
            // Exchange code for access token with PKCE code_verifier
            // Note: Following Snapchat's example - not including client_secret in body for PKCE
            String tokenUrl = "https://accounts.snapchat.com/accounts/oauth2/token";

            // Server-side: Use BOTH PKCE code_verifier AND client_secret
            String formData = String.format(
                "grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s&client_secret=%s&code_verifier=%s",
                URLEncoder.encode(code, StandardCharsets.UTF_8),
                URLEncoder.encode(snapchatRedirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(snapchatClientId, StandardCharsets.UTF_8),
                URLEncoder.encode(snapchatClientSecret, StandardCharsets.UTF_8),
                URLEncoder.encode(codeVerifier != null ? codeVerifier : "", StandardCharsets.UTF_8)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> request = new HttpEntity<>(formData, headers);

            Map<String, Object> tokenResponse = restTemplate.postForObject(
                tokenUrl,
                request,
                Map.class
            );

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                logger.error("Snapchat token response is null or missing access_token");
                throw new RuntimeException("Failed to get access token from Snapchat");
            }

            String accessToken = (String) tokenResponse.get("access_token");
            logger.info("Successfully obtained Snapchat access token");

            // Get user's Snapchat profile
            String profileUrl = "https://kit.snapchat.com/v1/me";

            HttpHeaders profileHeaders = new HttpHeaders();
            profileHeaders.setBearerAuth(accessToken);

            HttpEntity<?> profileRequest = new HttpEntity<>(profileHeaders);

            Map<String, Object> profile = restTemplate.exchange(
                profileUrl,
                HttpMethod.GET,
                profileRequest,
                Map.class
            ).getBody();

            // Extract user info from profile
            Map<String, Object> data = (Map<String, Object>) profile.get("data");
            Map<String, Object> me = (Map<String, Object>) data.get("me");
            String snapchatId = (String) me.get("externalId");
            String displayName = (String) me.get("displayName");

            logger.info("Retrieved Snapchat profile for user: {}", displayName);

            // Check if account already connected
            SocialAccount existingAccount = socialAccountRepository
                .findByUserIdAndPlatform(userId, "snapchat")
                .orElse(null);

            if (existingAccount != null) {
                existingAccount.setPlatformUserId(snapchatId);
                existingAccount.setUsername(displayName);
                existingAccount.setAccessToken(accessToken);
                existingAccount.setConnectedAt(LocalDateTime.now());
                existingAccount.setMetrics(snapchatService.syncMetrics(existingAccount));
                existingAccount.setLastSynced(LocalDateTime.now());

                SocialAccount saved = socialAccountRepository.save(existingAccount);
                logger.info("Updated existing Snapchat account");
                return socialAccountMapper.toDTO(saved);
            } else {
                SocialAccount account = new SocialAccount();
                account.setUserId(userId);
                account.setPlatform("Snapchat");
                account.setPlatformUserId(snapchatId);
                account.setUsername(displayName);
                account.setAccessToken(accessToken);
                account.setConnectedAt(LocalDateTime.now());
                account.setMetrics(snapchatService.syncMetrics(account));
                account.setLastSynced(LocalDateTime.now());

                SocialAccount savedAccount = socialAccountRepository.save(account);
                logger.info("Successfully connected Snapchat account for user {}", userId);
                return socialAccountMapper.toDTO(savedAccount);
            }

        } catch (Exception e) {
            logger.error("Error connecting Snapchat account: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to connect Snapchat account: " + e.getMessage());
        }
    }

    public SocialAccountDTO connectTikTokAccount(String authHeader, String code, String codeVerifier) {
        logger.info("Connecting TikTok account with OAuth code and PKCE");

        String userId = authService.validateAndExtractUserId(authHeader);

        try {
            // Exchange code for access token with PKCE code_verifier
            String tokenUrl = "https://open.tiktokapis.com/v2/oauth/token/";

            // TikTok requires form-encoded POST with PKCE
            String formData = String.format(
                "client_key=%s&client_secret=%s&code=%s&grant_type=authorization_code&redirect_uri=%s&code_verifier=%s",
                URLEncoder.encode(tiktokClientKey, StandardCharsets.UTF_8),
                URLEncoder.encode(tiktokClientSecret, StandardCharsets.UTF_8),
                URLEncoder.encode(code, StandardCharsets.UTF_8),
                URLEncoder.encode(tiktokRedirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(codeVerifier != null ? codeVerifier : "", StandardCharsets.UTF_8)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> request = new HttpEntity<>(formData, headers);

            Map<String, Object> tokenResponse = restTemplate.postForObject(
                tokenUrl,
                request,
                Map.class
            );

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                logger.error("TikTok token response is null or missing access_token: {}", tokenResponse);
                throw new RuntimeException("Failed to get access token from TikTok");
            }

            String accessToken = (String) tokenResponse.get("access_token");
            String openId = (String) tokenResponse.get("open_id");
            logger.info("Successfully obtained TikTok access token, open_id: {}", openId);
            logger.debug("Full token response: {}", tokenResponse);

            // Get user's TikTok profile using the scopes we requested
            // user.info.profile fields: open_id, union_id, avatar_url, avatar_url_100, avatar_large_url, display_name, bio_description, profile_deep_link, is_verified, username
            // user.info.stats fields: follower_count, following_count, likes_count, video_count
            String profileUrl = "https://open.tiktokapis.com/v2/user/info/?fields=open_id,display_name,avatar_url,username,follower_count,following_count,likes_count,video_count";

            HttpHeaders profileHeaders = new HttpHeaders();
            profileHeaders.setBearerAuth(accessToken);

            HttpEntity<?> profileRequest = new HttpEntity<>(profileHeaders);

            Map<String, Object> profileResponse = restTemplate.exchange(
                profileUrl,
                HttpMethod.GET,
                profileRequest,
                Map.class
            ).getBody();

            logger.info("TikTok profile response: {}", profileResponse);

            // Extract user info from profile response
            // TikTok API returns: { "data": { "user": { ... } }, "error": { ... } }
            String displayName = "TikTok User";
            String username = null;
            String tiktokId = openId;
            Integer followerCount = 0;
            Integer videoCount = 0;

            if (profileResponse != null && profileResponse.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) profileResponse.get("data");
                if (data != null && data.containsKey("user")) {
                    Map<String, Object> user = (Map<String, Object>) data.get("user");
                    if (user != null) {
                        displayName = user.get("display_name") != null ? (String) user.get("display_name") : displayName;
                        username = user.get("username") != null ? (String) user.get("username") : null;
                        tiktokId = user.get("open_id") != null ? (String) user.get("open_id") : openId;

                        // Get stats if available
                        if (user.get("follower_count") != null) {
                            followerCount = ((Number) user.get("follower_count")).intValue();
                        }
                        if (user.get("video_count") != null) {
                            videoCount = ((Number) user.get("video_count")).intValue();
                        }
                    }
                }
            }

            // Use username if available, otherwise display_name
            String accountName = username != null ? username : displayName;
            logger.info("Retrieved TikTok profile for user: {} ({})", displayName, username);

            // Create metrics with real data from TikTok
            AccountMetrics metrics = new AccountMetrics();
            metrics.setConnections(followerCount);  // followers
            metrics.setPosts(videoCount);           // videos
            metrics.setEngagementScore(85);         // default score
            metrics.setPendingResponses(0);
            metrics.setNewMessages(0);

            // Check if account already connected
            SocialAccount existingAccount = socialAccountRepository
                .findByUserIdAndPlatform(userId, "tiktok")
                .orElse(null);

            if (existingAccount != null) {
                existingAccount.setPlatformUserId(tiktokId);
                existingAccount.setUsername(accountName);
                existingAccount.setAccessToken(accessToken);
                existingAccount.setConnectedAt(LocalDateTime.now());
                existingAccount.setMetrics(metrics);
                existingAccount.setLastSynced(LocalDateTime.now());

                SocialAccount saved = socialAccountRepository.save(existingAccount);
                logger.info("Updated existing TikTok account: {}", accountName);
                return socialAccountMapper.toDTO(saved);
            } else {
                SocialAccount account = new SocialAccount();
                account.setUserId(userId);
                account.setPlatform("TikTok");
                account.setPlatformUserId(tiktokId);
                account.setUsername(accountName);
                account.setAccessToken(accessToken);
                account.setConnectedAt(LocalDateTime.now());
                account.setMetrics(metrics);
                account.setLastSynced(LocalDateTime.now());

                SocialAccount savedAccount = socialAccountRepository.save(account);
                logger.info("Successfully connected TikTok account: {} for user {}", accountName, userId);
                return socialAccountMapper.toDTO(savedAccount);
            }

        } catch (Exception e) {
            logger.error("Error connecting TikTok account: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to connect TikTok account: " + e.getMessage());
        }
    }
}
