# Facebook Real Data Integration Guide

This guide shows you exactly what to change to integrate real Facebook data instead of mock data.

---

## 1. Update FacebookService.java with Real API Calls

Replace the mock methods in `FacebookService.java` with these real implementations:

### A. Add Required Dependencies

First, add these to your service:

```java
package com.cliq24.backend.service;

import com.cliq24.backend.model.SocialAccount;
import com.cliq24.backend.repository.SocialAccountRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
```

### B. Add RestTemplate to Constructor

```java
private final SocialAccountRepository socialAccountRepository;
private final RestTemplate restTemplate;

@Autowired
public FacebookService(SocialAccountRepository socialAccountRepository) {
    this.socialAccountRepository = socialAccountRepository;
    this.restTemplate = new RestTemplate();
}
```

### C. Replace getRecentMessages() Method

```java
/**
 * Get recent messages for a Facebook account using Facebook Graph API
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

    // Get the access token stored for this account
    String accessToken = account.getAccessToken();
    if (accessToken == null || accessToken.isEmpty()) {
        throw new RuntimeException("No access token found for this account");
    }

    try {
        // Facebook Graph API endpoint for page conversations
        // Get the Page ID (stored in account.platformUserId)
        String pageId = account.getPlatformUserId();

        String apiUrl = String.format(
            "https://graph.facebook.com/v18.0/%s/conversations?fields=messages{message,from,created_time},updated_time&limit=5&access_token=%s",
            pageId,
            accessToken
        );

        // Make API call
        ResponseEntity<Map> response = restTemplate.getForEntity(apiUrl, Map.class);
        Map<String, Object> responseBody = response.getBody();

        if (responseBody == null || !responseBody.containsKey("data")) {
            logger.warn("No conversations data returned from Facebook API");
            return new ArrayList<>();
        }

        // Parse conversations and extract messages
        List<Map<String, Object>> conversations = (List<Map<String, Object>>) responseBody.get("data");
        List<Map<String, Object>> allMessages = new ArrayList<>();

        for (Map<String, Object> conversation : conversations) {
            if (conversation.containsKey("messages")) {
                Map<String, Object> messagesData = (Map<String, Object>) conversation.get("messages");
                List<Map<String, Object>> messages = (List<Map<String, Object>>) messagesData.get("data");

                for (Map<String, Object> msg : messages) {
                    Map<String, Object> formattedMessage = new HashMap<>();
                    formattedMessage.put("id", msg.get("id"));

                    // Extract sender info
                    if (msg.containsKey("from")) {
                        Map<String, Object> from = (Map<String, Object>) msg.get("from");
                        formattedMessage.put("senderId", from.get("id"));
                        formattedMessage.put("senderName", from.get("name"));
                    }

                    formattedMessage.put("message", msg.get("message"));

                    // Convert Facebook timestamp to LocalDateTime
                    String createdTime = (String) msg.get("created_time");
                    if (createdTime != null) {
                        formattedMessage.put("timestamp", createdTime);
                    }

                    formattedMessage.put("read", true); // Assume read for now

                    allMessages.add(formattedMessage);
                }
            }
        }

        // Sort by timestamp (most recent first) and limit to 5
        allMessages.sort((a, b) -> {
            String timeA = (String) a.get("timestamp");
            String timeB = (String) b.get("timestamp");
            return timeB.compareTo(timeA);
        });

        List<Map<String, Object>> recentMessages = allMessages.subList(0, Math.min(5, allMessages.size()));

        logger.info("Returning {} messages for account {}", recentMessages.size(), accountId);
        return recentMessages;

    } catch (Exception e) {
        logger.error("Failed to fetch messages from Facebook API: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to fetch messages from Facebook: " + e.getMessage());
    }
}
```

### D. Replace sendMessage() Method

