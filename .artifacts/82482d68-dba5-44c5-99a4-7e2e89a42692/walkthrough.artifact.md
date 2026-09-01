# Walkthrough - Secure Firestore with Anonymous Auth

I have implemented Firebase Anonymous Authentication to secure the trial and app category collections. This prevents public data harvesting and unauthorized deletions while maintaining a seamless experience for guest users.

## Changes Made

### Authentication & Core Logic
- **SessionRepository**: Added `ensureFirebaseAuthenticated()` which signs the user into Firebase anonymously if they are not already logged in (e.g., via Clerk). This ensures every request to Firestore has a valid `request.auth` object.
- **TrialModel**: Added a `uid` field to track which Firebase user owns a specific trial record.

### Repository Updates
- **TrialRepositoryImpl**:
    - **Migration**: Updated `refreshTrialStatus` to automatically add the current `uid` to existing Firestore documents that lack one. This "claims" the document for the current device/user session.
    - **Persistence**: Updated `recordTrialModel` and `recordDirectSignUp` to include the Firebase `uid` in all new Firestore writes.

### Security Infrastructure
- **firestore.rules**: Created a new rules file with the following security logic:
    - `/trials` and `/directSignUps`: `create` requires auth. `get`, `update`, and `delete` require the `uid` to match the authenticated user (with a fallback for legacy docs without a `uid` during the migration phase).
    - `/appCategories`: `read` is public, but `write` requires authentication to prevent spam.

## Verification Results

### Automated Tests
- Added `startTrial adds firebase uid to model` to `TrialRepositoryTest`.
- Added `refreshTrialStatus claims existing trial doc with uid` to `TrialRepositoryTest` to verify the migration logic.
- Verified that syntax and structure are sound via static analysis.

### Manual Verification Steps Recommended
1.  **Deploy Rules**: Apply the contents of [firestore.rules](file:///C:/Users/MikeW/AndroidStudioProjects/Speakify/firestore.rules) to your Firebase Console.
2.  **Verify Guest Access**: Open the app as a guest and check the Firebase console to confirm an anonymous user was created.
3.  **Verify Trial Protection**: Confirm that trial documents in Firestore now contain a `uid` field.
