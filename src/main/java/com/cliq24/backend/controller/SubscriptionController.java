package com.cliq24.backend.controller;

import com.cliq24.backend.model.User;
import com.cliq24.backend.repository.UserRepository;
import com.cliq24.backend.service.AuthService;
import com.cliq24.backend.service.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@CrossOrigin(origins = {"http://localhost:3000", "https://cliq24.app"})
public class SubscriptionController {

    private static final Logger logger = LogManager.getLogger(SubscriptionController.class);

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final SubscriptionService subscriptionService;
    private final AuthService authService;
    private final UserRepository userRepository;

    @Autowired
    public SubscriptionController(SubscriptionService subscriptionService,
                                   AuthService authService,
                                   UserRepository userRepository) {
        this.subscriptionService = subscriptionService;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    /**
     * Create a Stripe checkout session for subscription
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession() {
        try {
            // Get userId from SecurityContext (set by JWT filter from cookie or header)
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            String userId = auth.getName();

            String successUrl = "https://cliq24.app/?subscription=success";
            String cancelUrl = "https://cliq24.app/?subscription=canceled";

            String checkoutUrl = subscriptionService.createCheckoutSession(userId, successUrl, cancelUrl);

            Map<String, String> response = new HashMap<>();
            response.put("url", checkoutUrl);

            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            logger.error("Failed to create checkout session", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating checkout session", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current user's subscription status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSubscriptionStatus() {
        try {
            // Get userId from SecurityContext (set by JWT filter from cookie or header)
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            String userId = auth.getName();
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("tier", user.getSubscriptionTier());
            response.put("status", user.getSubscriptionStatus());
            response.put("accountLimit", subscriptionService.getAccountLimit(user));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting subscription status", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Manually activate premium (for testing when webhooks don't work on localhost)
     * REMOVE THIS IN PRODUCTION!
     */
    @PostMapping("/activate-premium-test")
    public ResponseEntity<?> activatePremiumTest() {
        try {
            // Get userId from SecurityContext (set by JWT filter from cookie or header)
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            String userId = auth.getName();

            // Manually activate premium for testing
            subscriptionService.activateSubscription(userId, "test_customer_id", "test_subscription_id");

            logger.info("Manually activated premium for testing: {}", userId);
            return ResponseEntity.ok(Map.of("message", "Premium activated for testing"));
        } catch (Exception e) {
            logger.error("Error activating premium", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Stripe webhook endpoint
     * This receives events from Stripe about subscription changes
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                  @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            logger.error("Webhook signature verification failed", e);
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        logger.info("Received Stripe webhook event: {}", event.getType());

        // Handle the event
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;

        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        }

        switch (event.getType()) {
            case "checkout.session.completed":
                // Payment successful, activate subscription
                Session session = (Session) stripeObject;
                String userId = session.getMetadata().get("userId");
                String customerId = session.getCustomer();
                String subscriptionId = session.getSubscription();

                subscriptionService.activateSubscription(userId, customerId, subscriptionId);
                logger.info("Subscription activated for user: {}", userId);
                break;

            case "customer.subscription.updated":
                // Subscription status changed (e.g., payment failed, reactivated)
                com.stripe.model.Subscription subscription = (com.stripe.model.Subscription) stripeObject;
                subscriptionService.updateSubscriptionStatus(subscription.getCustomer(), subscription.getStatus());
                break;

            case "customer.subscription.deleted":
                // Subscription canceled
                com.stripe.model.Subscription deletedSub = (com.stripe.model.Subscription) stripeObject;
                subscriptionService.updateSubscriptionStatus(deletedSub.getCustomer(), "CANCELED");
                break;

            default:
                logger.info("Unhandled webhook event type: {}", event.getType());
        }

        return ResponseEntity.ok("Webhook handled");
    }
}
