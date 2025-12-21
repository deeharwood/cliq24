package com.cliq24.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "social_accounts")
public class SocialAccount {
    @Id
    private String id;

    private String userId;
    private String platform;
    private String platformUserId;
    private String username;
    private String accountName; // Display name (e.g., "John Doe" vs username "@johndoe")
    private String accessToken; // Will be encrypted
    private String refreshToken; // Will be encrypted
    private LocalDateTime tokenExpiresAt;

    private AccountMetrics metrics;
    private LocalDateTime lastSynced;
    private LocalDateTime connectedAt = LocalDateTime.now();

    // LinkedIn-specific fields
    private String accountType; // "personal" or "company" (for LinkedIn)
    private Map<String, Integer> manualMetrics = new HashMap<>(); // User-provided metrics for personal accounts

    public SocialAccount() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPlatformUserId() {
        return platformUserId;
    }

    public void setPlatformUserId(String platformUserId) {
        this.platformUserId = platformUserId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public LocalDateTime getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public AccountMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(AccountMetrics metrics) {
        this.metrics = metrics;
    }

    public LocalDateTime getLastSynced() {
        return lastSynced;
    }

    public void setLastSynced(LocalDateTime lastSynced) {
        this.lastSynced = lastSynced;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Map<String, Integer> getManualMetrics() {
        return manualMetrics;
    }

    public void setManualMetrics(Map<String, Integer> manualMetrics) {
        this.manualMetrics = manualMetrics;
    }
}

