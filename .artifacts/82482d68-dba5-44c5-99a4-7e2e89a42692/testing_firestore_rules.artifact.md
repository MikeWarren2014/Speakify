# Testing Firestore Security Rules Locally

To test the new security rules without deploying them to production, you should use the **Firebase Emulator Suite**. This allows you to run a local version of Firestore and Auth to verify your rules against actual requests.

## Prerequisites

1.  **Node.js**: Required to run the Firebase CLI.
2.  **Firebase CLI**: Install it globally:
    ```bash
    npm install -g firebase-tools
    ```

## Step 1: Initialize Firebase Local Settings

If you haven't already, initialize the project in your root directory:
1.  Run `firebase init emulators`.
2.  Select **Firestore** and **Authentication**.
3.  When asked for the rules file, point it to the one we just created: `firestore.rules`.

This will create a `firebase.json` file in your project root. Ensure it looks something like this:

```json
{
  "firestore": {
    "rules": "firestore.rules"
  },
  "emulators": {
    "auth": { "port": 9099 },
    "firestore": { "port": 8080 },
    "ui": { "enabled": true }
  }
}
```

## Step 2: Start the Emulators

Run the following command in your terminal:
```bash
firebase emulators:start
```
You can now access the **Emulator UI** at `http://localhost:4000`. This provides a console where you can manually add data and see if your rules block/allow actions.

## Step 3: Connect the Android App to the Emulator

To test the rules using your app's actual logic, configure your `FirebaseFirestore` and `FirebaseAuth` instances to point to the local emulator in your code (usually in a `debug` or `testing` flavor).

### In `BaseFirestoreRepository.kt` or a Dependency Injection module:

```kotlin
if (BuildConfig.DEBUG) {
    // Point to your machine's IP address (10.0.2.2 for Android Emulator)
    val host = "10.0.2.2"

    firestore.useEmulator(host, 8080)
    firebaseAuth.useEmulator(host, 9099)

    // Disable SSL for local testing
    firestore.firestoreSettings = firestoreSettings {
        isPersistenceEnabled = false
    }
}
```

## Step 4: Automated Rules Testing (Recommended)

The most robust way to test rules is via a dedicated test suite using the `@firebase/rules-unit-testing` library. This allows you to write scripts that assert "User A can read Doc B" or "Guest cannot delete Doc C".

Example test snippet (JavaScript):

```javascript
const testing = require('@firebase/rules-unit-testing');

// Test that a guest cannot delete a trial doc
it('should deny guest from deleting a trial doc', async () => {
  const db = testing.initializeTestEnvironment({ projectId: "my-project" }).unauthenticatedContext().firestore();
  const doc = db.collection('trials').doc('device1');
  await testing.assertFails(doc.delete());
});
```

> [!TIP]
> Using the **Emulator UI** is the fastest way to manually verify that the `uid` field we added is correctly being used to permit or deny access.
