package com.cliq24.backend.dto;

import java.time.LocalDateTime;

public class SocialAccountDTO {
    private String id;
    private String userId;
    private String platform;
    private String platformUserId;
    private String username;
    private String profilePicture;

    // Metrics
    private AccountMetricsDTO metrics;

    // Timestamps
    private LocalDateTime connectedAt;
    private LocalDateTime lastSynced;

    // Calculated score
    private Integer engagementScore;

    // Status
    private Boolean isActive;
    private Boolean needsReconnection;

    public SocialAccountDTO() {
    }

    public SocialAccountDTO(String id, String userId, String platform, String platformUserId,
                           String username, String profilePicture, AccountMetricsDTO metrics,
                           LocalDateTime connectedAt, LocalDateTime lastSynced,
                           Integer engagementScore, Boolean isActive, Boolean needsReconnection) {
        this.id = id;
        this.userId = userId;
        this.platform = platform;
        this.platformUserId = platformUserId;
        this.username = username;
        this.profilePicture = profilePicture;
        this.metrics = metrics;
        this.connectedAt = connectedAt;
        this.lastSynced = lastSynced;
        this.engagementScore = engagementScore;
        this.isActive = isActive;
        this.needsReconnection = needsReconnection;
    }

    public static SocialAccountDTOBuilder builder() {
        return new SocialAccountDTOBuilder();
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

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public AccountMetricsDTO getMetrics() {
        return metrics;
    }

    public void setMetrics(AccountMetricsDTO metrics) {
        this.metrics = metrics;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }

    public LocalDateTime getLastSynced() {
        return lastSynced;
    }

    public void setLastSynced(LocalDateTime lastSynced) {
        this.lastSynced = lastSynced;
    }

    public Integer getEngagementScore() {
        return engagementScore;
    }

    public void setEngagementScore(Integer engagementScore) {
        this.engagementScore = engagementScore;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getNeedsReconnection() {
        return needsReconnection;
    }

    public void setNeedsReconnection(Boolean needsReconnection) {
        this.needsReconnection = needsReconnection;
    }

    public static class SocialAccountDTOBuilder {
        private String id;
        private String userId;
        private String platform;
        private String platformUserId;
        private String username;
        private String profilePicture;
        private AccountMetricsDTO metrics;
        private LocalDateTime connectedAt;
        private LocalDateTime lastSynced;
        private Integer engagementScore;
        private Boolean isActive;
        private Boolean needsReconnection;

        SocialAccountDTOBuilder() {
        }

        public SocialAccountDTOBuilder id(String id) {
            this.id = id;
            return this;
        }

        public SocialAccountDTOBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public SocialAccountDTOBuilder platform(String platform) {
            this.platform = platform;
            return this;
        }

        public SocialAccountDTOBuilder platformUserId(String platformUserId) {
            this.platformUserId = platformUserId;
            return this;
        }

        public SocialAccountDTOBuilder username(String username) {
            this.username = username;
            return this;
        }

        public SocialAccountDTOBuilder profilePicture(String profilePicture) {
            this.profilePicture = profilePicture;
            return this;
        }

        public SocialAccountDTOBuilder metrics(AccountMetricsDTO metrics) {
            this.metrics = metrics;
            return this;
        }

        public SocialAccountDTOBuilder connectedAt(LocalDateTime connectedAt) {
            this.connectedAt = connectedAt;
            return this;
        }

        public SocialAccountDTOBuilder lastSynced(LocalDateTime lastSynced) {
            this.lastSynced = lastSynced;
            return this;
        }

        public SocialAccountDTOBuilder engagementScore(Integer engagementScore) {
            this.engagementScore = engagementScore;
            return this;
        }

        public SocialAccountDTOBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public SocialAccountDTOBuilder needsReconnection(Boolean needsReconnection) {
            this.needsReconnection = needsReconnection;
            return this;
        }

        public SocialAccountDTO build() {
            return new SocialAccountDTO(id, userId, platform, platformUserId, username,
                                       profilePicture, metrics, connectedAt, lastSynced,
                                       engagementScore, isActive, needsReconnection);
        }
    }
}
