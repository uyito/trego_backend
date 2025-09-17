# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.2.0 fitness backend API for the Trego app, using Java 17 and Firebase as the primary database. The architecture follows standard Spring Boot conventions with clean separation between controllers, services, repositories, and configuration.

## Development Commands

### Build & Run
```bash
# Run the application locally
./mvnw spring-boot:run

# Run with development profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=development

# Build production JAR
./mvnw clean package -DskipTests

# Build with production profile
./mvnw clean package -Pproduction
```

### Testing
```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report

# Run integration tests
./mvnw verify

# Run specific test class
./mvnw test -Dtest=AuthControllerTest
```

### Docker Operations
```bash
# Run full stack with monitoring
docker-compose up --build

# Run only the API
docker build -t trego-backend .
docker run -p 8080:8080 --env-file .env trego-backend

# View logs
docker-compose logs -f api
```

## Architecture

### Core Components
- **Firebase Integration**: Firestore as primary database, Firebase Auth for user authentication
- **Security Layer**: Spring Security with Firebase token validation + JWT for stateless sessions
- **External APIs**: OpenAI for AI features, Stripe for payments, Spring Mail for notifications
- **Monitoring**: Spring Actuator + Prometheus metrics, Grafana dashboards
- **Infrastructure**: Nginx reverse proxy, Redis for caching/rate limiting

### Package Structure
```
com.trego/
├── TregoApplication.java      # Main Spring Boot application
├── config/                   # Configuration classes (Firebase, Security, etc.)
├── controller/               # REST API controllers
├── dto/                     # Data transfer objects
├── model/                   # Entity models (User, UserProfile, etc.)
├── repository/              # Data access layer (FirestoreRepository base class)
├── security/                # Security filters and JWT handling
└── service/                 # Business logic services
```

## Configuration

### Required Environment Variables
- `FIREBASE_PROJECT_ID`: Firebase project identifier
- `FIREBASE_CREDENTIALS_PATH`: Path to Firebase service account JSON
- `JWT_SECRET`: JWT signing secret (256-bit minimum)
- `OPENAI_API_KEY`: OpenAI API key for AI features
- `STRIPE_SECRET_KEY`: Stripe secret key for payments

### Key Configuration Files
- `src/main/resources/application.yml`: Main Spring configuration
- `config/firebase-credentials.json`: Firebase service account key
- `docker-compose.yml`: Full production stack with monitoring

## Database Schema

Uses Firebase Firestore with the following collections:
- Users: Authentication and basic user data
- UserProfiles: Detailed fitness profiles and preferences
- WorkoutPlans: AI-generated personalized routines
- NutritionData: Meal tracking and nutritional information
- SocialConnections: Friend relationships and challenges

## API Structure

All endpoints are prefixed with `/api` (configured via `server.servlet.context-path`).

Main endpoint groups:
- `/api/auth/*`: Authentication and user management
- `/actuator/*`: Health checks and metrics (health, info, metrics, prometheus)

## Development Notes

- Application runs on port 8080 by default
- Uses Maven wrapper (`./mvnw`) for consistent builds across environments
- Firebase credentials must be placed in `config/firebase-credentials.json`
- Logs are written to `logs/trego-backend.log`
- The project structure suggests it was migrated from Node.js to Java for better type safety and enterprise features