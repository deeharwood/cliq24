# Cliq24 Frontend Dashboard

## Overview

The Cliq24 dashboard features a premium **Mercedes-Benz ambient lighting** inspired design, providing a luxurious and immersive experience for tracking your social media performance across multiple platforms.

## Design Philosophy

The dashboard is inspired by Mercedes-Benz's ambient lighting system, featuring:

- **Dark Premium Background**: Deep blacks with subtle gradients mimicking a car dashboard at night
- **Ambient Glows**: Soft blue, purple, and teal glows that pulse and animate
- **Glassmorphism**: Translucent panels with frosted glass effects
- **Smooth Animations**: Buttery-smooth transitions and hover effects
- **Premium Typography**: Clean, modern Inter font family
- **Responsive Design**: Adapts beautifully to all screen sizes

## Features

### Main Hero Pod
- **Overall Performance Score**: Circular progress indicator with gradient
- **Live Sync Indicator**: Shows real-time connection status
- **Aggregate Statistics**: Total accounts, followers, and posts
- **Engagement Labels**: Dynamic labels based on performance (Crushing It! 🔥, Doing Well 👍, etc.)

### Social Media Pods
Individual pods for each connected platform:
- Facebook
- Instagram
- Twitter
- LinkedIn
- TikTok
- YouTube
- Snapchat

Each pod displays:
- Platform-specific color scheme and glow effects
- Engagement score
- Follower count
- Posts count
- Pending responses
- New messages

### Features
- **Click to Sync**: Click any pod to sync that account's data
- **Auto-Refresh**: Automatically syncs data every 5 minutes
- **Connect New Platforms**: Modal to add new social media accounts
- **Demo Mode**: Runs with demo data if not authenticated

## Color Palette

```css
/* Mercedes-inspired ambient colors */
--ambient-blue: #00d4ff
--ambient-purple: #b24bf3
--ambient-teal: #00ffcc
--ambient-pink: #ff006e

/* Dark premium background */
--bg-primary: #0a0a0f
--bg-secondary: #141419
--bg-tertiary: #1a1a24
```

## Platform-Specific Colors

- **Facebook**: #1877f2 (Blue)
- **Instagram**: #e4405f (Pink/Gradient)
- **Twitter**: #1da1f2 (Light Blue)
- **LinkedIn**: #0a66c2 (Professional Blue)
- **TikTok**: #00f2ea (Cyan)
- **YouTube**: #ff0000 (Red)
- **Snapchat**: #fffc00 (Yellow)

## File Structure

```
static/
├── index.html      # Main HTML structure
├── style.css       # Premium Mercedes-Benz styling
├── app.js          # JavaScript application logic
└── README.md       # This file
```

## How It Works

### Authentication
1. User visits the dashboard
2. If no JWT token is found, demo mode loads
3. Click the brand logo to initiate Google OAuth login
4. After successful login, JWT token is stored and dashboard loads real data

### API Integration
The dashboard communicates with the Spring Boot backend via REST API:

- `GET /auth/me` - Get current user information
- `GET /api/social-accounts` - Get all connected social accounts
- `POST /api/social-accounts/{platform}` - Connect new account
- `POST /api/social-accounts/{accountId}/sync` - Sync account data
- `DELETE /api/social-accounts/{accountId}` - Disconnect account

### Score Calculation
The engagement score (0-100) is calculated by the backend using:
- 30% - Connections/Followers
- 50% - Posts Activity
- 20% - Response Rate

Score labels:
- **80-100**: "Crushing It! 🔥"
- **60-79**: "Doing Well 👍"
- **40-59**: "Needs Attention ⚠️"
- **0-39**: "Falling Behind 📉"

## Animations

### Ambient Background
- Three floating orbs with blur effects
- 20-second looping animations
- Color gradients: blue, purple, teal

### Logo Pulse
- 3-second pulsing glow effect
- Alternates between blue and purple shadows

### Sync Indicator
- Pulsing dot animation
- 2-second loop to indicate live connection

### Pod Interactions
- Hover: Lifts 8px with enhanced glow
- Click: Syncs data with visual feedback
- Smooth 0.4s cubic-bezier transitions

## Responsive Breakpoints

```css
@media (max-width: 768px) {
    /* Stack hero score vertically */
    /* Single column pods grid */
    /* 2-column platform grid in modal */
}
```

## Browser Support

- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)
- Mobile browsers (iOS Safari, Chrome Android)

## Customization

### Change Ambient Colors
Edit the CSS variables in `style.css`:

```css
:root {
    --ambient-blue: #your-color;
    --ambient-purple: #your-color;
    --ambient-teal: #your-color;
}
```

### Adjust Animation Speed
Modify animation durations in `style.css`:

```css
animation: float 20s infinite ease-in-out; /* Change 20s */
```

### Add New Platforms
1. Add platform configuration in `app.js` `getPlatformIcon()` function
2. Add CSS color scheme in `style.css` (`.pod-yourplatform`)
3. Add button in modal HTML

## Performance

- Lightweight: ~50KB total (HTML + CSS + JS)
- No external dependencies except Google Fonts
- Optimized CSS with CSS variables
- Efficient JavaScript with class-based architecture
- Lazy loading of data

## Future Enhancements

- [ ] Real-time WebSocket updates
- [ ] Advanced analytics charts
- [ ] Dark/Light theme toggle
- [ ] Customizable color schemes
- [ ] Export reports functionality
- [ ] Mobile app (PWA)
- [ ] Notifications system
- [ ] Keyboard shortcuts

## Troubleshooting

### Dashboard shows "No Accounts Connected"
- Ensure backend is running
- Check browser console for API errors
- Verify JWT token is valid

### Pods not loading
- Check network tab for failed API calls
- Ensure CORS is properly configured on backend
- Verify social accounts exist in database

### Animations stuttering
- Check browser hardware acceleration
- Reduce animation complexity in CSS
- Clear browser cache

## Credits

Design inspired by Mercedes-Benz ambient lighting system and premium automotive dashboard UI/UX principles.

Built with vanilla JavaScript, HTML5, and CSS3 for optimal performance and compatibility.
