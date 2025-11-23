package com.cliq24.backend.platforms;

import com.cliq24.backend.model.AccountMetrics;
import com.cliq24.backend.model.SocialAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class TwitterService {

    private static final Logger logger = LogManager.getLogger(TwitterService.class);

    public AccountMetrics syncMetrics(SocialAccount account) {
        // TODO: Implement Twitter API integration
        logger.warn("Twitter integration not yet implemented");
        return new AccountMetrics();
    }
}
