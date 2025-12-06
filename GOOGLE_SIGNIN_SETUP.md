# Google Sign-In Setup Guide

## What Was Fixed

1. ✅ **Firebase Authentication Integration**: Replaced simulated authentication with real Firebase Auth
2. ✅ **Email/Password Sign-In**: Now uses Firebase Authentication
3. ✅ **Google Sign-In Implementation**: Added Google Sign-In functionality
4. ✅ **Auth State Persistence**: MainActivity now checks if user is already logged in
5. ✅ **Web Client ID**: Added to `strings.xml` from your `google-services.json`

## Current Status

- ✅ Email/Password authentication is fully functional
- ⚠️ Google Sign-In requires one more step (see below)

## Required Step for Google Sign-In

For Google Sign-In to work, you need to add your app's **SHA-1 fingerprint** to Firebase Console:

### Step 1: Get Your SHA-1 Fingerprint

**For Debug Build:**
```bash
cd android
./gradlew signingReport
```

Or on Windows:
```bash
cd android
gradlew signingReport
```

Look for the SHA-1 value under `Variant: debug` → `Config: debug`

**Alternative method (using keytool):**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### Step 2: Add SHA-1 to Firebase

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (`lguinah2`)
3. Go to **Project Settings** (gear icon)
4. Scroll down to **Your apps** section
5. Click on your Android app (`com.example.mynewapplication`)
6. Scroll to **SHA certificate fingerprints**
7. Click **Add fingerprint**
8. Paste your SHA-1 fingerprint
9. Click **Save**

### Step 3: Download Updated google-services.json (Optional)

After adding SHA-1, you may want to download the updated `google-services.json` file, though it's not strictly necessary if you've already added the web client ID to `strings.xml`.

## Testing

### Test Email/Password Sign-In
1. Run the app
2. Enter an email ending with `@estin.dz`
3. Enter a password (create account first if needed)
4. Click "Sign In"

### Test Google Sign-In
1. After adding SHA-1 fingerprint, wait a few minutes for Firebase to update
2. Run the app
3. Click "Continue with Google"
4. Select your Google account
5. Should sign in successfully

## Troubleshooting

### Google Sign-In Not Working

**Error: "10:" or "DEVELOPER_ERROR"**
- Make sure you've added the SHA-1 fingerprint to Firebase
- Wait a few minutes after adding SHA-1 for Firebase to update
- Make sure Google Sign-In is enabled in Firebase Console → Authentication → Sign-in method

**Error: "Web Client ID not found"**
- Check that `default_web_client_id` is in `strings.xml`
- The value should be: `412765576160-76dc413lk0r1dnk5qu1r4natk554tcpt.apps.googleusercontent.com`

**App Crashes on Google Sign-In**
- Make sure Google Sign-In is enabled in Firebase Console
- Check that `google-services.json` is in the `app/` directory
- Sync Gradle files

### Email Sign-In Issues

**"No account found"**
- User needs to sign up first
- Go to Firebase Console → Authentication to create a test user

**"Invalid password"**
- Check password in Firebase Console
- User can reset password if needed

## Additional Notes

- The app now checks Firebase auth state on startup
- If a user is already logged in, they'll go directly to the main app
- Users are automatically logged out only when they explicitly sign out
- All authentication is now handled by Firebase (no more simulated delays)

## Security Reminders

- For production, consider:
  - Adding SHA-256 fingerprint as well
  - Setting up proper Firestore security rules
  - Implementing password reset functionality
  - Adding email verification

