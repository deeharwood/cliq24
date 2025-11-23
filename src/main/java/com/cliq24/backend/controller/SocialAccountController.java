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
@CrossOrigin(origins = {"http://localhost:3000", "https://cliq24.app"})
public class SocialAccountController {

    private final SocialAccountService socialAccountService;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String facebookAppId;

    @Value("${spring.security.oauth2.client.registration.facebook.scope}")
    private String facebookScope;

    @Value("${spring.security.oauth2.client.registration.instagram.client-id}")
    private String instagramAppId;

    @Value("${spring.security.oauth2.client.registration.instagram.scope}")
    private String instagramScope;

    @Value("${spring.security.oauth2.client.registration.linkedin.client-id}")
    private String linkedInClientId;

    @Value("${spring.security.oauth2.client.registration.linkedin.scope}")
    private String linkedInScope;

    @Value("${spring.security.oauth2.client.registration.snapchat.client-id}")
    private String snapchatClientId;

    @Value("${spring.security.oauth2.client.registration.snapchat.scope}")
    private String snapchatScope;

    @Value("${snapchat.redirect.uri}")
    private String snapchatRedirectUri;

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
        String redirectUri = "http://localhost:8080/api/social-accounts/facebook/callback";
        String state = token; // Pass JWT as state (already without Bearer prefix from frontend)

        String authUrl = String.format(
            "https://www.facebook.com/v18.0/dialog/oauth?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
            facebookAppId,
            URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
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
        String redirectUri = "http://localhost:8080/api/social-accounts/instagram/callback";
        String state = token; // Pass JWT as state (already without Bearer prefix from frontend)

        String authUrl = String.format(
            "https://www.facebook.com/v18.0/dialog/oauth?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
            instagramAppId,
            URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
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

        String redirectUri = "http://localhost:8080/api/social-accounts/linkedin/callback";
        String state = token;

        String authUrl = String.format(
            "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&state=%s",
            linkedInClientId,
            URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
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
}
