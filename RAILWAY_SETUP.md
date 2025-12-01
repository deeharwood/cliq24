# Railway Deployment Guide for Cliq24

## Prerequisites

1. A Railway account (https://railway.app)
2. Railway CLI installed (optional but recommended)
3. Your project pushed to a Git repository (GitHub, GitLab, or Bitbucket)

## Step 1: Create a New Project on Railway

1. Log in to Railway (https://railway.app)
2. Click "New Project"
3. Select "Deploy from GitHub repo"
4. Choose your cliq24 repository
5. Railway will automatically detect the Dockerfile

## Step 2: Add MongoDB Database

1. In your Railway project, click "+ New"
2. Select "Database" → "MongoDB"
3. Railway will provision a MongoDB instance
4. Copy the `MONGO_URL` connection string from the MongoDB service variables

## Step 3: Configure Environment Variables

In your Railway project settings, add these environment variables:

### Required Variables

```
SPRING_PROFILES_ACTIVE=prod
PORT=8080
MONGODB_URI=<your-railway-mongodb-connection-string>
JWT_SECRET=<generate-a-strong-random-secret-at-least-64-characters>
ENCRYPTION_KEY=<generate-a-32-character-hex-string>
```

### Google OAuth (Required for login)

```
GOOGLE_CLIENT_ID=<your-google-client-id>
GOOGLE_CLIENT_SECRET=<your-google-client-secret>
GOOGLE_REDIRECT_URI=https://your-app.railway.app/login/oauth2/code/google
```

### Optional Social Media Platform Credentials

Add these as needed for the platforms you want to support:

```
FACEBOOK_APP_ID=<your-facebook-app-id>
FACEBOOK_APP_SECRET=<your-facebook-app-secret>

INSTAGRAM_APP_ID=<your-instagram-app-id>
INSTAGRAM_APP_SECRET=<your-instagram-app-secret>

TWITTER_CLIENT_ID=<your-twitter-client-id>
TWITTER_CLIENT_SECRET=<your-twitter-client-secret>

TIKTOK_CLIENT_KEY=<your-tiktok-client-key>
TIKTOK_CLIENT_SECRET=<your-tiktok-client-secret>

LINKEDIN_CLIENT_ID=<your-linkedin-client-id>
LINKEDIN_CLIENT_SECRET=<your-linkedin-client-secret>

YOUTUBE_API_KEY=<your-youtube-api-key>

SNAPCHAT_CLIENT_ID=<your-snapchat-client-id>
SNAPCHAT_CLIENT_SECRET=<your-snapchat-client-secret>

STRIPE_SECRET_KEY=<your-stripe-secret-key>
STRIPE_PUBLISHABLE_KEY=<your-stripe-publishable-key>
STRIPE_WEBHOOK_SECRET=<your-stripe-webhook-secret>
STRIPE_PRICE_ID=<your-stripe-price-id>
```

### CORS Configuration

```
CORS_ORIGINS=https://your-app.railway.app
```

## Step 4: Generate Secure Secrets

### JWT Secret (minimum 64 characters)
```bash
openssl rand -base64 64
```

### Encryption Key (32 characters hex)
```bash
openssl rand -hex 16
```

## Step 5: Configure Your Domain

### Option A: Use Railway's Generated Domain
Railway automatically provides a domain like `your-app.railway.app`

### Option B: Use Custom Domain
1. Go to your service settings in Railway
2. Click "Settings" → "Domains"
3. Click "Custom Domain"
4. Enter your domain (e.g., cliq24.app)
5. Add the CNAME record to your DNS provider as shown by Railway

## Step 6: Update OAuth Redirect URIs

For each OAuth provider you're using, update the authorized redirect URIs to include your Railway domain:

### Google OAuth
1. Go to https://console.cloud.google.com
2. Select your project
3. Go to "Credentials"
4. Edit your OAuth 2.0 Client ID
5. Add authorized redirect URI: `https://your-app.railway.app/login/oauth2/code/google`

### Facebook/Instagram
1. Go to https://developers.facebook.com
2. Select your app
3. Go to "Facebook Login" → "Settings"
4. Add valid OAuth redirect URI: `https://your-app.railway.app/api/social-accounts/facebook/callback`
5. For Instagram: `https://your-app.railway.app/api/social-accounts/instagram/callback`

### Twitter
1. Go to https://developer.twitter.com
2. Select your app
3. Edit app settings
4. Add callback URL: `https://your-app.railway.app/api/social-accounts/twitter/callback`

### LinkedIn
1. Go to https://www.linkedin.com/developers
2. Select your app
3. Go to "Auth" tab
4. Add redirect URL: `https://your-app.railway.app/api/social-accounts/linkedin/callback`

### TikTok
1. Go to https://developers.tiktok.com
2. Select your app
3. Add redirect URI: `https://your-app.railway.app/api/social-accounts/tiktok/callback`

### YouTube
Uses Google OAuth credentials (see Google OAuth above)
Add: `https://your-app.railway.app/api/social-accounts/youtube/callback`

### Snapchat
1. Go to https://kit.snapchat.com
2. Select your app
3. Add redirect URI: `https://your-app.railway.app/api/social-accounts/snapchat/callback`

## Step 7: Deploy

1. Push your code to your Git repository
2. Railway will automatically build and deploy
3. Monitor the deployment logs in Railway dashboard
4. Once deployed, visit your app at the Railway-provided URL

## Step 8: Verify Deployment

1. Visit your Railway app URL
2. Try logging in with Google OAuth
3. Test connecting social media accounts
4. Check the Railway logs for any errors

## Troubleshooting

### Build Failures
- Check Railway build logs
- Ensure Dockerfile is in the root directory
- Verify Java 17 is specified in Dockerfile

### OAuth Errors
- Verify all redirect URIs match exactly (including https://)
- Check that OAuth credentials are correctly set in Railway environment variables
- Ensure OAuth apps are published/approved by the platforms

### Database Connection Issues
- Verify MONGODB_URI is correctly copied from Railway MongoDB service
- Check MongoDB service is running in Railway
- Ensure network connectivity between services

### SSL/HTTPS Issues
- Railway handles SSL automatically via reverse proxy
- Ensure `server.ssl.enabled=false` in application-prod.properties
- Verify `server.forward-headers-strategy=framework` is set

## Monitoring

- View logs: Railway Dashboard → Your Service → Deployments → View Logs
- Monitor metrics: Railway Dashboard → Your Service → Metrics
- Set up health checks in Railway service settings

## Cost Optimization

- Railway offers a free tier with $5/month credit
- MongoDB will use most of your resources
- Consider upgrading to a paid plan for production use
- Monitor usage in Railway dashboard

## Rolling Back

If a deployment fails:
1. Go to Railway Dashboard → Deployments
2. Find a previous successful deployment
3. Click "Redeploy"

## Continuous Deployment

Railway automatically redeploys when you push to your main branch. To disable:
1. Go to Service Settings
2. Under "Source"
3. Toggle "Auto Deploy" off

## Support

- Railway Documentation: https://docs.railway.app
- Railway Discord: https://discord.gg/railway
- Railway Help: https://help.railway.app
