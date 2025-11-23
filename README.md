# Cliq24 Backend API

A comprehensive social media management platform backend built with Spring Boot that helps users track and manage multiple social media accounts with engagement scoring.

## Features

- **Multi-Platform Support**: Connect and manage Facebook, Instagram, Twitter, LinkedIn, TikTok, YouTube, and Snapchat accounts
- **Google OAuth Authentication**: Secure user authentication using Google OAuth 2.0
- **Engagement Scoring**: Automatic calculation of engagement scores based on account metrics
- **Real-time Metrics**: Sync and track followers, posts, pending responses, and messages
- **RESTful API**: Clean, well-documented REST API endpoints
- **MongoDB Integration**: Scalable NoSQL database for flexible data storage
- **JWT Security**: Token-based authentication for API access
- **Encrypted Tokens**: Secure storage of OAuth tokens with encryption

## Technology Stack

- **Java 17**: Modern Java LTS version
- **Spring Boot 3.1.5**: Latest Spring Boot framework
- **Spring Security**: OAuth2 and JWT authentication
- **Spring Data MongoDB**: Database integration
- **Lombok**: Reduce boilerplate code
- **Log4j2**: Advanced logging framework
- **Maven**: Dependency management and build tool

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- MongoDB 4.4+

### Installation

1. **Clone the repository**
   ```bash
   git clone <your-repository-url>
   cd cliq24
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your credentials
   ```

3. **Start MongoDB**
   ```bash
   # Windows
   net start MongoDB

   # Linux/Mac
   sudo systemctl start mongod
   ```

4. **Build and run**
   ```bash
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```

The API will be available at `http://localhost:8080`

## Configuration

### Required Environment Variables

| Variable | Description |
|----------|-------------|
| `MONGODB_URI` | MongoDB connection string |
| `JWT_SECRET` | Secret key for JWT tokens |
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth Client Secret |

See `.env.example` for complete list of environment variables.

### OAuth Setup

To use social media integrations, you need to set up OAuth apps:

1. **Google**: https://console.cloud.google.com/
2. **Facebook**: https://developers.facebook.com/
3. **Twitter**: https://developer.twitter.com/
4. **LinkedIn**: https://www.linkedin.com/developers/
5. **TikTok**: https://developers.tiktok.com/
6. **YouTube**: Uses Google OAuth credentials

## API Endpoints

### Authentication

```
GET  /auth/google              - Initiate Google OAuth login
GET  /auth/google/callback     - Google OAuth callback
GET  /auth/me                  - Get current user (requires JWT)
```

### Social Accounts

```
GET    /api/social-accounts                    - Get all connected accounts
POST   /api/social-accounts/{platform}         - Connect new account
DELETE /api/social-accounts/{accountId}        - Disconnect account
POST   /api/social-accounts/{accountId}/sync   - Sync account metrics
```

### Example Request

```bash
# Get current user
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/auth/me

# Get all social accounts
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/api/social-accounts
```

## Project Structure

```
src/
├── main/
│   ├── java/com/cliq24/backend/
│   │   ├── config/              # Configuration classes
│   │   ├── controller/          # REST controllers
│   │   ├── dto/                 # Data Transfer Objects
│   │   ├── exception/           # Exception handlers
│   │   ├── mapper/              # Entity-DTO mappers
│   │   ├── model/               # MongoDB entities
│   │   ├── platforms/           # Social media platform services
│   │   ├── repository/          # MongoDB repositories
│   │   ├── service/             # Business logic services
│   │   └── util/                # Utility classes
│   └── resources/
│       ├── application.properties
│       ├── prod.properties
│       └── log4j2-spring.xml
└── test/                        # Test classes
```

## Data Models

### User
- User account with Google OAuth authentication
- Stores email, name, profile picture

### Social Account
- Represents a connected social media account
- Stores platform, username, encrypted tokens
- Contains engagement metrics and scores

### Account Metrics
- Connections/followers count
- Posts count
- Pending responses
- New messages
- Calculated engagement score (0-100)

## Engagement Scoring

The system calculates engagement scores using a weighted algorithm:

- **30%** - Connections/Followers
- **50%** - Posts Activity
- **20%** - Response Rate

Scores range from 0-100 with labels:
- 80-100: "Crushing It! 🔥"
- 60-79: "Doing Well 👍"
- 40-59: "Needs Attention ⚠️"
- 0-39: "Falling Behind 📉"

## Deployment

### Using Docker

```bash
# Build and run with Docker Compose
docker-compose up -d

# Or build manually
docker build -t cliq24-backend .
docker run -p 8080:8080 cliq24-backend
```

### Cloud Platforms

See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed deployment instructions for:
- Heroku
- AWS Elastic Beanstalk
- Google Cloud Platform
- Azure
- Docker/Kubernetes

## Security

- JWT-based authentication
- OAuth2 integration with social platforms
- Token encryption for stored credentials
- CORS configuration
- Input validation
- SQL injection prevention (using MongoDB)

**Important**: Always change default secrets in production!

## Development

### Running Tests

```bash
./mvnw test
```

### Building for Production

```bash
./mvnw clean package -DskipTests
```

The JAR file will be in `target/cliq24-backend-1.0.0.jar`

### Code Style

This project uses:
- Lombok for reducing boilerplate
- Builder pattern for DTOs
- Service layer for business logic
- Repository pattern for data access

## Logging

The application uses Log4j2 for logging. Configuration is in `src/main/resources/log4j2-spring.xml`.

Log levels:
- `DEBUG`: Detailed information for debugging
- `INFO`: General application flow
- `WARN`: Warning messages
- `ERROR`: Error events

## Troubleshooting

### MongoDB Connection Failed
- Ensure MongoDB is running
- Check connection string in environment variables
- Verify network access and firewall settings

### OAuth Authentication Errors
- Verify OAuth credentials are correct
- Check redirect URIs match exactly
- Ensure all required scopes are configured

### Build Failures
- Check Java version: `java -version`
- Clear Maven cache: `./mvnw clean`
- Update dependencies: `./mvnw dependency:resolve`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## Roadmap

- [ ] Implement actual social media API integrations
- [ ] Add real-time notifications
- [ ] Implement scheduling for posts
- [ ] Add analytics dashboard
- [ ] Support for more platforms
- [ ] Mobile app API support
- [ ] Webhooks for real-time updates

## License

Copyright © 2024 Cliq24. All rights reserved.

## Support

- **Documentation**: See [DEPLOYMENT.md](DEPLOYMENT.md)
- **Issues**: Create an issue on GitHub
- **Email**: support@cliq24.app

## Acknowledgments

- Spring Boot team for the excellent framework
- MongoDB for the flexible database
- All social media platforms for their APIs

---

Built with ❤️ using Spring Boot
