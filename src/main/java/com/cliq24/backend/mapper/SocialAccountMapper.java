package com.cliq24.backend.mapper;

import com.cliq24.backend.dto.AccountMetricsDTO;
import com.cliq24.backend.dto.SocialAccountDTO;
import com.cliq24.backend.model.AccountMetrics;
import com.cliq24.backend.model.SocialAccount;
import com.cliq24.backend.util.ScoreCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SocialAccountMapper {

    private final ScoreCalculator scoreCalculator;

    @Autowired
    public SocialAccountMapper(ScoreCalculator scoreCalculator) {
        this.scoreCalculator = scoreCalculator;
    }
    
    public SocialAccountDTO toDTO(SocialAccount account) {
        if (account == null) {
            return null;
        }
        
        AccountMetricsDTO metricsDTO = toMetricsDTO(account.getMetrics());
        Integer score = scoreCalculator.calculateEngagementScore(account.getMetrics());
        
        return SocialAccountDTO.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .platform(account.getPlatform())
                .platformUserId(account.getPlatformUserId())
                .username(account.getUsername())
                .metrics(metricsDTO)
                .connectedAt(account.getConnectedAt())
                .lastSynced(account.getLastSynced())
                .engagementScore(score)
                .isActive(true)
                .needsReconnection(isTokenExpired(account))
                .build();
    }
    
    public AccountMetricsDTO toMetricsDTO(AccountMetrics metrics) {
        if (metrics == null) {
            return new AccountMetricsDTO();
        }

        return AccountMetricsDTO.builder()
                .engagementScore(metrics.getEngagementScore())
                .connections(metrics.getConnections())
                .posts(metrics.getPosts())
                .pendingResponses(metrics.getPendingResponses())
                .newMessages(metrics.getNewMessages())
                .build();
    }
    
    public AccountMetrics toMetricsEntity(AccountMetricsDTO dto) {
        if (dto == null) {
            return new AccountMetrics();
        }
        
        AccountMetrics metrics = new AccountMetrics();
        metrics.setConnections(dto.getConnections());
        metrics.setPosts(dto.getPosts());
        metrics.setPendingResponses(dto.getPendingResponses());
        metrics.setNewMessages(dto.getNewMessages());
        
        return metrics;
    }
    
    private boolean isTokenExpired(SocialAccount account) {
        if (account.getTokenExpiresAt() == null) {
            return false;
        }
        return account.getTokenExpiresAt().isBefore(java.time.LocalDateTime.now());
    }
}
