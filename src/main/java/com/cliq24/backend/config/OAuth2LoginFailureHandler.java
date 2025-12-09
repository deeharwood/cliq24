package com.cliq24.backend.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    public OAuth2LoginFailureHandler() {
        setDefaultFailureUrl("/?error=oauth_failed");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException exception) throws IOException, ServletException {
        logger.error("OAuth2 login failed!", exception);
        logger.error("Exception type: {}", exception.getClass().getName());
        logger.error("Exception message: {}", exception.getMessage());
        if (exception.getCause() != null) {
            logger.error("Cause: {}", exception.getCause().getMessage());
        }

        super.onAuthenticationFailure(request, response, exception);
    }
}