```java
/**
 * Send a message via Facebook Messenger API
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

    // Get the access token
    String accessToken = account.getAccessToken();
    if (accessToken == null || accessToken.isEmpty()) {
        throw new RuntimeException("No access token found for this account");
    }

    try {
        // Facebook Send API endpoint
        String pageId = account.getPlatformUserId();
        String apiUrl = String.format(
            "https://graph.facebook.com/v18.0/%s/messages?access_token=%s",
            pageId,
            accessToken
        );

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();

        // Recipient
        Map<String, String> recipient = new HashMap<>();
        recipient.put("id", recipientId);
        requestBody.put("recipient", recipient);

        // Message
        Map<String, String> messageObj = new HashMap<>();
        messageObj.put("text", message);
        requestBody.put("message", messageObj);

        // Messaging type
        requestBody.put("messaging_type", "RESPONSE");

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Make API call
        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
        Map<String, Object> responseBody = response.getBody();

        if (responseBody == null) {
            throw new RuntimeException("No response from Facebook API");
        }

        logger.info("Message sent successfully to {} via Facebook", recipientId);

        // Format response
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("messageId", responseBody.get("message_id"));
        result.put("recipientId", recipientId);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis() / 1000);

        return result;

    } catch (Exception e) {
        logger.error("Failed to send message via Facebook API: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to send message via Facebook: " + e.getMessage());
    }
}
```

---

## 2. Update SocialAccount Model to Store Access Token

Make sure your `SocialAccount.java` model has an `accessToken` field:

```java
@Document(collection = "social_accounts")
public class SocialAccount {
    @Id
    private String id;

    private String userId;
    private String platform;
    private String platformUserId;  // Facebook Page ID
    private String username;

    @Encrypted  // Make sure to encrypt this field!
    private String accessToken;     // ADD THIS FIELD

    private Map<String, Object> metrics;
    private LocalDateTime connectedAt;
    private LocalDateTime lastSyncedAt;

    // Getters and setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
```

---

## 3. Update Facebook Connection Flow to Store Page Access Token

In `SocialAccountService.java`, update `connectFacebookAccount()` to get and store the page access token:

```java
public SocialAccountDTO connectFacebookAccount(String authHeader, String code) {
    String userId = authService.validateAndExtractUserId(authHeader);

    try {
        // 1. Exchange code for user access token
        String tokenUrl = String.format(
            "https://graph.facebook.com/v18.0/oauth/access_token?client_id=%s&client_secret=%s&redirect_uri=%s&code=%s",
            facebookAppId, facebookAppSecret, facebookRedirectUri, code
        );

        ResponseEntity<Map> tokenResponse = restTemplate.getForEntity(tokenUrl, Map.class);
        String userAccessToken = (String) tokenResponse.getBody().get("access_token");

        // 2. Get user's Facebook Pages
        String pagesUrl = String.format(
            "https://graph.facebook.com/v18.0/me/accounts?access_token=%s",
            userAccessToken
        );

        ResponseEntity<Map> pagesResponse = restTemplate.getForEntity(pagesUrl, Map.class);
        Map<String, Object> pagesBody = pagesResponse.getBody();

        List<Map<String, Object>> pages = (List<Map<String, Object>>) pagesBody.get("data");

        if (pages == null || pages.isEmpty()) {
            throw new RuntimeException("No Facebook Pages found. You need to manage a Facebook Page to use this feature.");
        }

        // 3. Use the first page (or let user choose in UI)
        Map<String, Object> page = pages.get(0);
        String pageId = (String) page.get("id");
        String pageName = (String) page.get("name");
        String pageAccessToken = (String) page.get("access_token");  // This is the important one!

        // 4. Get long-lived page access token (doesn't expire)
        String longLivedTokenUrl = String.format(
            "https://graph.facebook.com/v18.0/oauth/access_token?grant_type=fb_exchange_token&client_id=%s&client_secret=%s&fb_exchange_token=%s",
            facebookAppId, facebookAppSecret, pageAccessToken
        );

        ResponseEntity<Map> longLivedResponse = restTemplate.getForEntity(longLivedTokenUrl, Map.class);
        String longLivedPageToken = (String) longLivedResponse.getBody().get("access_token");

        // 5. Create or update social account
        SocialAccount account = new SocialAccount();
        account.setUserId(userId);
        account.setPlatform("Facebook");
        account.setPlatformUserId(pageId);
        account.setUsername(pageName);
        account.setAccessToken(longLivedPageToken);  // STORE THE TOKEN!
        account.setConnectedAt(LocalDateTime.now());

        // Initialize metrics
        AccountMetrics metrics = new AccountMetrics();
        metrics.setEngagementScore(0);
        metrics.setConnections(0);
        metrics.setPosts(0);
        account.setMetrics(metrics.toMap());

        SocialAccount savedAccount = socialAccountRepository.save(account);

        // Sync initial data
        facebookService.syncAccountData(savedAccount);

        return socialAccountMapper.toDTO(savedAccount);

    } catch (Exception e) {
        logger.error("Failed to connect Facebook account: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to connect Facebook: " + e.getMessage());
    }
}
```

