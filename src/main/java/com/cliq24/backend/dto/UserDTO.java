package com.cliq24.backend.dto;

import java.time.LocalDateTime;

public class UserDTO {
    private String id;
    private String googleId;
    private String email;
    private String name;
    private String picture;
    private String token; // JWT token for authentication
    private LocalDateTime createdAt;
    private Integer connectedAccountsCount;
    private Integer overallScore;
    private String userType; // COMPANY or END_USER

    public UserDTO() {
    }

    public UserDTO(String id, String googleId, String email, String name, String picture,
                   String token, LocalDateTime createdAt, Integer connectedAccountsCount,
                   Integer overallScore, String userType) {
        this.id = id;
        this.googleId = googleId;
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.token = token;
        this.createdAt = createdAt;
        this.connectedAccountsCount = connectedAccountsCount;
        this.overallScore = overallScore;
        this.userType = userType;
    }

    public static UserDTOBuilder builder() {
        return new UserDTOBuilder();
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getConnectedAccountsCount() {
        return connectedAccountsCount;
    }

    public void setConnectedAccountsCount(Integer connectedAccountsCount) {
        this.connectedAccountsCount = connectedAccountsCount;
    }

    public Integer getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Integer overallScore) {
        this.overallScore = overallScore;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public static class UserDTOBuilder {
        private String id;
        private String googleId;
        private String email;
        private String name;
        private String picture;
        private String token;
        private LocalDateTime createdAt;
        private Integer connectedAccountsCount;
        private Integer overallScore;
        private String userType;

        UserDTOBuilder() {
        }

        public UserDTOBuilder id(String id) {
            this.id = id;
            return this;
        }

        public UserDTOBuilder googleId(String googleId) {
            this.googleId = googleId;
            return this;
        }

        public UserDTOBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserDTOBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserDTOBuilder picture(String picture) {
            this.picture = picture;
            return this;
        }

        public UserDTOBuilder token(String token) {
            this.token = token;
            return this;
        }

        public UserDTOBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserDTOBuilder connectedAccountsCount(Integer connectedAccountsCount) {
            this.connectedAccountsCount = connectedAccountsCount;
            return this;
        }

        public UserDTOBuilder overallScore(Integer overallScore) {
            this.overallScore = overallScore;
            return this;
        }

        public UserDTOBuilder userType(String userType) {
            this.userType = userType;
            return this;
        }

        public UserDTO build() {
            return new UserDTO(id, googleId, email, name, picture, token, createdAt,
                             connectedAccountsCount, overallScore, userType);
        }
    }
}

