package com.cliq24.backend.util;

import com.cliq24.backend.model.AccountMetrics;
import org.springframework.stereotype.Component;

@Component
public class ScoreCalculator {
    
    private static final double CONNECTIONS_WEIGHT = 0.3;
    private static final double POSTS_WEIGHT = 0.5;
    private static final double RESPONSE_WEIGHT = 0.2;
    
    /**
     * Calculate engagement score (0-100) based on account metrics
     */
    public Integer calculateEngagementScore(AccountMetrics metrics) {
        if (metrics == null) {
            return 0;
        }
        
        int connections = metrics.getConnections() != null ? metrics.getConnections() : 0;
        int posts = metrics.getPosts() != null ? metrics.getPosts() : 0;
        int pendingResponses = metrics.getPendingResponses() != null ? metrics.getPendingResponses() : 0;
        
        // Connection score: 0-100 based on connections (max at 10,000)
        double connectionScore = Math.min(connections / 100.0, 100.0) * CONNECTIONS_WEIGHT;
        
        // Post score: 0-100 based on posts (each post worth 2 points, max 50 posts = 100)
        double postScore = Math.min(posts * 2.0, 100.0) * POSTS_WEIGHT;
        
        // Response score: Higher pending responses lower the score
        double responseScore = Math.max(0, 100.0 - (pendingResponses * 2.0)) * RESPONSE_WEIGHT;
        
        int totalScore = (int) Math.round(connectionScore + postScore + responseScore);
        
        // Ensure score is between 0 and 100
        return Math.max(0, Math.min(100, totalScore));
    }
    
    /**
     * Calculate overall score across multiple accounts
     */
    public Integer calculateOverallScore(java.util.List<AccountMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return 0;
        }
        
        double totalScore = metricsList.stream()
                .mapToInt(this::calculateEngagementScore)
                .average()
                .orElse(0.0);
        
        return (int) Math.round(totalScore);
    }
    
    /**
     * Get score label based on score value
     */
    public String getScoreLabel(Integer score) {
        if (score >= 80) return "Crushing It! 🔥";
        if (score >= 60) return "Doing Well 👍";
        if (score >= 40) return "Needs Attention ⚠️";
        return "Falling Behind 📉";
    }
    
    /**
     * Get score color class for UI
     */
    public String getScoreColor(Integer score) {
        if (score >= 80) return "green";
        if (score >= 60) return "blue";
        if (score >= 40) return "yellow";
        return "red";
    }
}

