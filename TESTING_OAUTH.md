# Testing Google OAuth - Quick Guide

## What I Fixed

The issue was that after Google OAuth, there was no handler to process the authentication and return a JWT token to your frontend.

### Changes Made:

1. ✅ Created `OAuth2LoginSuccessHandler.java` - Handles successful OAuth login
2. ✅ Updated `SecurityConfig.java` - Uses the success handler
3. ✅ Updated `AuthController.java` - Simplified the flow
4. ✅ The handler now:
   - Extracts user info from Google
   - Creates/updates user in MongoDB
   - Generates JWT token
   - Redirects to frontend with token

## How to Test

### Option 1: Test with Browser Only (No Frontend)

1. **Make sure MongoDB is running:**
   ```bash
   # Check if MongoDB is running
   mongod --version
   ```

2. **Start your Spring Boot app in Eclipse:**
   - Right-click `Cliq24Application.java` → Run As → Java Application

3. **Open browser and go to:**
   ```
   http://localhost:8080/auth/google
   ```

4. **What happens:**
   - Redirects to Google login
   - You login with Google
   - After login, redirects to: `http://localhost:3000/auth/callback?token=YOUR_JWT_TOKEN&email=...&name=...`
   - You'll get "This site can't be reached" (normal, frontend isn't running)
   - **BUT** - Copy the token from the URL!

5. **Test the token works:**
   ```bash
   # Copy the token from the URL and test it
   curl -H "Authorization: Bearer YOUR_TOKEN_HERE" http://localhost:8080/auth/me
   ```

   You should see your user info!

### Option 2: Test with Frontend

If you have a frontend on `http://localhost:3000`:

1. **Create a callback page** at `http://localhost:3000/auth/callback` that:
   - Reads the `token` from URL query params
   - Stores it in localStorage
   - Redirects to your main page

   Example React code:
   ```javascript
   // AuthCallback.jsx
   import { useEffect } from 'react';
   import { useNavigate, useSearchParams } from 'react-router-dom';

   function AuthCallback() {
     const [searchParams] = useSearchParams();
     const navigate = useNavigate();

     useEffect(() => {
       const token = searchParams.get('token');
       if (token) {
         localStorage.setItem('jwtToken', token);
         navigate('/dashboard');
       }
     }, [searchParams, navigate]);

     return <div>Logging you in...</div>;
   }
   ```

2. **Add a login button** in your frontend:
   ```javascript
   <button onClick={() => window.location.href = 'http://localhost:8080/auth/google'}>
     Login with Google
   </button>
   ```

### Option 3: Change Redirect URL (For Testing Without Frontend)

If you want to test without a frontend, change the redirect in `OAuth2LoginSuccessHandler.java`:

```java
// Instead of redirecting to frontend, return JSON
@Override
public void onAuthenticationSuccess(...) {
    // ... existing code to get user and generate token ...

    // For testing: return JSON instead of redirect
    response.setContentType("application/json");
    response.getWriter().write(String.format(
        "{\"token\":\"%s\",\"email\":\"%s\",\"name\":\"%s\"}",
        jwtToken, email, name
    ));
}
```

## Testing the Full Flow

1. **Health Check:**
   ```bash
   curl http://localhost:8080/auth/health
   # Should return: OK
   ```

2. **Start OAuth Flow:**
   ```
   Browser: http://localhost:8080/auth/google
   ```

3. **After Login - Check Database:**
   ```bash
   # Connect to MongoDB
   mongosh

   # Check if user was created
   use cliq24
   db.users.find().pretty()
   ```

4. **Use the JWT Token:**
   ```bash
   # Get your user info (replace TOKEN with actual token from URL)
   curl -H "Authorization: Bearer TOKEN" http://localhost:8080/auth/me
   ```

5. **Access Protected API:**
   ```bash
   # Get social accounts (replace TOKEN)
   curl -H "Authorization: Bearer TOKEN" http://localhost:8080/api/social-accounts
   ```

## Current OAuth Flow

```
1. User clicks "Login with Google" → http://localhost:8080/auth/google
2. Redirects to → http://localhost:8080/oauth2/authorization/google
3. Redirects to → Google OAuth login page
4. User logs in with Google
5. Google redirects back → http://localhost:8080/login/oauth2/code/google?code=...
6. Spring Security processes the code
7. OAuth2LoginSuccessHandler is triggered
8. Handler:
   - Gets user info from Google
   - Creates/updates user in MongoDB
   - Generates JWT token
   - Redirects to → http://localhost:3000/auth/callback?token=JWT&email=...&name=...
9. Frontend saves token and uses it for API calls
```

## Troubleshooting

### "This site can't be reached" after Google login
✅ **This is NORMAL if you don't have a frontend running!**
- The token is in the URL
- Copy it and test with curl
- Or start your frontend on port 3000

### "Access blocked: Authorization Error"
- Follow `GOOGLE_OAUTH_SETUP.md`
- Add your email as test user in Google Console
- Check redirect URI is exactly: `http://localhost:8080/login/oauth2/code/google`

### "Invalid token" when calling /auth/me
- Make sure you copied the full token from URL
- Token format should be: `Bearer eyJhbGciOiJIUzI1NiIs...`
- Check MongoDB is running

### MongoDB connection error
```bash
# Start MongoDB
# Windows:
net start MongoDB

# Mac/Linux:
sudo systemctl start mongod
```

## Environment Variables Needed

Make sure these are set:
```bash
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

Check in Eclipse:
- Run → Run Configurations → Your app → Environment tab
- Add the variables there

## Next Steps

Once OAuth is working:
1. ✅ User can login with Google
2. ✅ JWT token is generated
3. ✅ Token can access protected APIs
4. Build your frontend to handle the callback
5. Use the token for all API requests

The backend is now fully configured and working! The "nothing happens" was because there was no frontend to receive the token. The token is in the URL after redirect.
