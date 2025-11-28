package com.cliq24.backend.controller;

import com.cliq24.backend.dto.SocialAccountDTO;
import com.cliq24.backend.service.SocialAccountService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/social-accounts")
@CrossOrigin(origins = {"http://localhost:3000", "https://localhost:3000", "https://localhost:8443", "https://cliq24.app"})
public class SocialAccountController {

    private final SocialAccountService socialAccountService;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String facebookAppId;

    @Value("${spring.security.oauth2.client.registration.facebook.scope}")
    private String facebookScope;

    @Value("${facebook.redirect.uri}")
    private String facebookRedirectUri;

    @Value("${spring.security.oauth2.client.registration.instagram.client-id}")
    private String instagramAppId;

    @Value("${spring.security.oauth2.client.registration.instagram.scope}")
    private String instagramScope;

    @Value("${instagram.redirect.uri}")
    private String instagramRedirectUri;

    @Value("${spring.security.oauth2.client.registration.linkedin.client-id}")
    private String linkedInClientId;

    @Value("${spring.security.oauth2.client.registration.linkedin.scope}")
    private String linkedInScope;

    @Value("${linkedin.redirect.uri}")
    private String linkedInRedirectUri;

    @Value("${spring.security.oauth2.client.registration.snapchat.client-id}")
    private String snapchatClientId;

    @Value("${spring.security.oauth2.client.registration.snapchat.scope}")
    private String snapchatScope;

    @Value("${snapchat.redirect.uri}")
    private String snapchatRedirectUri;

    @Value("${tiktok.client.key}")
    private String tiktokClientKey;

    @Value("${tiktok.client.secret}")
    private String tiktokClientSecret;

    @Value("${tiktok.redirect.uri}")
    private String tiktokRedirectUri;

    @Value("${spring.security.oauth2.client.registration.twitter.client-id}")
    private String twitterClientId;

    @Value("${spring.security.oauth2.client.registration.twitter.scope}")
    private String twitterScope;

    @Value("${twitter.redirect.uri}")
    private String twitterRedirectUri;

    @Value("${spring.security.oauth2.client.registration.youtube.client-id}")
    private String youtubeClientId;

    @Value("${spring.security.oauth2.client.registration.youtube.scope}")
    private String youtubeScope;

    @Value("${youtube.redirect.uri}")
    private String youtubeRedirectUri;

    // Store PKCE code verifiers temporarily (in production, use Redis or session)
    private static final ConcurrentHashMap<String, String> pkceVerifiers = new ConcurrentHashMap<>();

    @Autowired
    public SocialAccountController(SocialAccountService socialAccountService) {
        this.socialAccountService = socialAccountService;
    }

