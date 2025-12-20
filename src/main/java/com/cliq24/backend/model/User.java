package com.cliq24.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "users")
public class User {
    @Id
    private String id;

    private String googleId;
    private String email;
    private String name;
    private String picture;
    private String passwordHash; // For email/password authentication
    private LocalDateTime createdAt = LocalDateTime.now();

    // Subscription fields
    private String subscriptionTier = "FREE"; // FREE, PREMIUM
    private String subscriptionStatus = "ACTIVE"; // ACTIVE, CANCELED, PAST_DUE, INCOMPLETE
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private LocalDateTime subscriptionEndsAt;

    // User preferences for personalized dashboard
    // Map of platform -> list of goals (e.g., "facebook" -> ["growth", "engagement"])
    private Map<String, List<String>> platformGoals = new HashMap<>();

    // Additional platform-specific settings (for future use)
    private Map<String, Object> platformPreferences = new HashMap<>();

    public User() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSubscriptionTier() {
        return subscriptionTier;
    }

    public void setSubscriptionTier(String subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }

    public LocalDateTime getSubscriptionEndsAt() {
        return subscriptionEndsAt;
    }

    public void setSubscriptionEndsAt(LocalDateTime subscriptionEndsAt) {
        this.subscriptionEndsAt = subscriptionEndsAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Map<String, List<String>> getPlatformGoals() {
        return platformGoals;
    }

    public void setPlatformGoals(Map<String, List<String>> platformGoals) {
        this.platformGoals = platformGoals;
    }

    public Map<String, Object> getPlatformPreferences() {
        return platformPreferences;
    }

    public void setPlatformPreferences(Map<String, Object> platformPreferences) {
        this.platformPreferences = platformPreferences;
    }
}




