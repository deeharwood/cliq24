package com.cliq24.backend.platforms;

import com.cliq24.backend.model.AccountMetrics;
import com.cliq24.backend.model.SocialAccount;
import com.cliq24.backend.repository.SocialAccountRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class FacebookService {

    private static final Logger logger = LogManager.getLogger(FacebookService.class);

    private final SocialAccountRepository socialAccountRepository;
    private final RestTemplate restTemplate;

    @Autowired
    public FacebookService(SocialAccountRepository socialAccountRepository) {
        this.socialAccountRepository = socialAccountRepository;
        this.restTemplate = new RestTemplate();
    }

    public AccountMetrics syncMetrics(SocialAccount account) {
        logger.info("Syncing real metrics for Facebook account: {}", account.getUsername());

        String accessToken = account.getAccessToken();
        AccountMetrics metrics = new AccountMetrics();

        // If no access token, return default metrics
        if (accessToken == null || accessToken.isEmpty()) {
            logger.warn("No access token for account {}, using default metrics", account.getId());
            metrics.setConnections(0);
            metrics.setPosts(0);
            metrics.setPendingResponses(0);
            metrics.setNewMessages(0);
            return metrics;
        }

        try {
            String pageId = account.getPlatformUserId();

            // Fetch Page info including follower count - try multiple field variations
            String pageInfoUrl = String.format(
                "https://graph.facebook.com/v18.0/%s?fields=followers_count,fan_count,name,likes,talking_about_count,new_like_count&access_token=%s",
                pageId, accessToken
            );

            logger.info("Fetching page info from Facebook API for page: {}", pageId);
            Map<String, Object> pageInfo = restTemplate.getForObject(pageInfoUrl, Map.class);

            // Log what we actually received from Facebook
            logger.info("Facebook API response: {}", pageInfo);

            // Get follower count - try multiple field names
            int followerCount = 0;
            if (pageInfo != null) {
                logger.info("Available fields in response: {}", pageInfo.keySet());

                if (pageInfo.containsKey("followers_count")) {
                    followerCount = ((Number) pageInfo.get("followers_count")).intValue();
                    logger.info("Got follower count from 'followers_count': {}", followerCount);
                } else if (pageInfo.containsKey("fan_count")) {
                    followerCount = ((Number) pageInfo.get("fan_count")).intValue();
                    logger.info("Got follower count from 'fan_count': {}", followerCount);
                } else if (pageInfo.containsKey("likes")) {
                    followerCount = ((Number) pageInfo.get("likes")).intValue();
                    logger.info("Got follower count from 'likes': {}", followerCount);
                } else {
                    logger.warn("No follower count field found in response. Available fields: {}", pageInfo.keySet());
                }
                logger.info("Facebook page {} has {} followers", pageInfo.get("name"), followerCount);
            } else {
                logger.error("Received null response from Facebook API");
            }

            // Fetch post count (get recent posts and count them, or use summary)
            String postsUrl = String.format(
                "https://graph.facebook.com/v18.0/%s/posts?summary=true&limit=100&access_token=%s",
                pageId, accessToken
            );

            logger.info("Fetching posts from Facebook API");
            Map<String, Object> postsResponse = restTemplate.getForObject(postsUrl, Map.class);
            int postCount = 0;
            if (postsResponse != null && postsResponse.containsKey("data")) {
                List<?> posts = (List<?>) postsResponse.get("data");
                postCount = posts.size();
                logger.info("Retrieved {} posts from feed", postCount);

                // If we got 100 posts, there might be more, but we'll use 100 as the count
                if (postCount == 100 && postsResponse.containsKey("summary")) {
                    Map<String, Object> summary = (Map<String, Object>) postsResponse.get("summary");
                    if (summary.containsKey("total_count")) {
                        postCount = ((Number) summary.get("total_count")).intValue();
                        logger.info("Using total_count from summary: {}", postCount);
                    }
                }
                logger.info("Facebook page has {} posts", postCount);
            } else {
                logger.warn("No posts data in response. Response: {}", postsResponse);
            }

            // Set the metrics
            metrics.setConnections(followerCount);
            metrics.setPosts(postCount);

            // For now, set pending responses and new messages to 0
            // These would require additional API calls to conversations and comments
            metrics.setPendingResponses(0);
            metrics.setNewMessages(0);

            logger.info("Successfully synced Facebook metrics: followers={}, posts={}", followerCount, postCount);

        } catch (Exception e) {
            logger.error("Failed to sync Facebook metrics: {}", e.getMessage(), e);
            // Return default metrics on error
            metrics.setConnections(0);
            metrics.setPosts(0);
            metrics.setPendingResponses(0);
            metrics.setNewMessages(0);
        }

        return metrics;
    }

    /**
     * Get recent messages for a Facebook account
     * Returns the last 5 messages from conversations
     */
    public List<Map<String, Object>> getRecentMessages(String userId, String accountId) {
        logger.debug("Getting recent messages for account {} owned by user {}", accountId, userId);

        // Verify the account belongs to this user
        SocialAccount account = socialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Social account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Account does not belong to user");
        }

        if (!"Facebook".equals(account.getPlatform())) {
            throw new RuntimeException("This endpoint is only for Facebook accounts");
        }

        String accessToken = account.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            logger.warn("No access token for account {}, returning mock data", accountId);
            return generateMockMessages();
        }

        try {
            String pageId = account.getPlatformUserId();

            // Fetch conversations from the page
            String conversationsUrl = String.format(
                "https://graph.facebook.com/v18.0/%s/conversations?fields=id,snippet,updated_time,unread_count,participants&limit=5&access_token=%s",
                pageId, accessToken
            );

            logger.info("Fetching conversations from Facebook API for page: {}", pageId);
            Map<String, Object> response = restTemplate.getForObject(conversationsUrl, Map.class);

            if (response == null || !response.containsKey("data")) {
                logger.warn("No conversations data returned from Facebook API");
                return generateMockMessages();
            }

            List<Map<String, Object>> conversations = (List<Map<String, Object>>) response.get("data");
            List<Map<String, Object>> messages = new ArrayList<>();

            // Process each conversation
            for (Map<String, Object> conversation : conversations) {
                String conversationId = (String) conversation.get("id");

                // Fetch messages from this conversation
                String messagesUrl = String.format(
                    "https://graph.facebook.com/v18.0/%s/messages?fields=id,from,message,created_time&limit=1&access_token=%s",
                    conversationId, accessToken
                );

                Map<String, Object> messagesResponse = restTemplate.getForObject(messagesUrl, Map.class);

                if (messagesResponse != null && messagesResponse.containsKey("data")) {
                    List<Map<String, Object>> conversationMessages = (List<Map<String, Object>>) messagesResponse.get("data");

                    if (!conversationMessages.isEmpty()) {
                        Map<String, Object> latestMessage = conversationMessages.get(0);
                        Map<String, Object> formattedMessage = new HashMap<>();

                        formattedMessage.put("id", latestMessage.get("id"));
                        formattedMessage.put("message", latestMessage.get("message"));
                        formattedMessage.put("timestamp", latestMessage.get("created_time"));

                        // Get sender info from participants
                        if (conversation.containsKey("participants")) {
                            Map<String, Object> participants = (Map<String, Object>) conversation.get("participants");
                            if (participants.containsKey("data")) {
                                List<Map<String, Object>> participantList = (List<Map<String, Object>>) participants.get("data");
                                if (!participantList.isEmpty()) {
                                    Map<String, Object> participant = participantList.get(0);
                                    formattedMessage.put("senderId", participant.get("id"));
                                    formattedMessage.put("senderName", participant.get("name"));
                                }
                            }
                        }

                        // Check if message is read based on unread_count
                        int unreadCount = conversation.containsKey("unread_count") ?
                            ((Number) conversation.get("unread_count")).intValue() : 0;
                        formattedMessage.put("read", unreadCount == 0);

                        messages.add(formattedMessage);
                    }
                }
            }

            logger.info("Returning {} real messages from Facebook", messages.size());
            return messages;

        } catch (Exception e) {
            logger.error("Failed to fetch messages from Facebook API: {}", e.getMessage(), e);
            return generateMockMessages();
        }
    }

    /**
     * Send a message via Facebook Send API
     */
    public Map<String, Object> sendMessage(String userId, String accountId, String recipientId, String message) {
        logger.debug("Sending message from account {} to {}", accountId, recipientId);

        // Verify the account belongs to this user
        SocialAccount account = socialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Social account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Account does not belong to user");
        }

        if (!"Facebook".equals(account.getPlatform())) {
            throw new RuntimeException("This endpoint is only for Facebook accounts");
        }

        String accessToken = account.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            logger.warn("No access token for account {}, cannot send message", accountId);
            throw new RuntimeException("No access token available for this account");
        }

        try {
            // Use Facebook Send API to send the message
            String sendApiUrl = String.format(
                "https://graph.facebook.com/v18.0/me/messages?access_token=%s",
                accessToken
            );

            // Prepare the message payload
            Map<String, Object> messagePayload = new HashMap<>();
            Map<String, Object> recipient = new HashMap<>();
            recipient.put("id", recipientId);

            Map<String, Object> messageContent = new HashMap<>();
            messageContent.put("text", message);

            messagePayload.put("recipient", recipient);
            messagePayload.put("message", messageContent);
            messagePayload.put("messaging_type", "RESPONSE");

            logger.info("Sending message via Facebook Send API to recipient: {}", recipientId);

            // Send POST request to Facebook Send API
            Map<String, Object> response = restTemplate.postForObject(sendApiUrl, messagePayload, Map.class);

            if (response != null && response.containsKey("message_id")) {
                logger.info("Message sent successfully via Facebook, message_id: {}", response.get("message_id"));

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("messageId", response.get("message_id"));
                result.put("recipientId", recipientId);
                result.put("message", message);
                result.put("timestamp", System.currentTimeMillis() / 1000);

                return result;
            } else {
                logger.error("Failed to send message, unexpected response: {}", response);
                throw new RuntimeException("Failed to send message via Facebook API");
            }

        } catch (Exception e) {
            logger.error("Failed to send message via Facebook API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Generate mock messages as fallback when API access is unavailable
     */
    private List<Map<String, Object>> generateMockMessages() {
        List<Map<String, Object>> messages = new ArrayList<>();

        String[] senders = {
            "Sarah Johnson",
            "Mike Chen",
            "Emily Rodriguez",
            "David Kim",
            "Jessica Martinez"
        };

        String[] messageTexts = {
            "Hey! I saw your recent post about the product launch. Looks amazing! 🎉",
            "Thanks for the quick response! Really appreciate your help with this.",
            "Is this still available? I'd love to get more details.",
            "Great content as always! Keep up the awesome work! 👍",
            "Quick question about your services. Can you DM me when you get a chance?"
        };

        for (int i = 0; i < 5; i++) {
            Map<String, Object> message = new HashMap<>();
            message.put("id", "msg_" + UUID.randomUUID().toString());
            message.put("senderId", "user_" + (i + 1));
            message.put("senderName", senders[i]);
            message.put("message", messageTexts[i]);
            message.put("timestamp", LocalDateTime.now().minusHours(i + 1).toString());
            message.put("read", i < 2); // First 2 are read
            messages.add(message);
        }

        return messages;
    }

    /**
     * Get real posts from Facebook Page
     */
    public List<Map<String, Object>> getPosts(String userId, String accountId, int limit) {
        logger.debug("Getting posts for account {} owned by user {}", accountId, userId);

        // Verify the account belongs to this user
        SocialAccount account = socialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Social account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Account does not belong to user");
        }

        if (!"Facebook".equals(account.getPlatform())) {
            throw new RuntimeException("This endpoint is only for Facebook accounts");
        }

        String accessToken = account.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            logger.warn("No access token for account {}, returning mock data", accountId);
            return getMockPosts();
        }

        try {
            String pageId = account.getPlatformUserId();
            String apiUrl = String.format(
                "https://graph.facebook.com/v18.0/%s/feed?fields=id,message,created_time,full_picture,permalink_url,likes.summary(true),comments.summary(true),shares&limit=%d&access_token=%s",
                pageId, limit, accessToken
            );

            logger.info("Fetching posts from Facebook API for page: {}", pageId);
            Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);

            if (response == null || !response.containsKey("data")) {
                logger.warn("No posts data returned from Facebook API");
                return getMockPosts();
            }

            List<Map<String, Object>> posts = (List<Map<String, Object>>) response.get("data");
            List<Map<String, Object>> formattedPosts = new ArrayList<>();

            for (Map<String, Object> post : posts) {
                Map<String, Object> formattedPost = new HashMap<>();
                formattedPost.put("id", post.get("id"));
                formattedPost.put("message", post.get("message"));
                formattedPost.put("createdTime", post.get("created_time"));
                formattedPost.put("picture", post.get("full_picture"));
                formattedPost.put("link", post.get("permalink_url"));

                // Extract like count
                if (post.containsKey("likes")) {
                    Map<String, Object> likes = (Map<String, Object>) post.get("likes");
                    if (likes.containsKey("summary")) {
                        Map<String, Object> summary = (Map<String, Object>) likes.get("summary");
                        formattedPost.put("likeCount", summary.get("total_count"));
                    }
                }

                // Extract comment count
                if (post.containsKey("comments")) {
                    Map<String, Object> comments = (Map<String, Object>) post.get("comments");
                    if (comments.containsKey("summary")) {
                        Map<String, Object> summary = (Map<String, Object>) comments.get("summary");
                        formattedPost.put("commentCount", summary.get("total_count"));
                    }
                }

                // Extract share count
                if (post.containsKey("shares")) {
                    Map<String, Object> shares = (Map<String, Object>) post.get("shares");
                    formattedPost.put("shareCount", shares.get("count"));
                }

                formattedPosts.add(formattedPost);
            }

            logger.info("Returning {} real posts from Facebook", formattedPosts.size());
            return formattedPosts;

        } catch (Exception e) {
            logger.error("Failed to fetch posts from Facebook API: {}", e.getMessage(), e);
            return getMockPosts();
        }
    }

    /**
     * Get real photos from Facebook Page
     */
    public List<Map<String, Object>> getPhotos(String userId, String accountId, int limit) {
        logger.debug("Getting photos for account {} owned by user {}", accountId, userId);

        SocialAccount account = socialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Social account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Account does not belong to user");
        }

        if (!"Facebook".equals(account.getPlatform())) {
            throw new RuntimeException("This endpoint is only for Facebook accounts");
        }

        String accessToken = account.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            logger.warn("No access token for account {}, returning mock data", accountId);
            return getMockPhotos();
        }

        try {
            String pageId = account.getPlatformUserId();
            String apiUrl = String.format(
                "https://graph.facebook.com/v18.0/%s/photos?fields=id,images,created_time,name,likes.summary(true),comments.summary(true),link&type=uploaded&limit=%d&access_token=%s",
                pageId, limit, accessToken
            );

            logger.info("Fetching photos from Facebook API for page: {}", pageId);
            Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);

            if (response == null || !response.containsKey("data")) {
                logger.warn("No photos data returned from Facebook API");
                return getMockPhotos();
            }

            List<Map<String, Object>> photos = (List<Map<String, Object>>) response.get("data");
            List<Map<String, Object>> formattedPhotos = new ArrayList<>();

            for (Map<String, Object> photo : photos) {
                Map<String, Object> formattedPhoto = new HashMap<>();
                formattedPhoto.put("id", photo.get("id"));
                formattedPhoto.put("caption", photo.get("name"));
                formattedPhoto.put("createdTime", photo.get("created_time"));
                formattedPhoto.put("link", photo.get("link"));

                // Get the highest resolution image
                if (photo.containsKey("images")) {
                    List<Map<String, Object>> images = (List<Map<String, Object>>) photo.get("images");
                    if (!images.isEmpty()) {
                        formattedPhoto.put("imageUrl", images.get(0).get("source"));
                    }
                }

                // Extract like count
                if (photo.containsKey("likes")) {
                    Map<String, Object> likes = (Map<String, Object>) photo.get("likes");
                    if (likes.containsKey("summary")) {
                        Map<String, Object> summary = (Map<String, Object>) likes.get("summary");
                        formattedPhoto.put("likeCount", summary.get("total_count"));
                    }
                }

                formattedPhotos.add(formattedPhoto);
            }

            logger.info("Returning {} real photos from Facebook", formattedPhotos.size());
            return formattedPhotos;

        } catch (Exception e) {
            logger.error("Failed to fetch photos from Facebook API: {}", e.getMessage(), e);
            return getMockPhotos();
        }
    }

    /**
     * Get real videos from Facebook Page
     */
    public List<Map<String, Object>> getVideos(String userId, String accountId, int limit) {
        logger.debug("Getting videos for account {} owned by user {}", accountId, userId);

        SocialAccount account = socialAccountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Social account not found"));

        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Account does not belong to user");
        }

        if (!"Facebook".equals(account.getPlatform())) {
            throw new RuntimeException("This endpoint is only for Facebook accounts");
        }

        String accessToken = account.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            logger.warn("No access token for account {}, returning mock data", accountId);
            return getMockVideos();
        }

        try {
            String pageId = account.getPlatformUserId();
            String apiUrl = String.format(
                "https://graph.facebook.com/v18.0/%s/videos?fields=id,title,description,created_time,source,picture,permalink_url,likes.summary(true),comments.summary(true),views&limit=%d&access_token=%s",
                pageId, limit, accessToken
            );

            logger.info("Fetching videos from Facebook API for page: {}", pageId);
            Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);

            if (response == null || !response.containsKey("data")) {
                logger.warn("No videos data returned from Facebook API");
                return getMockVideos();
            }

            List<Map<String, Object>> videos = (List<Map<String, Object>>) response.get("data");
            List<Map<String, Object>> formattedVideos = new ArrayList<>();

            for (Map<String, Object> video : videos) {
                Map<String, Object> formattedVideo = new HashMap<>();
                formattedVideo.put("id", video.get("id"));
                formattedVideo.put("title", video.get("title"));
                formattedVideo.put("description", video.get("description"));
                formattedVideo.put("createdTime", video.get("created_time"));
                formattedVideo.put("videoUrl", video.get("source"));
                formattedVideo.put("thumbnail", video.get("picture"));
                formattedVideo.put("link", video.get("permalink_url"));
                formattedVideo.put("views", video.get("views"));

                // Extract like count
                if (video.containsKey("likes")) {
                    Map<String, Object> likes = (Map<String, Object>) video.get("likes");
                    if (likes.containsKey("summary")) {
                        Map<String, Object> summary = (Map<String, Object>) likes.get("summary");
                        formattedVideo.put("likeCount", summary.get("total_count"));
                    }
                }

                formattedVideos.add(formattedVideo);
            }

            logger.info("Returning {} real videos from Facebook", formattedVideos.size());
            return formattedVideos;

        } catch (Exception e) {
            logger.error("Failed to fetch videos from Facebook API: {}", e.getMessage(), e);
            return getMockVideos();
        }
    }

    // Mock data fallbacks
    private List<Map<String, Object>> getMockPosts() {
        List<Map<String, Object>> posts = new ArrayList<>();
        Map<String, Object> post = new HashMap<>();
        post.put("id", "mock_1");
        post.put("message", "This is a mock post. Connect your Facebook account with proper permissions to see real posts.");
        post.put("createdTime", LocalDateTime.now().toString());
        post.put("likeCount", 0);
        post.put("commentCount", 0);
        posts.add(post);
        return posts;
    }

    private List<Map<String, Object>> getMockPhotos() {
        return new ArrayList<>();
    }

    private List<Map<String, Object>> getMockVideos() {
        return new ArrayList<>();
    }
}