    // PKCE helper methods
    private String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(bytes);
            byte[] digest = messageDigest.digest();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate code challenge", e);
        }
    }

    public static String getCodeVerifier(String state) {
        return pkceVerifiers.remove(state);
    }
    
    @GetMapping
    public ResponseEntity<List<SocialAccountDTO>> getUserAccounts(
            @RequestHeader("Authorization") String token) {
        List<SocialAccountDTO> accounts = socialAccountService.getUserAccounts(token);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Initiate Facebook OAuth connection
     */
    @GetMapping("/Facebook")
    public void initiateFacebookConnection(
            @RequestParam(required = false) String token,
            HttpServletResponse response) throws IOException {

        if (token == null || token.isEmpty()) {
            response.sendRedirect("/?error=missing_token");
            return;
        }

        // Store token in session or pass as state parameter
        String state = token; // Pass JWT as state (already without Bearer prefix from frontend)

        String authUrl = String.format(
            "https://www.facebook.com/v18.0/dialog/oauth?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
            facebookAppId,
            URLEncoder.encode(facebookRedirectUri, StandardCharsets.UTF_8),
            URLEncoder.encode(facebookScope, StandardCharsets.UTF_8),
            URLEncoder.encode(state, StandardCharsets.UTF_8)
        );

        response.sendRedirect(authUrl);
    }

    /**
     * Facebook OAuth callback
     */
    @GetMapping("/facebook/callback")
    public void facebookCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response) throws IOException {

        try {
            // State contains the JWT token
            String token = "Bearer " + state;

            // Exchange code for access token and create account
            SocialAccountDTO account = socialAccountService.connectFacebookAccount(token, code);

            // Redirect back to frontend with success
            response.sendRedirect("/?facebook_connected=true");
        } catch (Exception e) {
            // Redirect back with error
            response.sendRedirect("/?facebook_error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Initiate Instagram OAuth connection
     */
    @GetMapping("/Instagram")
    public void initiateInstagramConnection(
            @RequestParam(required = false) String token,
            HttpServletResponse response) throws IOException {

        if (token == null || token.isEmpty()) {
            response.sendRedirect("/?error=missing_token");
            return;
        }

        // Store token in session or pass as state parameter
        String state = token; // Pass JWT as state (already without Bearer prefix from frontend)

        String authUrl = String.format(
            "https://www.facebook.com/v18.0/dialog/oauth?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
            instagramAppId,
            URLEncoder.encode(instagramRedirectUri, StandardCharsets.UTF_8),
            URLEncoder.encode(instagramScope, StandardCharsets.UTF_8),
            URLEncoder.encode(state, StandardCharsets.UTF_8)
        );

        response.sendRedirect(authUrl);
    }

    /**
     * Instagram OAuth callback
     */
    @GetMapping("/instagram/callback")
    public void instagramCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response) throws IOException {

        try {
            // State contains the JWT token
            String token = "Bearer " + state;

            // Exchange code for access token and create account
            SocialAccountDTO account = socialAccountService.connectInstagramAccount(token, code);

            // Redirect back to frontend with success
            response.sendRedirect("/?instagram_connected=true");
        } catch (Exception e) {
            // Redirect back with error
            response.sendRedirect("/?instagram_error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Initiate LinkedIn OAuth connection
     */
    @GetMapping("/LinkedIn")
    public void initiateLinkedInConnection(
            @RequestParam(required = false) String token,
            HttpServletResponse response) throws IOException {

        if (token == null || token.isEmpty()) {
            response.sendRedirect("/?error=missing_token");
            return;
        }

        String state = token;

        String authUrl = String.format(
            "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s",
            linkedInClientId,
            URLEncoder.encode(linkedInRedirectUri, StandardCharsets.UTF_8),
            URLEncoder.encode(linkedInScope, StandardCharsets.UTF_8),
            URLEncoder.encode(state, StandardCharsets.UTF_8)
        );

        response.sendRedirect(authUrl);
    }

    /**
     * LinkedIn OAuth callback
     */
    @GetMapping("/linkedin/callback")
    public void linkedInCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response) throws IOException {

        try {
            String token = "Bearer " + state;
            SocialAccountDTO account = socialAccountService.connectLinkedInAccount(token, code);
            response.sendRedirect("/?linkedin_connected=true");
        } catch (Exception e) {
            response.sendRedirect("/?linkedin_error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Initiate Snapchat OAuth connection with PKCE
     */
    @GetMapping("/Snapchat")
    public void initiateSnapchatConnection(
            @RequestParam(required = false) String token,
            HttpServletResponse response) throws IOException {

        if (token == null || token.isEmpty()) {
            response.sendRedirect("/?error=missing_token");
            return;
        }

        String state = token;

        // Generate PKCE code verifier and challenge
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        // Store code verifier for later use in token exchange
        pkceVerifiers.put(state, codeVerifier);

        // Snapchat uses Snap Kit Login Kit with PKCE (server-side)
        String scopeSpaceSeparated = snapchatScope.replace(",", " ");
        String authUrl = String.format(
            "https://accounts.snapchat.com/accounts/oauth2/auth?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s&code_challenge=%s&code_challenge_method=S256",
            snapchatClientId,
            URLEncoder.encode(snapchatRedirectUri, StandardCharsets.UTF_8),
            URLEncoder.encode(scopeSpaceSeparated, StandardCharsets.UTF_8),
            URLEncoder.encode(state, StandardCharsets.UTF_8),
            URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8)
        );

        response.sendRedirect(authUrl);
    }

    /**
     * Snapchat OAuth callback with PKCE
     */
    @GetMapping("/snapchat/callback")
    public void snapchatCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response) throws IOException {

        try {
            String token = "Bearer " + state;
            // Retrieve the code verifier for PKCE
            String codeVerifier = pkceVerifiers.remove(state);
            SocialAccountDTO account = socialAccountService.connectSnapchatAccount(token, code, codeVerifier);
            response.sendRedirect("/?snapchat_connected=true");
        } catch (Exception e) {
            response.sendRedirect("/?snapchat_error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Initiate TikTok OAuth connection with PKCE
     */
    @GetMapping("/TikTok")
    public void initiateTikTokConnection(
            @RequestParam(required = false) String token,
            HttpServletResponse response) throws IOException {

        if (token == null || token.isEmpty()) {
            response.sendRedirect("/?error=missing_token");
            return;
        }

        String state = token;

        // Generate PKCE code verifier and challenge (TikTok requires PKCE)
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        // Store code verifier for later use in token exchange
        pkceVerifiers.put(state, codeVerifier);

        // TikTok OAuth2 authorization URL
        // Scopes: user.info.profile for profile data, user.info.stats for follower/video counts
        String scope = "user.info.profile,user.info.stats";
        String authUrl = String.format(
            "https://www.tiktok.com/v2/auth/authorize/?client_key=%s&redirect_uri=%s&scope=%s&response_type=code&state=%s&code_challenge=%s&code_challenge_method=S256",
            tiktokClientKey,
            URLEncoder.encode(tiktokRedirectUri, StandardCharsets.UTF_8),
            URLEncoder.encode(scope, StandardCharsets.UTF_8),
            URLEncoder.encode(state, StandardCharsets.UTF_8),
            URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8)
        );

        response.sendRedirect(authUrl);
    }

    /**
     * TikTok OAuth callback with PKCE
     */
    @GetMapping("/tiktok/callback")
    public void tiktokCallback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            HttpServletResponse response) throws IOException {

        if (error != null) {
            response.sendRedirect("/?tiktok_error=" + URLEncoder.encode(error_description != null ? error_description : error, StandardCharsets.UTF_8));
            return;
        }

        try {
            String token = "Bearer " + state;
            // Retrieve the code verifier for PKCE
            String codeVerifier = pkceVerifiers.remove(state);
            SocialAccountDTO account = socialAccountService.connectTikTokAccount(token, code, codeVerifier);
            response.sendRedirect("/?tiktok_connected=true");
        } catch (Exception e) {
            response.sendRedirect("/?tiktok_error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Initiate platform connection (for demo/testing - other platforms)
     */
    @GetMapping("/{platform}")
    public ResponseEntity<SocialAccountDTO> initiateConnection(
            @PathVariable String platform,
            @RequestHeader("Authorization") String token) {
        // For non-Facebook platforms, use demo mode for now
        SocialAccountDTO account = socialAccountService.connectAccount(platform, token, "demo_code");
        return ResponseEntity.ok(account);
    }

    @PostMapping("/{platform}")
    public ResponseEntity<SocialAccountDTO> connectAccount(
            @PathVariable String platform,
            @RequestHeader("Authorization") String token,
            @RequestParam String code) {
        SocialAccountDTO account = socialAccountService.connectAccount(platform, token, code);
        return ResponseEntity.ok(account);
    }
    
    @DeleteMapping("/{accountId}")
    public ResponseEntity<?> disconnectAccount(
            @PathVariable String accountId,
            @RequestHeader("Authorization") String token) {
        socialAccountService.disconnectAccount(accountId, token);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{accountId}/sync")
    public ResponseEntity<SocialAccountDTO> syncMetrics(
            @PathVariable String accountId,
            @RequestHeader("Authorization") String token) {
        SocialAccountDTO account = socialAccountService.syncMetrics(accountId, token);
        return ResponseEntity.ok(account);
    }

    /**
     * Initiate Twitter/X OAuth connection with PKCE
     */
    @GetMapping("/Twitter")
    public void initiateTwitterConnection(
            @RequestParam(required = false) String token,
            HttpServletResponse response) throws IOException {

        if (token == null || token.isEmpty()) {
            response.sendRedirect("https://cliq24.app/?error=missing_token");
            return;
        }

        // Generate PKCE parameters
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        String state = token; // Use JWT token as state

        // Store code verifier for callback
        pkceVerifiers.put(state, codeVerifier);

        // Twitter expects space-separated scopes, convert from comma-separated
        String spaceSeparatedScopes = twitterScope.replace(",", " ");

        String authUrl = String.format(
            "https://twitter.com/i/oauth2/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s&code_challenge=%s&code_challenge_method=S256",
            URLEncoder.encode(twitterClientId, StandardCharsets.UTF_8),
            URLEncoder.encode(twitterRedirectUri, StandardCharsets.UTF_8),
            URLEncoder.encode(spaceSeparatedScopes, StandardCharsets.UTF_8),
            URLEncoder.encode(state, StandardCharsets.UTF_8),
            URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8)
        );

        response.sendRedirect(authUrl);
    }

    /**
     * Twitter/X OAuth callback
     */
    @GetMapping("/twitter/callback")
    public void twitterCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response) throws IOException {

        try {
            // State contains the JWT token
            String token = "Bearer " + state;

            // Get code verifier from storage
            String codeVerifier = getCodeVerifier(state);
            if (codeVerifier == null) {
                response.sendRedirect("https://cliq24.app/?twitter_error=invalid_state");
                return;
            }

            // Exchange code for access token and create account
            SocialAccountDTO account = socialAccountService.connectTwitterAccount(token, code, codeVerifier);

            // Redirect back to frontend with success
            response.sendRedirect("https://cliq24.app/?twitter_connected=true");
        } catch (Exception e) {
            // Redirect back with error
            response.sendRedirect("https://cliq24.app/?twitter_error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Initiate YouTube OAuth connection (uses Google OAuth with YouTube scopes)
     */
    @GetMapping("/YouTube")
    public void initiateYouTubeConnection(
            @RequestParam(required = false) String token,
            HttpServletResponse response) throws IOException {

        if (token == null || token.isEmpty()) {
            response.sendRedirect("https://cliq24.app/?error=missing_token");
            return;
        }

        // Use token as state parameter
        String state = token;

        // Google expects space-separated scopes, convert from comma-separated
        String spaceSeparatedScopes = youtubeScope.replace(",", " ");

        String authUrl = String.format(
            "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s&access_type=offline&prompt=consent",
            URLEncoder.encode(youtubeClientId, StandardCharsets.UTF_8),
            URLEncoder.encode(youtubeRedirectUri, StandardCharsets.UTF_8),
            URLEncoder.encode(spaceSeparatedScopes, StandardCharsets.UTF_8),
            URLEncoder.encode(state, StandardCharsets.UTF_8)
        );

        response.sendRedirect(authUrl);
    }

    /**
     * YouTube OAuth callback
     */
    @GetMapping("/youtube/callback")
    public void youtubeCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response) throws IOException {

        try {
            // State contains the JWT token
            String token = "Bearer " + state;

            // Exchange code for access token and create account
            SocialAccountDTO account = socialAccountService.connectYouTubeAccount(token, code);

            // Redirect back to frontend with success
            response.sendRedirect("https://cliq24.app/?youtube_connected=true");
        } catch (Exception e) {
            // Redirect back with error
            response.sendRedirect("https://cliq24.app/?youtube_error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
