# Cliq24 Backend - Deployment Guide

## Overview
Cliq24 is a Spring Boot application that helps users manage multiple social media accounts with engagement tracking and scoring.

## Prerequisites

### Development Environment
- Java 17 or higher
- Maven 3.6+
- MongoDB 4.4+
- Git

### Production Environment
- Java 17 Runtime
- MongoDB (Atlas recommended for cloud deployment)
- Cloud platform (AWS, GCP, Azure, or Heroku)

## Local Development Setup

### 1. Clone the Repository
```bash
git clone <your-repository-url>
cd cliq24
```

### 2. Configure MongoDB
Make sure MongoDB is running locally:
```bash
# On Windows
net start MongoDB

# On Mac/Linux
sudo systemctl start mongod
```

### 3. Set Environment Variables
Create a `.env` file in the project root or set these environment variables:

```bash
# Google OAuth
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret

# Facebook OAuth
export FACEBOOK_APP_ID=your-facebook-app-id
export FACEBOOK_APP_SECRET=your-facebook-app-secret

# Twitter API
export TWITTER_API_KEY=your-twitter-api-key
export TWITTER_API_SECRET=your-twitter-api-secret
export TWITTER_ACCESS_TOKEN=your-twitter-access-token
export TWITTER_ACCESS_SECRET=your-twitter-access-secret

# LinkedIn OAuth
export LINKEDIN_CLIENT_ID=your-linkedin-client-id
export LINKEDIN_CLIENT_SECRET=your-linkedin-client-secret

# TikTok OAuth
export TIKTOK_CLIENT_KEY=your-tiktok-client-key
export TIKTOK_CLIENT_SECRET=your-tiktok-client-secret

# YouTube API
export YOUTUBE_API_KEY=your-youtube-api-key

# MongoDB (optional, default is localhost)
export MONGODB_URI=mongodb://localhost:27017/cliq24
```

### 4. Build the Application
```bash
# Using Maven wrapper (recommended)
./mvnw clean install

# Or using installed Maven
mvn clean install
```

### 5. Run the Application
```bash
# Using Maven
./mvnw spring-boot:run

# Or run the JAR directly
java -jar target/cliq24-backend-1.0.0.jar
```

The application will start on `http://localhost:8080`

## Testing the API

### Health Check
```bash
curl http://localhost:8080/health
```

### Test Google OAuth Login
```bash
curl http://localhost:8080/auth/google
```

### Get User Info (with JWT token)
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" http://localhost:8080/auth/me
```

## Production Deployment

### Option 1: Deploy to Heroku

1. **Install Heroku CLI**
   ```bash
   # Download from https://devcenter.heroku.com/articles/heroku-cli
   ```

2. **Login to Heroku**
   ```bash
   heroku login
   ```

3. **Create Heroku App**
   ```bash
   heroku create cliq24-backend
   ```

4. **Add MongoDB Add-on**
   ```bash
   heroku addons:create mongolab:sandbox
   ```

5. **Set Environment Variables**
   ```bash
   heroku config:set GOOGLE_CLIENT_ID=your-google-client-id
   heroku config:set GOOGLE_CLIENT_SECRET=your-google-client-secret
   heroku config:set FACEBOOK_APP_ID=your-facebook-app-id
   # ... set all other environment variables
   ```

6. **Deploy**
   ```bash
   git push heroku main
   ```

7. **Open Application**
   ```bash
   heroku open
   ```

### Option 2: Deploy to AWS Elastic Beanstalk

1. **Install AWS CLI and EB CLI**
   ```bash
   pip install awscli awsebcli
   ```

2. **Initialize EB**
   ```bash
   eb init -p java-17 cliq24-backend
   ```

3. **Create Environment**
   ```bash
   eb create cliq24-prod
   ```

4. **Set Environment Variables**
   ```bash
   eb setenv GOOGLE_CLIENT_ID=your-google-client-id \
             GOOGLE_CLIENT_SECRET=your-google-client-secret \
             MONGODB_URI=your-mongodb-atlas-uri
   # ... set all other environment variables
   ```

5. **Deploy**
   ```bash
   eb deploy
   ```

### Option 3: Docker Deployment

1. **Create Dockerfile** (if not exists)
   ```dockerfile
   FROM openjdk:17-jdk-slim
   WORKDIR /app
   COPY target/cliq24-backend-1.0.0.jar app.jar
   EXPOSE 8080
   ENTRYPOINT ["java", "-jar", "app.jar"]
   ```

2. **Build Docker Image**
   ```bash
   docker build -t cliq24-backend .
   ```

3. **Run Docker Container**
   ```bash
   docker run -p 8080:8080 \
     -e GOOGLE_CLIENT_ID=your-google-client-id \
     -e MONGODB_URI=your-mongodb-uri \
     cliq24-backend
   ```

4. **Or use Docker Compose**
   Create `docker-compose.yml`:
   ```yaml
   version: '3.8'
   services:
     app:
       build: .
       ports:
         - "8080:8080"
       environment:
         - MONGODB_URI=mongodb://mongo:27017/cliq24
         - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
         - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
       depends_on:
         - mongo

     mongo:
       image: mongo:latest
       ports:
         - "27017:27017"
       volumes:
         - mongodb_data:/data/db

   volumes:
     mongodb_data:
   ```

   Run:
   ```bash
   docker-compose up -d
   ```

## MongoDB Setup

### Using MongoDB Atlas (Recommended for Production)

1. Create account at https://www.mongodb.com/cloud/atlas
2. Create a new cluster
3. Create database user with password
4. Whitelist your IP address (or use 0.0.0.0/0 for testing)
5. Get connection string:
   ```
   mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/cliq24?retryWrites=true&w=majority
   ```
6. Set as `MONGODB_URI` environment variable

## OAuth Configuration

### Google OAuth Setup
1. Go to https://console.cloud.google.com/
2. Create a new project
3. Enable Google+ API
4. Create OAuth 2.0 credentials
5. Add authorized redirect URIs:
   - `http://localhost:8080/auth/google/callback` (development)
   - `https://your-domain.com/auth/google/callback` (production)