---

## 4. Facebook Graph API Endpoints Reference

Here are the key endpoints you'll use:

### Get Conversations (Messages)
```
GET https://graph.facebook.com/v18.0/{page-id}/conversations
    ?fields=messages{message,from,created_time},updated_time
    &limit=10
    &access_token={page-access-token}
```

### Send Message
```
POST https://graph.facebook.com/v18.0/{page-id}/messages
Content-Type: application/json

{
  "recipient": {
    "id": "{recipient-psid}"
  },
  "message": {
    "text": "Your message here"
  },
  "messaging_type": "RESPONSE"
}
```

### Get Page Insights (Metrics)
```
GET https://graph.facebook.com/v18.0/{page-id}/insights
    ?metric=page_impressions,page_engaged_users,page_fan_adds
    &period=day
    &access_token={page-access-token}
```

### Get Page Follower Count
```
GET https://graph.facebook.com/v18.0/{page-id}
    ?fields=followers_count,name,fan_count
    &access_token={page-access-token}
```

---

## 5. Required Facebook App Permissions

In your Facebook App Dashboard (developers.facebook.com), request these permissions:

### For Messaging:
- `pages_messaging` - Send and receive messages
- `pages_manage_metadata` - Manage page metadata

### For Metrics:
- `pages_read_engagement` - Read page engagement metrics
- `pages_show_list` - List pages user manages
- `pages_read_user_content` - Read page content
- `read_insights` - Read page insights

### Permission Request Flow:
When users connect Facebook, they'll see a permission dialog asking for these permissions. Make sure your Facebook App is approved for these permissions in production.

---

## 6. Testing the Integration

### Step 1: Update application.properties
```properties
spring.security.oauth2.client.registration.facebook.scope=pages_show_list,pages_messaging,pages_manage_metadata,pages_read_engagement,read_insights,pages_read_user_content
```

### Step 2: Test in Facebook Graph API Explorer
1. Go to: https://developers.facebook.com/tools/explorer
2. Select your app
3. Get Page Access Token with required permissions
4. Test the endpoints manually:
   - Get conversations: `/{page-id}/conversations?fields=messages`
   - Send message: POST to `/{page-id}/messages`

### Step 3: Handle Errors
Add proper error handling:

```java
try {
    // API call
} catch (HttpClientErrorException e) {
    if (e.getStatusCode().value() == 400) {
        // Permission error or invalid request
        logger.error("Facebook API error: {}", e.getResponseBodyAsString());
        throw new RuntimeException("Facebook permission error: " + e.getMessage());
    } else if (e.getStatusCode().value() == 401) {
        // Token expired
        throw new RuntimeException("Facebook access token expired. Please reconnect your account.");
    }
    throw new RuntimeException("Facebook API error: " + e.getMessage());
}
```

---

## 7. Important Notes

### Access Token Management:
- **User Access Tokens**: Expire in ~60 days
- **Page Access Tokens**: Can be long-lived (don't expire)
- **Get long-lived token**: Use token exchange endpoint
- **Store encrypted**: Use `@Encrypted` annotation on accessToken field

### Rate Limits:
Facebook has rate limits:
- 200 API calls per user per hour
- 4800 API calls per app per hour per user

### Webhook Alternative:
For real-time messages, set up Facebook Webhooks:
1. Create webhook endpoint: `POST /api/facebook/webhook`
2. Verify webhook in Facebook App settings
3. Subscribe to `messages` events
4. Receive messages in real-time instead of polling

### Message Types:
Facebook supports different message types:
- Text messages
- Images/videos
- Quick replies
- Buttons/templates

---

## 8. Security Checklist

- [ ] Encrypt access tokens in database
- [ ] Never log access tokens
- [ ] Implement token refresh logic
- [ ] Validate webhook signatures
- [ ] Rate limit your API calls
- [ ] Handle token expiration gracefully
- [ ] Add error logging for debugging
- [ ] Test with different page types

---

## Summary

**What Changes:**
1. Replace `generateMockMessages()` with real Graph API calls in `FacebookService.java`
2. Add `accessToken` field to `SocialAccount.java`
3. Update `connectFacebookAccount()` to get and store page access token
4. Request proper permissions in Facebook App

**What You Get:**
- Real Facebook messages from page conversations
- Ability to send actual messages to users
- Live metrics and insights
- No more mock data!

**Time to Implement:** ~2-3 hours including testing

Let me know if you need help with any specific part!
