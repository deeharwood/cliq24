# Cliq24 Startup Script with Environment Variables
# This script sets all required environment variables and starts the Spring Boot application

Write-Host "Setting environment variables..." -ForegroundColor Green

# Set JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
Write-Host "JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Yellow

# MongoDB Configuration
$env:MONGODB_URI = "mongodb://localhost:27017/cliq24"

# JWT Configuration
$env:JWT_SECRET = "your-super-secret-jwt-key-change-this-in-production-must-be-at-least-256-bits"

# Encryption Key (256-bit hex string)
$env:ENCRYPTION_KEY = "0123456789abcdef0123456789abcdef"

# Google OAuth
$env:GOOGLE_CLIENT_ID = "1047793516143-4ba93l5a0sjvntjn54scd9g6pmu3vbnu.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET = "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"

# Facebook OAuth
$env:FACEBOOK_APP_ID = "your-facebook-app-id"
$env:FACEBOOK_APP_SECRET = "your-facebook-app-secret"

# Twitter API
$env:TWITTER_API_KEY = "your-twitter-api-key"
$env:TWITTER_API_SECRET = "your-twitter-api-secret"
$env:TWITTER_ACCESS_TOKEN = "your-twitter-access-token"
$env:TWITTER_ACCESS_SECRET = "your-twitter-access-secret"

# LinkedIn OAuth
$env:LINKEDIN_CLIENT_ID = "your-linkedin-client-id"
$env:LINKEDIN_CLIENT_SECRET = "your-linkedin-client-secret"

# TikTok OAuth
$env:TIKTOK_CLIENT_KEY = "your-tiktok-client-key"
$env:TIKTOK_CLIENT_SECRET = "your-tiktok-client-secret"

# YouTube API (uses Google OAuth)
$env:YOUTUBE_API_KEY = "your-youtube-api-key"

Write-Host "Environment variables set successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Google Client ID: $env:GOOGLE_CLIENT_ID" -ForegroundColor Cyan
Write-Host ""
Write-Host "Starting Spring Boot application..." -ForegroundColor Green
Write-Host ""

# Start the Spring Boot application
.\mvnw.cmd spring-boot:run
