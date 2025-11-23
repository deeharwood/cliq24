# Cliq24 Backend - Quick Start Guide

## 🚀 Deploy in 5 Minutes

### Option 1: Docker (Recommended)

```bash
# 1. Navigate to project
cd cliq24

# 2. Copy environment template
cp .env.example .env

# 3. Edit .env and add your credentials
# (At minimum, set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET)

# 4. Start the application
docker-compose up -d

# 5. Test it works
curl http://localhost:8080/health
```

✅ Your API is now running at http://localhost:8080

### Option 2: Local Java

```bash
# 1. Ensure Java 17+ and MongoDB are installed
java -version
mongod --version

# 2. Start MongoDB
net start MongoDB  # Windows
sudo systemctl start mongod  # Linux/Mac

# 3. Set environment variables
export GOOGLE_CLIENT_ID=your-client-id
export GOOGLE_CLIENT_SECRET=your-client-secret
# ... set other variables from .env.example

# 4. Build and run
./mvnw spring-boot:run

# 5. Test it works
curl http://localhost:8080/health
```

✅ Your API is now running at http://localhost:8080

## 📋 Before You Start

### Required Credentials

You need OAuth credentials from:
- ✅ **Google** (Required) - https://console.cloud.google.com/
- ⚪ Facebook (Optional)
- ⚪ Twitter (Optional)
- ⚪ LinkedIn (Optional)
- ⚪ TikTok (Optional)
- ⚪ YouTube (Optional)

### Minimum Setup

For basic functionality, you only need:
1. Google OAuth credentials
2. MongoDB running
3. A JWT secret (can use default for testing)

## 🔑 Getting Google OAuth Credentials

1. Go to https://console.cloud.google.com/
2. Create a new project
3. Enable Google+ API
4. Go to "Credentials" → "Create Credentials" → "OAuth 2.0 Client ID"
5. Set authorized redirect URIs:
   - `http://localhost:8080/auth/google/callback`
   - `https://your-domain.com/auth/google/callback` (for production)
6. Copy the Client ID and Client Secret

## 🧪 Test Your Deployment

```bash
# 1. Health check
curl http://localhost:8080/health

# 2. Start OAuth flow (in browser)
open http://localhost:8080/auth/google

# 3. After login, you'll get a JWT token
# Use it to access protected endpoints:
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/auth/me
```

## 🌐 Deploy to Production

### Heroku (Easiest)

```bash
# 1. Install Heroku CLI
# Download from https://devcenter.heroku.com/articles/heroku-cli

# 2. Login and create app
heroku login
heroku create your-app-name

# 3. Add MongoDB
heroku addons:create mongolab:sandbox

# 4. Set environment variables
heroku config:set GOOGLE_CLIENT_ID=your-client-id
heroku config:set GOOGLE_CLIENT_SECRET=your-client-secret
heroku config:set JWT_SECRET=$(openssl rand -base64 64)

# 5. Deploy
git push heroku main

# 6. Open your app
heroku open
```

### AWS / GCP / Azure

See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed instructions.

## 🔧 Configuration

### Environment Variables

Copy `.env.example` to `.env` and fill in:

```bash
# Minimum required
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
MONGODB_URI=mongodb://localhost:27017/cliq24

# Recommended to change
JWT_SECRET=your-super-secret-key-here

# Optional (for additional platforms)
FACEBOOK_APP_ID=...
TWITTER_API_KEY=...
LINKEDIN_CLIENT_ID=...
```

## 📱 API Endpoints

Once running, your API has these endpoints:

### Public Endpoints
- `GET /auth/google` - Start Google login
- `GET /auth/google/callback` - OAuth callback

### Protected Endpoints (need JWT token)
- `GET /auth/me` - Get current user
- `GET /api/social-accounts` - List connected accounts
- `POST /api/social-accounts/{platform}` - Connect account
- `DELETE /api/social-accounts/{id}` - Disconnect account
- `POST /api/social-accounts/{id}/sync` - Sync metrics

## ⚠️ Troubleshooting

### "Cannot connect to MongoDB"
```bash
# Check if MongoDB is running
mongod --version

# Start MongoDB
net start MongoDB  # Windows
sudo systemctl start mongod  # Linux/Mac
```

### "Port 8080 already in use"
```bash
# Change port in application.properties
server.port=8081

# Or kill the process using port 8080
# Windows: netstat -ano | findstr :8080
# Linux/Mac: lsof -i :8080
```

### "Invalid OAuth credentials"
- Double check Client ID and Secret
- Ensure redirect URIs match exactly
- Check that Google+ API is enabled

## 📚 More Information

- **Full Documentation**: [README.md](README.md)
- **Deployment Guide**: [DEPLOYMENT.md](DEPLOYMENT.md)
- **Completion Summary**: [COMPLETION_SUMMARY.md](COMPLETION_SUMMARY.md)

## ✅ Checklist

Before deploying to production:

- [ ] Changed JWT secret to a secure random value
- [ ] Set up MongoDB (preferably MongoDB Atlas)
- [ ] Configured all OAuth credentials
- [ ] Set CORS allowed origins to your frontend domain
- [ ] Enabled HTTPS/SSL
- [ ] Set up monitoring and logging
- [ ] Tested all API endpoints
- [ ] Reviewed security settings

## 🎉 That's It!

Your Cliq24 backend is ready to use. Connect it to your frontend and start managing social media accounts!

Need help? Check the other documentation files or create an issue.
