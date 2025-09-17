# Firebase Configuration

This directory contains Firebase configuration files for the Trego backend.

## Setup Instructions

1. **Generate Firebase Service Account Key**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Select your project
   - Go to Project Settings → Service Accounts
   - Click "Generate new private key"
   - Download the JSON file

2. **Place the Firebase Credentials**
   - Save the downloaded JSON file as `firebase-credentials.json` in this directory
   - The file should look like the sample provided in `firebase-credentials-sample.json`

3. **Configure Environment Variables**
   - Copy `.env.example` to `.env` in the backend root directory
   - Update the `FIREBASE_PROJECT_ID` with your actual project ID
   - Ensure `FIREBASE_CREDENTIALS_PATH` points to `./config/firebase-credentials.json`

## Security Notes

- **Never commit** `firebase-credentials.json` to version control
- The actual credentials file is excluded in `.gitignore`
- Use environment variables for sensitive configuration
- In production, consider using cloud-native credential management

## Firebase Services Required

Make sure these services are enabled in your Firebase project:

- **Authentication** - For user sign-in/sign-up
- **Firestore Database** - For data storage
- **Cloud Storage** - For file uploads (optional)

## Firestore Security Rules

Update your Firestore security rules to allow authenticated access:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow authenticated users to read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    match /userProfiles/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Add more specific rules as needed
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## Testing Firebase Connection

Once configured, test your Firebase connection:

```bash
# Run the application
./mvnw spring-boot:run

# Check health endpoint (should show Firebase status)
curl http://localhost:8080/actuator/health
```