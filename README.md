# Trego Backend API

A comprehensive fitness app backend built with **Spring Boot** and **Firebase**, providing personalized workout plans, nutrition tracking, social features, and AI-powered recommendations.

## 🚀 Features

- **User Authentication** - Firebase Auth integration with JWT tokens
- **User Profiles** - Comprehensive fitness profiles with preferences and metrics
- **Workout Plans** - AI-generated personalized workout routines
- **Nutrition Tracking** - Meal logging with macro tracking and recommendations
- **Social Features** - Friend connections, challenges, and activity feeds
- **Progress Tracking** - Detailed analytics and progress monitoring
- **AI Recommendations** - OpenAI-powered personalized suggestions
- **Real-time Updates** - Live activity tracking and notifications
- **Premium Features** - Subscription-based advanced functionalities

## 🛠 Technology Stack

- **Backend Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: Firebase Firestore
- **Authentication**: Firebase Auth + Spring Security
- **Build Tool**: Maven
- **Containerization**: Docker
- **Monitoring**: Spring Actuator + Prometheus
- **Email**: Spring Mail
- **External APIs**: OpenAI, Stripe

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker (optional)
- Firebase project with service account key

## ⚙️ Firebase Setup

1. **Create Firebase Project**
   ```bash
   # Go to https://console.firebase.google.com/
   # Create a new project or use existing one
   ```

2. **Enable Required Services**
   - Authentication (Email/Password, Google, Apple)
   - Firestore Database
   - Cloud Storage

3. **Generate Service Account Key**
   - Go to Project Settings > Service Accounts
   - Generate new private key
   - Download JSON file
   - Place it in `backend/config/firebase-credentials.json`

4. **Configure Firebase Rules** (Firestore Security Rules)
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /{document=**} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```

## 🔧 Configuration

Create a `.env` file in the backend directory:

```env
# Firebase Configuration
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_CREDENTIALS_PATH=./config/firebase-credentials.json
FIREBASE_STORAGE_BUCKET=your-project-id.appspot.com

# JWT Configuration  
JWT_SECRET=your-super-secret-jwt-key-change-in-production-256-bits-minimum
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# OpenAI Configuration
OPENAI_API_KEY=sk-your-openai-api-key
OPENAI_MODEL=gpt-3.5-turbo
OPENAI_MAX_TOKENS=2000
OPENAI_TEMPERATURE=0.7

# Stripe Configuration (for premium features)
STRIPE_PUBLIC_KEY=pk_test_your-stripe-public-key
STRIPE_SECRET_KEY=sk_test_your-stripe-secret-key
STRIPE_WEBHOOK_SECRET=whsec_your-webhook-secret

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://your-frontend-domain.com

# Rate Limiting
RATE_LIMIT_RPM=60
RATE_LIMIT_BURST=10

# Logging
LOG_LEVEL=INFO
```

## 🏃‍♂️ Running the Application

### Local Development

```bash
# Clone and navigate to backend
cd backend

# Run with Maven wrapper (recommended)
./mvnw spring-boot:run

# Run with development profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=development

# Run with custom port
SERVER_PORT=8081 ./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

### Using Docker

```bash
# Build and run with Docker Compose (includes monitoring stack)
docker-compose up --build

# Or build and run single container
docker build -t trego-backend .
docker run -p 8080:8080 --env-file .env trego-backend
```

## 📁 Project Structure

```
backend/
├── src/main/java/com/trego/
│   ├── TregoApplication.java              # Main application class
│   ├── config/                            # Configuration classes
│   │   ├── FirebaseConfig.java            # Firebase setup
│   │   └── SecurityConfig.java            # Security configuration
│   ├── controller/                        # REST controllers
│   │   └── AuthController.java            # Authentication endpoints
│   ├── dto/                              # Data Transfer Objects
│   │   ├── AuthRequest.java
│   │   ├── AuthResponse.java
│   │   └── RegisterRequest.java
│   ├── model/                            # Entity models
│   │   ├── BaseEntity.java               # Base entity with timestamps
│   │   ├── User.java                     # User model
│   │   └── UserProfile.java              # User profile model
│   ├── repository/                       # Data access layer
│   │   ├── FirestoreRepository.java      # Base Firestore operations
│   │   ├── UserRepository.java           # User data access
│   │   └── UserProfileRepository.java    # Profile data access
│   ├── security/                         # Security components
│   │   ├── FirebaseAuthenticationFilter.java
│   │   ├── FirebaseUserPrincipal.java
│   │   └── JwtAuthenticationEntryPoint.java
│   └── service/                          # Business logic
│       ├── AuthService.java              # Authentication service
│       └── EmailService.java             # Email service
├── src/main/resources/
│   └── application.yml                   # Application configuration
├── src/test/java/                        # Test classes
├── config/
│   └── firebase-credentials.json         # Firebase service account key
├── logs/                                 # Application logs
├── Dockerfile                           # Docker configuration
├── docker-compose.yml                  # Docker Compose with monitoring
├── pom.xml                             # Maven dependencies
└── README.md                           # This file
```

## 🌐 API Endpoints

### Authentication

```http
POST   /api/auth/register                # Register new user
POST   /api/auth/login                   # User login
POST   /api/auth/firebase-sync           # Sync Firebase user
POST   /api/auth/refresh                 # Refresh JWT token
GET    /api/auth/me                      # Get current user
POST   /api/auth/verify-email            # Verify email address
POST   /api/auth/forgot-password         # Request password reset
POST   /api/auth/reset-password          # Reset password
POST   /api/auth/logout                  # Logout user
PUT    /api/auth/profile                 # Update profile
DELETE /api/auth/account                 # Delete account
```