6. Copy Client ID and Client Secret

### Facebook OAuth Setup
1. Go to https://developers.facebook.com/
2. Create a new app
3. Add Facebook Login product
4. Configure OAuth redirect URIs
5. Copy App ID and App Secret

### Other Platforms
Follow similar steps for Twitter, LinkedIn, TikTok, and YouTube APIs.

## Security Considerations

### Production Checklist
- [ ] Change JWT secret in production
- [ ] Use strong encryption key (256-bit)
- [ ] Enable HTTPS/SSL
- [ ] Configure CORS properly
- [ ] Use MongoDB authentication
- [ ] Set up proper firewall rules
- [ ] Enable MongoDB encryption at rest
- [ ] Use environment variables for secrets
- [ ] Enable rate limiting
- [ ] Set up monitoring and logging
- [ ] Regular security updates

### Update JWT Secret
```bash
# Generate a secure random key
openssl rand -base64 64

# Set in environment
export JWT_SECRET=your-generated-secret-key
```

## Monitoring and Logging

### Application Logs
Logs are configured using Log4j2. Check `src/main/resources/log4j2-spring.xml` for configuration.

### Health Checks
Monitor the application health:
```bash
curl https://your-domain.com/health
```

### Database Monitoring
Use MongoDB Atlas monitoring dashboard for:
- Query performance
- Connection pooling
- Disk usage
- Alerts

## Troubleshooting

### Application won't start
- Check Java version: `java -version`
- Verify MongoDB connection
- Check environment variables are set
- Review application logs

### Authentication errors
- Verify OAuth credentials are correct
- Check redirect URIs match exactly
- Ensure CORS is configured properly

### Database connection issues
- Verify MongoDB is running
- Check connection string
- Ensure IP is whitelisted (MongoDB Atlas)
- Verify network/firewall settings

## API Endpoints

### Authentication
- `GET /auth/google` - Initiate Google OAuth
- `GET /auth/google/callback` - Google OAuth callback
- `GET /auth/me` - Get current user info

### Social Accounts
- `GET /api/social-accounts` - Get all connected accounts
- `POST /api/social-accounts/{platform}` - Connect new account
- `DELETE /api/social-accounts/{accountId}` - Disconnect account
- `POST /api/social-accounts/{accountId}/sync` - Sync account metrics

## Environment Variables Reference

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `MONGODB_URI` | MongoDB connection string | Yes | mongodb://localhost:27017/cliq24 |
| `JWT_SECRET` | Secret key for JWT tokens | Yes | (must change in production) |
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID | Yes | - |
| `GOOGLE_CLIENT_SECRET` | Google OAuth Secret | Yes | - |
| `FACEBOOK_APP_ID` | Facebook App ID | Yes | - |
| `FACEBOOK_APP_SECRET` | Facebook App Secret | Yes | - |
| `TWITTER_API_KEY` | Twitter API Key | Optional | - |
| `LINKEDIN_CLIENT_ID` | LinkedIn Client ID | Optional | - |
| `TIKTOK_CLIENT_KEY` | TikTok Client Key | Optional | - |
| `YOUTUBE_API_KEY` | YouTube API Key | Optional | - |

## Support

For issues or questions:
- Create an issue in the GitHub repository
- Contact: support@cliq24.app
- Documentation: https://docs.cliq24.app

## License

Copyright © 2024 Cliq24. All rights reserved.
