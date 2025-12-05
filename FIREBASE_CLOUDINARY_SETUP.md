# Firebase and Cloudinary Setup Guide

This guide will help you complete the setup of Firebase and Cloudinary for your Kotlin Android application.

## Firebase Setup

### Step 1: Create a Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" or select an existing project
3. Follow the setup wizard

### Step 2: Add Android App to Firebase
1. In Firebase Console, click "Add app" and select Android
2. Enter your package name: `com.example.mynewapplication`
3. Register the app
4. Download the `google-services.json` file

### Step 3: Add google-services.json
1. Place the downloaded `google-services.json` file in the `app/` directory (same level as `build.gradle`)
2. The file structure should look like:
   ```
   app/
     ├── build.gradle
     ├── google-services.json  ← Place it here
     └── src/
   ```

### Step 4: Enable Firebase Services
In Firebase Console, enable the following services:

#### Authentication
1. Go to Authentication → Sign-in method
2. Enable "Email/Password" provider
3. Optionally enable other providers as needed

#### Firestore Database
1. Go to Firestore Database
2. Click "Create database"
3. Start in **test mode** for development (you can change rules later)
4. Choose a location for your database

#### Storage (Optional - if you want to use Firebase Storage instead of Cloudinary)
1. Go to Storage
2. Click "Get started"
3. Start in test mode for development
4. Choose a location

### Step 5: Firestore Security Rules (Important!)
Update your Firestore rules in Firebase Console → Firestore Database → Rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // LostItems collection
    match /lostItems/{itemId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && 
        (resource.data.userId == request.auth.uid || 
         request.auth.token.admin == true);
    }
  }
}
```

## Cloudinary Setup

### Step 1: Create a Cloudinary Account
1. Go to [Cloudinary](https://cloudinary.com/)
2. Sign up for a free account
3. After signing up, you'll be taken to your dashboard

### Step 2: Get Your Credentials
1. In Cloudinary Dashboard, you'll see your:
   - **Cloud Name**
   - **API Key**
   - **API Secret**

### Step 3: Update CloudinaryService.kt
Open `app/src/main/java/com/example/mynewapplication/data/remote/CloudinaryService.kt`

Replace these constants with your actual credentials:
```kotlin
private const val CLOUD_NAME = "YOUR_CLOUD_NAME"
private const val API_KEY = "YOUR_API_KEY"
private const val API_SECRET = "YOUR_API_SECRET"
```

**⚠️ Security Note:** For production, consider storing these credentials securely using:
- Android Keystore
- Environment variables
- A secure configuration file (not committed to git)

### Step 4: Cloudinary Upload Presets (Optional)
You can create upload presets in Cloudinary Dashboard → Settings → Upload:
- This allows unsigned uploads
- Useful for client-side uploads

## Testing the Setup

### Test Firebase Connection
1. Build and run your app
2. Try to sign up/login
3. Check Firebase Console → Authentication to see if users are created

### Test Cloudinary Connection
1. Try uploading an image in the app
2. Check Cloudinary Dashboard → Media Library to see uploaded images

## Troubleshooting

### Firebase Issues
- **"google-services.json not found"**: Make sure the file is in the `app/` directory
- **Build errors**: Sync Gradle files (File → Sync Project with Gradle Files)
- **Authentication errors**: Check that Email/Password is enabled in Firebase Console

### Cloudinary Issues
- **Upload fails**: Verify your credentials are correct
- **Network errors**: Check internet permissions in AndroidManifest.xml (already added)
- **Initialization errors**: Make sure CloudinaryService.initialize() is called in MainActivity

## Next Steps

After setup is complete:
1. Update your ViewModels to use `FirebaseService` and `CloudinaryService`
2. Test all CRUD operations
3. Set up proper error handling
4. Configure production security rules

## Additional Resources

- [Firebase Android Documentation](https://firebase.google.com/docs/android/setup)
- [Cloudinary Android SDK](https://cloudinary.com/documentation/android_integration)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)

