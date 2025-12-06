# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Cliq24 is a Spring Boot-based social media management platform that helps users track and manage multiple social media accounts (Facebook, Instagram, Twitter, LinkedIn, TikTok, YouTube, Snapchat) with engagement scoring. The backend uses MongoDB for data storage, JWT for authentication, and integrates with Stripe for subscription management.

## Build and Development Commands

### Maven Commands
```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run

# Run tests
./mvnw test

# Build for production (skip tests)
./mvnw clean package -DskipTests

# Run specific test class
./mvnw test -Dtest=YourTestClassName

# Run specific test method
./mvnw test -Dtest=YourTestClassName#testMethodName
```

### Windows Note
On Windows, use `mvnw.cmd` instead of `./mvnw`:
```bash
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- MongoDB 4.4+ (must be running locally or accessible via MONGODB_URI)

### Environment Setup
1. Copy `.env.example` to `.env`
2. Configure required environment variables (see Environment Variables section)
3. Start MongoDB before running the application

## Architecture

### Multi-Tier Architecture
The application follows a standard Spring Boot layered architecture:

- **Controller Layer** (`controller/`): REST endpoints handling HTTP requests
- **Service Layer** (`service/`): Business logic and orchestration
- **Repository Layer** (`repository/`): MongoDB data access using Spring Data
- **Model Layer** (`model/`): MongoDB document entities
- **Platform Services** (`platforms/`): Social media API integrations (Facebook, Instagram, Twitter, etc.)
- **DTO Layer** (`dto/`): Data Transfer Objects for API responses
- **Mapper Layer** (`mapper/`): Entity-to-DTO conversions
- **Config Layer** (`config/`): Spring configuration classes (Security, OAuth2, MongoDB, etc.)

### Key Architectural Patterns

#### OAuth2 Integration Flow
Each social media platform has a dedicated service in `platforms/` that handles:
1. OAuth redirect URL generation
2. Authorization code exchange for access tokens
3. User profile fetching
4. Metrics synchronization from platform APIs

The flow is: User initiates OAuth → Platform redirects with code → `SocialAccountService` calls platform-specific service → Access token stored encrypted → Account created/updated in MongoDB.

Platform services use different OAuth flows:
- **PKCE (Proof Key for Code Exchange)**: Twitter, TikTok, Snapchat
- **Standard OAuth2**: Facebook, Instagram, LinkedIn, YouTube

#### Subscription and Account Limits
`SubscriptionService` enforces account connection limits:
- **FREE tier**: 2 social accounts maximum
- **PREMIUM tier**: Unlimited accounts

The service integrates with Stripe for payment processing. Account limit checks occur in `SocialAccountService.checkAccountLimit()` before connecting new accounts.

#### Engagement Score Calculation
`ScoreCalculator` computes engagement scores (0-100) using weighted metrics:
- **30%**: Connections/Followers (max score at 10,000 connections)
- **50%**: Posts Activity (each post = 2 points, max 50 posts)
- **20%**: Response Rate (pending responses reduce score)

Scores map to labels:
- 80-100: "Crushing It!"
- 60-79: "Doing Well"
- 40-59: "Needs Attention"
- 0-39: "Falling Behind"

#### Security Architecture
- **Google OAuth2** for primary user authentication (handled by Spring Security)
- **JWT tokens** issued after successful OAuth login (JwtUtil generates tokens)
- **Token encryption** for stored OAuth access tokens (EncryptionUtil uses AES)
- **Authorization header validation** in services (AuthService.validateAndExtractUserId)

The OAuth2LoginSuccessHandler creates JWT tokens after Google login and redirects to frontend with token in URL fragment.

### Data Models

#### User Entity
- Primary authentication via Google OAuth (googleId, email)
- Optional email/password authentication (passwordHash)
- Subscription tracking (subscriptionTier, subscriptionStatus, stripeCustomerId, stripeSubscriptionId)
- Stored in `users` collection

#### SocialAccount Entity
- Represents connected social media accounts
- Links to User via userId
- Stores encrypted access tokens and refresh tokens
- Contains embedded AccountMetrics object
- Platform identifier (facebook, instagram, twitter, linkedin, tiktok, youtube, snapchat)
- Stored in `social_accounts` collection

#### AccountMetrics (Embedded)
- connections: Followers/connections count
- posts: Number of posts
- pendingResponses: Unanswered messages/comments
- newMessages: New messages count
- engagementScore: Calculated score (0-100)

### Configuration Profiles

The application supports multiple Spring profiles:
- **local**: Development mode (default, uses .env file, SSL enabled with localhost cert)
- **prod**: Production mode (set via SPRING_PROFILES_ACTIVE env var, expects production SSL setup)

Profile-specific properties:
- `application.properties`: Base configuration with placeholders
- `application-local.properties`: Local development overrides
- `application-prod.properties`: Production overrides (if exists)

### Environment Variables

Critical environment variables (defined in .env or system env):

**Database:**
- MONGODB_URI: MongoDB connection string

**Security:**
- JWT_SECRET: Secret key for JWT token signing (256-bit minimum)
- ENCRYPTION_KEY: Key for encrypting OAuth tokens (256-bit hex)

**Google OAuth (Primary Auth):**
- GOOGLE_CLIENT_ID
- GOOGLE_CLIENT_SECRET
- GOOGLE_REDIRECT_URI (typically https://domain/login/oauth2/code/google)

**Social Platform OAuth:**
- FACEBOOK_APP_ID, FACEBOOK_APP_SECRET
- INSTAGRAM_APP_ID, INSTAGRAM_APP_SECRET
- TWITTER_CLIENT_ID, TWITTER_CLIENT_SECRET
- LINKEDIN_CLIENT_ID, LINKEDIN_CLIENT_SECRET
- TIKTOK_CLIENT_KEY, TIKTOK_CLIENT_SECRET
- SNAPCHAT_CLIENT_ID, SNAPCHAT_CLIENT_SECRET
- YOUTUBE_API_KEY (uses Google OAuth credentials)

**Stripe Payment:**
- STRIPE_SECRET_KEY
- STRIPE_PUBLISHABLE_KEY
- STRIPE_WEBHOOK_SECRET
- STRIPE_PRICE_ID (subscription price ID)

### Platform-Specific Implementation Notes

#### Instagram Integration
Currently operates in "demo mode" because Instagram requires Business accounts linked to Facebook Pages. The `connectInstagramAccount` method falls back to creating demo accounts with placeholder metrics if no Business account is found.

#### Twitter/X Integration
Uses OAuth2 with PKCE. The TwitterService handles PKCE code verifier validation. Twitter requires the code_verifier parameter during token exchange.

#### TikTok Integration
Uses OAuth2 with PKCE and custom client_key/client_secret parameters (not standard OAuth2). Retrieves real follower and video counts from TikTok API v2.

#### YouTube Integration
Reuses Google OAuth credentials but requires additional YouTube Data API v3 scopes. Fetches channel information and subscriber counts.

### Logging Configuration

The application uses Log4j2 (not Logback). Configuration in `src/main/resources/log4j2-spring.xml`. All Spring Boot starters explicitly exclude spring-boot-starter-logging in pom.xml.

Log levels:
- Application code (com.cliq24): INFO
- Spring Security: WARN
- Spring Web: WARN

### API Endpoint Structure

**Authentication:**
- GET `/auth/google`: Initiates Google OAuth flow
- GET `/login/oauth2/code/google`: Google OAuth callback
- GET `/auth/me`: Returns current user info (requires JWT)
- POST `/auth/register`: Email/password registration
- POST `/auth/login`: Email/password login

**Social Accounts:**
- GET `/api/social-accounts`: Get all user's connected accounts
- GET `/api/social-accounts/{platform}`: Initiate platform OAuth
- GET `/api/social-accounts/{platform}/callback`: Platform OAuth callback
- DELETE `/api/social-accounts/{accountId}`: Disconnect account
- POST `/api/social-accounts/{accountId}/sync`: Sync metrics

**Subscriptions:**
- POST `/api/subscription/create-checkout-session`: Create Stripe checkout
- POST `/api/subscription/webhook`: Stripe webhook handler

### Common Development Tasks

#### Adding a New Social Platform
1. Create platform service in `platforms/` implementing OAuth flow
2. Add OAuth configuration to `application.properties`
3. Add environment variables to `.env.example`
4. Update `SocialAccountService.syncMetrics()` switch statement
5. Add platform to `SecurityConfig` permitAll() list for callbacks
6. Create controller endpoint for OAuth initiation and callback

#### Modifying Engagement Score Algorithm
Edit `ScoreCalculator.calculateEngagementScore()` to adjust weights or scoring logic. The weights are constants at the top of the class.

#### Testing OAuth Integration Locally
1. Use ngrok or similar to create HTTPS tunnel: `ngrok http 8443`
2. Update OAuth redirect URIs in platform developer consoles to use ngrok URL
3. Update `.env` redirect URIs to match
4. Test OAuth flow through ngrok URL

### SSL/HTTPS Configuration

The application runs on HTTPS by default (port 8443) using a self-signed certificate in development. For production:
- Set SSL_ENABLED=false if behind a reverse proxy (e.g., Railway, Heroku)
- Use `server.forward-headers-strategy=framework` to trust X-Forwarded-* headers

Session cookies are configured with SameSite=None and Secure=true for Safari/iOS OAuth compatibility.
