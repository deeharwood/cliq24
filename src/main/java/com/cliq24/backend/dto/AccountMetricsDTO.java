package com.cliq24.backend.dto;

public class AccountMetricsDTO {
    private Integer engagementScore;
    private Integer connections;
    private Integer posts;
    private Integer pendingResponses;
    private Integer newMessages;
    private Integer recentPosts;

    // Additional metrics for specific platforms
    private Integer likes;
    private Integer comments;
    private Integer shares;
    private Integer views;
    private Double engagementRate;

    public AccountMetricsDTO() {
    }

    public AccountMetricsDTO(Integer engagementScore, Integer connections, Integer posts, Integer pendingResponses,
                            Integer newMessages, Integer recentPosts, Integer likes,
                            Integer comments, Integer shares, Integer views, Double engagementRate) {
        this.engagementScore = engagementScore;
        this.connections = connections;
        this.posts = posts;
        this.pendingResponses = pendingResponses;
        this.newMessages = newMessages;
        this.recentPosts = recentPosts;
        this.likes = likes;
        this.comments = comments;
        this.shares = shares;
        this.views = views;
        this.engagementRate = engagementRate;
    }

    public static AccountMetricsDTOBuilder builder() {
        return new AccountMetricsDTOBuilder();
    }

    public Integer getEngagementScore() {
        return engagementScore;
    }

    public void setEngagementScore(Integer engagementScore) {
        this.engagementScore = engagementScore;
    }

    public Integer getConnections() {
        return connections;
    }

    public void setConnections(Integer connections) {
        this.connections = connections;
    }

    public Integer getPosts() {
        return posts;
    }

    public void setPosts(Integer posts) {
        this.posts = posts;
    }

    public Integer getPendingResponses() {
        return pendingResponses;
    }

    public void setPendingResponses(Integer pendingResponses) {
        this.pendingResponses = pendingResponses;
    }

    public Integer getNewMessages() {
        return newMessages;
    }

    public void setNewMessages(Integer newMessages) {
        this.newMessages = newMessages;
    }

    public Integer getRecentPosts() {
        return recentPosts;
    }

    public void setRecentPosts(Integer recentPosts) {
        this.recentPosts = recentPosts;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public Integer getComments() {
        return comments;
    }

    public void setComments(Integer comments) {
        this.comments = comments;
    }

    public Integer getShares() {
        return shares;
    }

    public void setShares(Integer shares) {
        this.shares = shares;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public Double getEngagementRate() {
        return engagementRate;
    }

    public void setEngagementRate(Double engagementRate) {
        this.engagementRate = engagementRate;
    }

    public static class AccountMetricsDTOBuilder {
        private Integer engagementScore;
        private Integer connections;
        private Integer posts;
        private Integer pendingResponses;
        private Integer newMessages;
        private Integer recentPosts;
        private Integer likes;
        private Integer comments;
        private Integer shares;
        private Integer views;
        private Double engagementRate;

        AccountMetricsDTOBuilder() {
        }

        public AccountMetricsDTOBuilder engagementScore(Integer engagementScore) {
            this.engagementScore = engagementScore;
            return this;
        }

        public AccountMetricsDTOBuilder connections(Integer connections) {
            this.connections = connections;
            return this;
        }

        public AccountMetricsDTOBuilder posts(Integer posts) {
            this.posts = posts;
            return this;
        }

        public AccountMetricsDTOBuilder pendingResponses(Integer pendingResponses) {
            this.pendingResponses = pendingResponses;
            return this;
        }

        public AccountMetricsDTOBuilder newMessages(Integer newMessages) {
            this.newMessages = newMessages;
            return this;
        }

        public AccountMetricsDTOBuilder recentPosts(Integer recentPosts) {
            this.recentPosts = recentPosts;
            return this;
        }

        public AccountMetricsDTOBuilder likes(Integer likes) {
            this.likes = likes;
            return this;
        }

        public AccountMetricsDTOBuilder comments(Integer comments) {
            this.comments = comments;
            return this;
        }

        public AccountMetricsDTOBuilder shares(Integer shares) {
            this.shares = shares;
            return this;
        }

        public AccountMetricsDTOBuilder views(Integer views) {
            this.views = views;
            return this;
        }

        public AccountMetricsDTOBuilder engagementRate(Double engagementRate) {
            this.engagementRate = engagementRate;
            return this;
        }

        public AccountMetricsDTO build() {
            return new AccountMetricsDTO(engagementScore, connections, posts, pendingResponses, newMessages,
                                        recentPosts, likes, comments, shares, views, engagementRate);
        }
    }
}
