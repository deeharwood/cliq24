package com.cliq24.backend.service;

import com.cliq24.backend.model.User;
import com.cliq24.backend.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PreferencesService {

    private static final Logger logger = LogManager.getLogger(PreferencesService.class);

    private final UserRepository userRepository;

    // Available goals for each platform
    public static final List<String> AVAILABLE_GOALS = Arrays.asList(
        "growth",       // Growing followers/audience
        "engagement",   // Increasing likes/comments/shares
        "traffic",      // Driving website clicks/conversions
        "response",     // Improving response time/customer service
        "content",      // Tracking content performance
        "comprehensive" // All metrics
    );

    @Autowired
    public PreferencesService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get user's goals for a specific platform
     */
    public List<String> getPlatformGoals(String userId, String platform) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, List<String>> platformGoals = user.getPlatformGoals();
        if (platformGoals == null || !platformGoals.containsKey(platform.toLowerCase())) {
            // Return default: comprehensive view if no preferences set
            return Arrays.asList("comprehensive");
        }

        return platformGoals.get(platform.toLowerCase());
    }

    /**
     * Get all platform goals for a user
     */
    public Map<String, List<String>> getAllPlatformGoals(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, List<String>> platformGoals = user.getPlatformGoals();
        return platformGoals != null ? platformGoals : new HashMap<>();
    }

    /**
     * Set goals for a specific platform
     */
    public void setPlatformGoals(String userId, String platform, List<String> goals) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate goals
        for (String goal : goals) {
            if (!AVAILABLE_GOALS.contains(goal.toLowerCase())) {
                throw new IllegalArgumentException("Invalid goal: " + goal +
                    ". Available goals: " + AVAILABLE_GOALS);
            }
        }

        Map<String, List<String>> platformGoals = user.getPlatformGoals();
        if (platformGoals == null) {
            platformGoals = new HashMap<>();
            user.setPlatformGoals(platformGoals);
        }

        platformGoals.put(platform.toLowerCase(), goals);
        userRepository.save(user);

        logger.info("Updated goals for user {} platform {}: {}", userId, platform, goals);
    }

    /**
     * Update multiple platforms at once
     */
    public void setAllPlatformGoals(String userId, Map<String, List<String>> allGoals) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate all goals
        for (Map.Entry<String, List<String>> entry : allGoals.entrySet()) {
            for (String goal : entry.getValue()) {
                if (!AVAILABLE_GOALS.contains(goal.toLowerCase())) {
                    throw new IllegalArgumentException("Invalid goal: " + goal +
                        " for platform: " + entry.getKey());
                }
            }
        }

        // Convert all platform names to lowercase for consistency
        Map<String, List<String>> normalizedGoals = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : allGoals.entrySet()) {
            normalizedGoals.put(entry.getKey().toLowerCase(), entry.getValue());
        }

        user.setPlatformGoals(normalizedGoals);
        userRepository.save(user);

        logger.info("Updated all platform goals for user {}: {}", userId, normalizedGoals);
    }

    /**
     * Check if user has set preferences for a platform
     */
    public boolean hasPreferences(String userId, String platform) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, List<String>> platformGoals = user.getPlatformGoals();
        return platformGoals != null && platformGoals.containsKey(platform.toLowerCase());
    }

    /**
     * Get metric priority based on user goals
     * Returns which metrics should be highlighted for this user/platform
     */
    public Set<String> getMetricPriorities(String userId, String platform) {
        List<String> goals = getPlatformGoals(userId, platform);
        Set<String> priorities = new HashSet<>();

        for (String goal : goals) {
            switch (goal.toLowerCase()) {
                case "growth":
                    priorities.addAll(Arrays.asList("followers", "reach", "impressions", "follower_growth"));
                    break;
                case "engagement":
                    priorities.addAll(Arrays.asList("likes", "comments", "shares", "engagement_rate", "reactions"));
                    break;
                case "traffic":
                    priorities.addAll(Arrays.asList("clicks", "link_clicks", "website_visits", "ctr"));
                    break;
                case "response":
                    priorities.addAll(Arrays.asList("response_time", "unanswered_messages", "pending_comments"));
                    break;
                case "content":
                    priorities.addAll(Arrays.asList("post_performance", "best_posts", "content_types", "posting_times"));
                    break;
                case "comprehensive":
                    // Include everything
                    priorities.addAll(Arrays.asList("followers", "engagement_rate", "posts", "pending_responses"));
                    break;
            }
        }

        return priorities;
    }
}
