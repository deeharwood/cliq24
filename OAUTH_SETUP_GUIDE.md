# OAuth Setup Guide for Cliq24

## Prerequisites

1. **MongoDB** - Running on localhost:27017
2. **Google Cloud Account** - For OAuth credentials
3. **Environment Variables** - Set before starting the app

## Step 1: Install and Start MongoDB

### Windows:

**Download MongoDB:**
```
https://www.mongodb.com/try/download/community
```

**Install MongoDB Community Edition**

**Start MongoDB:**
```bash
# Option 1: Start as Windows Service (if installed as service)
net start MongoDB

# Option 2: Start manually
"C:\Program Files\MongoDB\Server\7.0\bin\mongod.exe" --dbpath="C:\data\db"
```

**Verify MongoDB is running:**
```bash
# Open another terminal
mongosh
# Should connect successfully
```

## Step 2: Set Up Google OAuth 2.0

### Create OAuth Credentials:

1. **Go to Google Cloud Console:**
   ```
   https://console.cloud.google.com/
   ```

2. **Create a New Project** (or select existing):
   - Click "Select a project" → "New Project"
   - Name: "Cliq24"
   - Click "Create"

3. **Enable Google+ API:**
   - Go to "APIs & Services" → "Library"
   - Search for "Google+ API"
   - Click "Enable"

4. **Create OAuth Consent Screen:**
   - Go to "APIs & Services" → "OAuth consent screen"
   - Choose "External" → "Create"
   - Fill in:
     - App name: `Cliq24`
     - User support email: Your email
     - Developer contact: Your email
   - Click "Save and Continue"
   - Scopes: Click "Add or Remove Scopes"
     - Select: `openid`, `email`, `profile`
   - Click "Save and Continue"
   - Test users: Add your email
   - Click "Save and Continue"

5. **Create OAuth Client ID:**
   - Go to "APIs & Services" → "Credentials"
   - Click "Create Credentials" → "OAuth client ID"
   - Application type: `Web application`
   - Name: `Cliq24 Web Client`
   - Authorized redirect URIs:
     ```
     http://localhost:8080/login/oauth2/code/google
     ```
   - Click "Create"
   - **Copy the Client ID and Client Secret** (you'll need these!)

## Step 3: Set Environment Variables

### Windows (PowerShell):

Create a file `.env.ps1` in your project root:

```powershell
# Google OAuth
$env:GOOGLE_CLIENT_ID="YOUR_GOOGLE_CLIENT_ID_HERE"
$env:GOOGLE_CLIENT_SECRET="YOUR_GOOGLE_CLIENT_SECRET_HERE"

# Optional: Other platforms (leave as placeholders for now)
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
```

**Load environment variables before starting:**
```powershell
. .\.env.ps1
.\mvnw.cmd spring-boot:run
```

### Alternative: System Environment Variables

1. Press `Win + R`, type `sysdm.cpl`, press Enter
2. Go to "Advanced" tab → "Environment Variables"
3. Under "User variables", click "New"
4. Add:
   - Variable name: `GOOGLE_CLIENT_ID`
   - Variable value: `YOUR_CLIENT_ID`
5. Repeat for `GOOGLE_CLIENT_SECRET`
6. Click OK
7. **Restart your terminal**

## Step 4: Verify MongoDB Connection

Before starting the app, verify MongoDB is accessible:

```bash
mongosh
```

Should show:
```
Current Mongosh Log ID: ...
Connecting to: mongodb://127.0.0.1:27017/...
Using MongoDB: ...
```

Create the database (optional):
```javascript
use cliq24
db.users.insertOne({test: "data"})
db.users.deleteMany({})
```

## Step 5: Start the Application

```bash
# Make sure MongoDB is running
# Make sure environment variables are set

.\mvnw.cmd spring-boot:run
```

Look for successful startup:
```
Started Cliq24Application in X.XXX seconds
```

## Step 6: Test OAuth Login

1. **Open browser:**
   ```
   http://localhost:8080
   ```

2. **Click the Cliq24 logo** (top-left) to initiate login

3. **You'll be redirected to Google login**

4. **Sign in with Google account**

5. **Grant permissions** to Cliq24

6. **You'll be redirected back** to the dashboard with your name showing

## Step 7: Verify Authentication

**Check if you're logged in:**
- Your name should appear in the top-right
- Demo mode notification should disappear
- You can now connect real social media accounts

**Test API access:**
Open browser console (F12) and run:
```javascript
fetch('/api/social-accounts', {
  headers: {
    'Authorization': 'Bearer ' + localStorage.getItem('cliq24_jwt')
  }
})
.then(r => r.json())
.then(console.log)
```

Should return your social accounts (empty array if none connected).

## Troubleshooting

### MongoDB Connection Failed

**Error:** `com.mongodb.MongoTimeoutException: Timed out after 30000 ms`

**Solution:**
```bash
# Check if MongoDB is running
mongosh

# If not running, start it
net start MongoDB

# Or manually:
"C:\Program Files\MongoDB\Server\7.0\bin\mongod.exe" --dbpath="C:\data\db"
```

### OAuth Error: redirect_uri_mismatch

**Error:** `The redirect URI in the request did not match a registered redirect URI`

**Solution:**
- Go back to Google Cloud Console
- APIs & Services → Credentials
- Edit your OAuth Client ID
- Ensure redirect URI is exactly:
  ```
  http://localhost:8080/login/oauth2/code/google
  ```
- No trailing slash!

### Environment Variables Not Set

**Error:** `Could not resolve placeholder 'GOOGLE_CLIENT_ID'`

**Solution:**
```powershell
# Load environment variables
. .\.env.ps1

# Verify they're set
echo $env:GOOGLE_CLIENT_ID

# Then start app
.\mvnw.cmd spring-boot:run
```

### JWT Token Issues

**Error:** Token not being stored

**Solution:**
- Check browser console for errors
- Verify JavaScript is loading: `http://localhost:8080/app.js`
- Check Network tab - look for redirect with `?token=...`

## Next Steps

Once Google OAuth is working:

1. **Add other social platforms:**
   - Set up Facebook OAuth
   - Set up Twitter OAuth
   - Set up LinkedIn OAuth
   - etc.

2. **Implement social media APIs:**
   - Connect to actual platform APIs
   - Fetch real follower counts
   - Calculate real engagement scores

3. **Deploy to production:**
   - Update redirect URIs
   - Use environment-specific configs
   - Set up SSL certificates

## Quick Start Commands

```bash
# 1. Start MongoDB
net start MongoDB

# 2. Load environment variables
. .\.env.ps1

# 3. Start application
.\mvnw.cmd spring-boot:run

# 4. Open browser
start http://localhost:8080
```

---

For more help, check:
- Google OAuth: https://developers.google.com/identity/protocols/oauth2
- MongoDB: https://www.mongodb.com/docs/manual/tutorial/install-mongodb-on-windows/
- Spring Security: https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html
