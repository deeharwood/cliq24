package com.cliq24.backend.model;

public class AccountMetrics {
    private Integer engagementScore = 0;
    private Integer connections = 0;
    private Integer posts = 0;
    private Integer pendingResponses = 0;
    private Integer newMessages = 0;

    public AccountMetrics() {
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
}
