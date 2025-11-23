package com.cliq24.backend.platforms;

import com.cliq24.backend.model.AccountMetrics;
import com.cliq24.backend.model.SocialAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class YouTubeService {

    private static final Logger logger = LogManager.getLogger(YouTubeService.class);

    public AccountMetrics syncMetrics(SocialAccount account) {
        // TODO: Implement YouTube API integration
        logger.warn("YouTube integration not yet implemented");
        return new AccountMetrics();
    }
}
