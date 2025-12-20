package com.cliq24.backend.service;

import com.cliq24.backend.model.User;
import com.cliq24.backend.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

@Service
public class SubscriptionService {

    private static final Logger logger = LogManager.getLogger(SubscriptionService.class);

    @Value("${stripe.api.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.price.id}")
    private String stripePriceId;

    private final UserRepository userRepository;
    private boolean stripeEnabled = false;

    @Autowired
    public SubscriptionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        // Only initialize Stripe if we have real credentials (not placeholder)
        if (stripeSecretKey != null && !stripeSecretKey.contains("placeholder")) {
            try {
                Stripe.apiKey = stripeSecretKey;
                stripeEnabled = true;
                logger.info("Stripe API initialized successfully");
            } catch (Exception e) {
                logger.warn("Failed to initialize Stripe API: {}", e.getMessage());
            }
        } else {
            logger.warn("Stripe API not configured - using placeholder values. Subscription features will be disabled.");
        }
    }

    private void checkStripeEnabled() {
        if (!stripeEnabled) {
            throw new RuntimeException("Stripe is not configured. Please set up Stripe credentials to use subscription features.");
        }
    }

    /**
     * Get the account limit for a user based on their subscription tier
     */
    public int getAccountLimit(User user) {
        if ("PREMIUM".equals(user.getSubscriptionTier()) &&
            "ACTIVE".equals(user.getSubscriptionStatus())) {
            return Integer.MAX_VALUE; // Unlimited for premium
        }
        return 3; // Free tier: 3 accounts
    }

    /**
     * Check if user can add more social accounts
     */
    public boolean canAddAccount(User user, int currentAccountCount) {
        int limit = getAccountLimit(user);
        return currentAccountCount < limit;
    }

    /**
     * Create a Stripe checkout session for subscription
     */
    public String createCheckoutSession(String userId, String successUrl, String cancelUrl) throws StripeException {
        checkStripeEnabled();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setPrice(stripePriceId)
                    .setQuantity(1L)
                    .build()
            )
            .putMetadata("userId", userId);

        // If user already has a Stripe customer ID, use it
        if (user.getStripeCustomerId() != null) {
            paramsBuilder.setCustomer(user.getStripeCustomerId());
        } else {
            // Create new customer
            paramsBuilder.setCustomerEmail(user.getEmail());
        }

        SessionCreateParams params = paramsBuilder.build();
        Session session = Session.create(params);

        logger.info("Created checkout session for user {}: {}", userId, session.getId());
        return session.getUrl();
    }

    /**
     * Update user subscription status after successful checkout
     */
    public void activateSubscription(String userId, String stripeCustomerId, String stripeSubscriptionId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setSubscriptionTier("PREMIUM");
        user.setSubscriptionStatus("ACTIVE");
        user.setStripeCustomerId(stripeCustomerId);
        user.setStripeSubscriptionId(stripeSubscriptionId);
        user.setSubscriptionEndsAt(null); // Active subscription has no end date

        userRepository.save(user);
        logger.info("Activated premium subscription for user {}", userId);
    }

    /**
     * Cancel user subscription
     */
    public void cancelSubscription(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setSubscriptionTier("FREE");
        user.setSubscriptionStatus("CANCELED");
        user.setSubscriptionEndsAt(LocalDateTime.now());

        userRepository.save(user);
        logger.info("Canceled subscription for user {}", userId);
    }

    /**
     * Update subscription status (called from webhook)
     */
    public void updateSubscriptionStatus(String stripeCustomerId, String status) {
        User user = userRepository.findByStripeCustomerId(stripeCustomerId)
            .orElseThrow(() -> new RuntimeException("User not found for customer: " + stripeCustomerId));

        user.setSubscriptionStatus(status);

        if ("CANCELED".equals(status) || "PAST_DUE".equals(status)) {
            user.setSubscriptionTier("FREE");
            user.setSubscriptionEndsAt(LocalDateTime.now());
        }

        userRepository.save(user);
        logger.info("Updated subscription status for user {} to {}", user.getId(), status);
    }
}
