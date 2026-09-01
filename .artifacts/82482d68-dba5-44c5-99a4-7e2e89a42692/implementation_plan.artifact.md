# Secure Firestore with Anonymous Authentication

This plan addresses security vulnerabilities in the `/trials`, `/directSignUps`, and `/appCategories` collections by implementing Firebase Anonymous Authentication. This ensures that every request to Firestore is authenticated, preventing public data harvesting and DOS attacks, while maintaining compatibility for existing trial users.

## User Review Required

> [!IMPORTANT]
> This strategy relies on Firebase Anonymous Auth. If a user clears their app data, their anonymous `uid` will change. We will use a migration strategy that links the existing `deviceId` based documents to the new `uid` on the first encounter.

> [!WARNING]
> We will need to deploy new Firestore Security Rules. I will provide the rules text, which must be updated in the Firebase Console (or `firestore.rules` file if managed via CLI).

## Proposed Changes

### Core Logic & Auth

#### [MODIFY] [SessionRepository.kt](file:///C:/Users/MikeW/AndroidStudioProjects/Speakify/app/src/main/java/com/mikewarren/speakify/data/SessionRepository.kt)
- Add `ensureFirebaseAuthenticated()` method to handle anonymous sign-in when no Clerk user is present.
- Call `ensureFirebaseAuthenticated()` in `reactToSessionState` for guest users.

#### [MODIFY] [TrialModel.kt](file:///C:/Users/MikeW/AndroidStudioProjects/Speakify/app/src/main/java/com/mikewarren/speakify/data/models/TrialModel.kt)
- Add `uid: String? = null` field to track the owner of the trial record in Firestore.

### Repositories

#### [MODIFY] [TrialRepositoryImpl.kt](file:///C:/Users/MikeW/AndroidStudioProjects/Speakify/app/src/main/java/com/mikewarren/speakify/data/TrialRepositoryImpl.kt)
- Update `refreshTrialStatus` to "claim" existing documents by adding the current `uid` if it's missing (migration path).
- Update `recordTrialModel` and `recordDirectSignUp` to include the current Firebase `uid`.

### Security Rules

#### [NEW] [firestore.rules](file:///C:/Users/MikeW/AndroidStudioProjects/Speakify/firestore.rules)
(Note: If this file isn't in your project, you should apply these to the Firebase Console)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Existing users collection (already locked down)
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    // Secure trials and directSignUps
    match /trials/{deviceId} {
      allow create: if request.auth != null;
      allow get, update: if request.auth != null &&
        (resource == null || resource.data.uid == null || resource.data.uid == request.auth.uid);
      allow delete: if request.auth != null && resource.data.uid == request.auth.uid;
    }

    match /directSignUps/{deviceId} {
      allow create: if request.auth != null;
      allow get, update: if request.auth != null &&
        (resource == null || resource.data.uid == null || resource.data.uid == request.auth.uid);
    }

    // App categories: Anyone can read, only authenticated users can contribute mappings
    match /appCategories/{packageName} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

## Verification Plan

### Automated Tests
- Unit tests in `TrialRepositoryTest` to verify that `uid` is correctly added to models during refresh and record.
- Verify that `SessionRepository` triggers anonymous sign-in when initialized without a user.

### Manual Verification
1. **Fresh Install**: Verify that an anonymous Firebase user is created on first run.
2. **Trial Start**: Verify the `trials` document in Firestore contains the `uid`.
3. **Migration**:
   - Manually create a `trials/{deviceId}` document *without* a `uid` in the console.
   - Run the app on a device with that `deviceId`.
   - Verify the app fetches the trial and updates the document to include the current anonymous `uid`.
4. **Sign In**: Verify that when signing in with Clerk, the Firebase session correctly transitions to the Clerk-linked account.
