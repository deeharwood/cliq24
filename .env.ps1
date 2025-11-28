# Cliq24 Environment Variables
# Load these before starting the app: . .\.env.ps1

# Google OAuth (from your client_secret JSON file)
$env:GOOGLE_CLIENT_ID="1047793516143-4ba93l5a0sjvntjn54scd9g6pmu3vbnu.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET="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"

# Other platforms (placeholders - add real credentials when ready)
$env:FACEBOOK_APP_ID="placeholder"
$env:FACEBOOK_APP_SECRET="placeholder"
$env:TWITTER_API_KEY="placeholder"
$env:TWITTER_API_SECRET="placeholder"
$env:TWITTER_ACCESS_TOKEN="placeholder"
$env:TWITTER_ACCESS_SECRET="placeholder"
$env:LINKEDIN_CLIENT_ID="placeholder"
$env:LINKEDIN_CLIENT_SECRET="placeholder"
$env:TIKTOK_CLIENT_KEY="placeholder"
$env:TIKTOK_CLIENT_SECRET="placeholder"
$env:YOUTUBE_API_KEY="placeholder"

Write-Host "✅ Environment variables loaded!" -ForegroundColor Green
Write-Host "Google Client ID: $env:GOOGLE_CLIENT_ID" -ForegroundColor Cyan
Write-Host ""
Write-Host "Now run: .\mvnw.cmd spring-boot:run" -ForegroundColor Yellow
