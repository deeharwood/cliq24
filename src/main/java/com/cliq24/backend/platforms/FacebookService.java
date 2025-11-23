package com.cliq24.backend.platforms;

import com.cliq24.backend.model.AccountMetrics;
import com.cliq24.backend.model.SocialAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class FacebookService {

    private static final Logger logger = LogManager.getLogger(FacebookService.class);

    public AccountMetrics syncMetrics(SocialAccount account) {
        // TODO: Implement real Facebook API integration
        logger.info("Using demo metrics for Facebook account: {}", account.getUsername());

        // Return demo metrics for now
        AccountMetrics metrics = new AccountMetrics();
        metrics.setEngagementScore(85); // Engagement score (0-100)
        metrics.setConnections(1250); // Friends/followers
        metrics.setPosts(89); // Total posts
        metrics.setPendingResponses(5); // Pending comments/messages
        metrics.setNewMessages(12); // Unread messages

        return metrics;
    }
}

