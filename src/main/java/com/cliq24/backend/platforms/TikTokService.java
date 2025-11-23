package com.cliq24.backend.platforms;

import com.cliq24.backend.model.AccountMetrics;
import com.cliq24.backend.model.SocialAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class TikTokService {

    private static final Logger logger = LogManager.getLogger(TikTokService.class);

    public AccountMetrics syncMetrics(SocialAccount account) {
        // TODO: Implement TikTok API integration
        logger.warn("TikTok integration not yet implemented");
        return new AccountMetrics();
    }
}
