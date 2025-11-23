# Google OAuth Setup Guide - Fix "Access blocked: Authorization Error"

## Problem
You're getting "Access blocked: Authorization Error" when trying to log in with Google.

## Solution

### Step 1: Configure Google Cloud Console

1. **Go to Google Cloud Console**
   - Visit: https://console.cloud.google.com/

2. **Select or Create a Project**
   - Click on the project dropdown at the top
   - Select your existing project or create a new one

3. **Enable Required APIs**
   - Go to "APIs & Services" → "Library"
   - Search for and enable:
     - Google+ API
     - Google OAuth2 API

4. **Configure OAuth Consent Screen**
   - Go to "APIs & Services" → "OAuth consent screen"
   - Choose **User Type**:
     - **External** (for testing with any Google account)
     - **Internal** (only if you have Google Workspace)
   - Click "Create"

   **App Information:**
   - App name: `Cliq24`
   - User support email: Your email
   - Developer contact: Your email

   **Scopes:**
   - Click "Add or Remove Scopes"
   - Add these scopes:
     - `openid`
     - `email`
     - `profile`
     - `.../auth/userinfo.email`
     - `.../auth/userinfo.profile`

   **Test Users (for External apps):**
   - Click "Add Users"
   - Add your Google email address
   - Add any other test users

   **Publishing Status:**
   - For development: Leave as "Testing"
   - For production: Click "Publish App"

5. **Create OAuth 2.0 Credentials**
   - Go to "APIs & Services" → "Credentials"
   - Click "Create Credentials" → "OAuth 2.0 Client ID"
   - Application type: **Web application**
   - Name: `Cliq24 Backend`

   **Authorized JavaScript origins:**
   ```
   http://localhost:8080
   http://localhost:3000
   ```

   **Authorized redirect URIs:**
   ```
   http://localhost:8080/login/oauth2/code/google
   http://localhost:8080/auth/google/callback
   ```

   For production, also add:
   ```
   https://your-domain.com/login/oauth2/code/google
   https://your-domain.com/auth/google/callback
   ```

6. **Copy Your Credentials**
   - Copy the **Client ID**
   - Copy the **Client Secret**

### Step 2: Set Environment Variables

**On Windows (Command Prompt):**
```cmd
set GOOGLE_CLIENT_ID=your-client-id-here
set GOOGLE_CLIENT_SECRET=your-client-secret-here
```

**On Windows (PowerShell):**
```powershell
$env:GOOGLE_CLIENT_ID="your-client-id-here"
$env:GOOGLE_CLIENT_SECRET="your-client-secret-here"
```

**On Linux/Mac:**
```bash
export GOOGLE_CLIENT_ID=your-client-id-here
export GOOGLE_CLIENT_SECRET=your-client-secret-here
```

**Or create a `.env` file** (if using docker-compose):
```
GOOGLE_CLIENT_ID=your-client-id-here
GOOGLE_CLIENT_SECRET=your-client-secret-here
```

### Step 3: Restart Your Application

After setting the environment variables, restart your Spring Boot application.

## Testing the OAuth Flow

### Option 1: Using Browser
1. Start your backend: `./mvnw spring-boot:run`
2. Open browser: `http://localhost:8080/oauth2/authorization/google`
3. You should be redirected to Google login
4. After login, you'll be redirected back to your app

### Option 2: Using Your Frontend
If you have a frontend app:
1. Start backend on port 8080
2. Start frontend on port 3000
3. Click "Login with Google" button
4. Frontend should redirect to: `http://localhost:8080/oauth2/authorization/google`

## Common Issues & Solutions

### Issue 1: "Access blocked: Authorization Error"
**Cause:** App is not published or test users not added

**Solution:**
- Add your email to "Test Users" in OAuth consent screen
- OR publish your app (go to OAuth consent screen → Publish App)

### Issue 2: "redirect_uri_mismatch"
**Cause:** Redirect URI doesn't match what's configured in Google Console

**Solution:**
- Make sure the redirect URI in Google Console exactly matches:
  `http://localhost:8080/login/oauth2/code/google`
- No trailing slash
- Correct protocol (http vs https)
- Correct port number

### Issue 3: "Invalid client"
**Cause:** Wrong Client ID or Client Secret

**Solution:**
- Double-check your environment variables
- Make sure there are no extra spaces
- Verify the credentials in Google Console

### Issue 4: "Scope error"
**Cause:** Missing required scopes in OAuth consent screen

**Solution:**
- Go to OAuth consent screen → Edit App
- Add scopes: openid, email, profile

## Verify Configuration

### Check Environment Variables
```bash
# Windows CMD
echo %GOOGLE_CLIENT_ID%
echo %GOOGLE_CLIENT_SECRET%

# Windows PowerShell
echo $env:GOOGLE_CLIENT_ID
echo $env:GOOGLE_CLIENT_SECRET%

# Linux/Mac
echo $GOOGLE_CLIENT_ID
echo $GOOGLE_CLIENT_SECRET
```

### Check Application Properties
The file `src/main/resources/application.properties` should have:
```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,email,profile
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/google
```

## API Endpoints

Once OAuth is working:

**Initiate Login:**
```
GET http://localhost:8080/oauth2/authorization/google
```

**Callback (handled automatically by Spring Security):**
```
GET http://localhost:8080/login/oauth2/code/google?code=...&state=...
```

**Get Current User:**
```
GET http://localhost:8080/auth/me
Headers: Authorization: Bearer YOUR_JWT_TOKEN
```

## Debugging

Enable debug logging in `application.properties`:
```properties
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.security.oauth2=DEBUG
```

Check the logs for:
- OAuth redirect URL
- Token exchange
- User info retrieval

## Production Checklist

Before deploying to production:

- [ ] Create production OAuth credentials in Google Console
- [ ] Add production redirect URIs (https://yourdomain.com/...)
- [ ] Publish OAuth consent screen
- [ ] Add privacy policy URL
- [ ] Add terms of service URL
- [ ] Set production environment variables
- [ ] Test with multiple Google accounts
- [ ] Enable HTTPS on your server
- [ ] Update CORS origins to production domain

## Summary

The key steps are:
1. ✅ Configure OAuth consent screen in Google Console
2. ✅ Add test users (your email)
3. ✅ Create OAuth 2.0 credentials
4. ✅ Add correct redirect URI: `http://localhost:8080/login/oauth2/code/google`
5. ✅ Set environment variables with Client ID and Secret
6. ✅ Restart your application

After following these steps, the "Access blocked" error should be resolved!