### Health & Monitoring

```http
GET    /actuator/health                  # Application health
GET    /actuator/info                    # Application info
GET    /actuator/metrics                 # Application metrics
GET    /actuator/prometheus              # Prometheus metrics
```

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run with coverage report
./mvnw test jacoco:report

# Run integration tests
./mvnw verify

# Run specific test class
./mvnw test -Dtest=AuthControllerTest
```

## 📦 Building for Production

```bash
# Build JAR file
./mvnw clean package -DskipTests

# Build with production profile
./mvnw clean package -Pproduction

# The JAR file will be created in target/trego-backend-1.0.0.jar
```

## 🐳 Docker Deployment

The Docker setup includes:
- **Application**: Spring Boot backend
- **Redis**: Caching and rate limiting
- **Nginx**: Reverse proxy and load balancing
- **Prometheus**: Metrics collection
- **Grafana**: Monitoring dashboards
- **Jaeger**: Distributed tracing (optional)

```bash
# Production deployment
docker-compose up -d

# View logs
docker-compose logs -f api

# Scale the application
docker-compose up -d --scale api=3
```

## 📊 Monitoring & Observability

### Health Checks
- Application health: `http://localhost:8080/actuator/health`
- Detailed health: `http://localhost:8080/actuator/health/details`

### Metrics
- Prometheus metrics: `http://localhost:8080/actuator/prometheus`
- Custom metrics: `http://localhost:8080/actuator/metrics`

### Logs
- Application logs: `logs/trego-backend.log`
- Access logs: Available through nginx container

### Monitoring Stack
- **Grafana**: `http://localhost:3000` (admin/admin)
- **Prometheus**: `http://localhost:9090`
- **Jaeger**: `http://localhost:16686`

## 🔒 Security Features

- **Firebase Authentication** - Secure user authentication
- **JWT Tokens** - Stateless authentication with refresh tokens
- **Spring Security** - Comprehensive security framework
- **CORS Protection** - Configurable cross-origin requests
- **Rate Limiting** - Request rate limiting (configurable)
- **Input Validation** - Bean validation on all inputs
- **Password Encryption** - BCrypt password hashing
- **Secure Headers** - Security headers via Spring Security

## 🔧 Development Tips

1. **Hot Reloading**: Add Spring Boot DevTools dependency for automatic restart
2. **Debugging**: Use `--debug` flag for verbose logging
3. **Profiles**: Use Spring profiles for environment-specific configuration
4. **IDE Support**: Import as Maven project in IntelliJ IDEA or Eclipse
5. **Database**: Use Firebase Console to view Firestore data
6. **Testing**: Use `@SpringBootTest` for integration tests

## 🚨 Troubleshooting

### Common Issues

1. **Firebase Connection Failed**
   - Verify `firebase-credentials.json` exists and is valid
   - Check `FIREBASE_PROJECT_ID` environment variable
   - Ensure Firebase project has required services enabled

2. **JWT Token Issues**
   - Verify `JWT_SECRET` is set and at least 256 bits
   - Check token expiration settings
   - Ensure system time is synchronized

3. **CORS Errors**
   - Update `CORS_ALLOWED_ORIGINS` environment variable
   - Check frontend URL is included in allowed origins

4. **Port Conflicts**
   - Change port using `SERVER_PORT` environment variable
   - Check if port 8080 is already in use

5. **Email Service Issues**
   - Verify Gmail app password (not regular password)
   - Check `MAIL_USERNAME` and `MAIL_PASSWORD` settings

### Debug Commands

```bash
# Check application logs
tail -f logs/trego-backend.log

# Test health endpoint
curl http://localhost:8080/actuator/health

# Test authentication
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# Check Firebase connection
curl http://localhost:8080/actuator/health/firebase
```

## 🔄 Migration from Node.js

This backend was converted from Node.js to Spring Boot Java. Key improvements:

- **Type Safety**: Strong typing eliminates runtime errors
- **Enterprise Features**: Spring Boot's robust ecosystem
- **Better Performance**: JVM optimization and memory management  
- **Enhanced Security**: Spring Security integration
- **Built-in Monitoring**: Comprehensive observability features
- **Scalability**: Better support for high-load scenarios

## 📈 Performance & Scaling

### JVM Tuning
```bash
# Optimize for containers
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Garbage collection optimization
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:G1HeapRegionSize=16m"
```

### Database Optimization
- Use Firestore composite indexes for complex queries
- Implement proper pagination for large datasets
- Cache frequently accessed data in Redis

### Caching Strategy
```yaml
# Redis configuration in application.yml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600s
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/new-feature`
3. Follow Java coding conventions and Spring Boot best practices
4. Add tests for new functionality
5. Update documentation as needed
6. Submit a pull request

### Code Style
- Follow Google Java Style Guide
- Use meaningful variable and method names
- Add JavaDoc comments for public methods
- Keep methods focused and single-purpose

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support:
1. Check this README and troubleshooting section
2. Review application logs in `logs/` directory
3. Check Firebase Console for authentication/database issues
4. Create an issue in the project repository

## 🗺 Roadmap

- [ ] Additional authentication providers (OAuth, Apple)
- [ ] Advanced AI recommendations
- [ ] Real-time notifications with WebSocket
- [ ] Advanced analytics and reporting
- [ ] Mobile push notifications
- [ ] Third-party fitness device integration
- [ ] Multi-language support
- [ ] Advanced social features