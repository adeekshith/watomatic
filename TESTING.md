# Testing Guide for Google Play Billing Implementation

## Prerequisites
1.  **Google Play Console**: Ensure you have created the subscription products with IDs:
    *   `automatic-ai-pro-monthly`
    *   `automatic-ai-pro-annual`
2.  **Firebase**: Ensure Firebase Functions are deployed (`firebase deploy --only functions`).
3.  **Test Account**: Add your email as a "License Tester" in Google Play Console settings.

## 1. Testing Purchase Flow (Sandbox)
1.  **Build**: Install the `GooglePlayDebug` build variant on a physical device (Emulators often have issues with Play Billing).
2.  **Navigate**: Go to `SubscriptionInfoActivity` (via "Automatic AI" settings).
3.  **UI Check**: Verify prices are loaded (e.g., $1.99/month).
4.  **Purchase**: Click "Subscribe". The Google Play bottom sheet should appear.
    *   **Success**: Use a test card "Always approves". Verify the app shows "Active: Monthly Plan".
    *   **Decline**: Use a test card "Always declines". Verify the app shows an error.

## 2. Testing Backend Verification
1.  **Logs**: Monitor Logcat (`TAG: BillingManager` and `FirebaseBackend`).
2.  **Verify**: After a successful purchase, check Logcat for "Backend verification successful".
3.  **Firestore**: Go to Firebase Console -> Firestore.
    *   Check `users/{userId}/subscriptions/active` document.
    *   Verify `isValid` is true and `expiryTime` is updated.

## 3. Testing Restoration
1.  **Uninstall & Reinstall**: Uninstall the app. Reinstall it.
2.  **Login**: Log in with the same account.
3.  **Check**: Go to Subscription page.
4.  **Action**: Click "Restore Purchases".
5.  **Result**: Toast should say "Successfully restored subscription!" and status should update to Active.

## 4. Testing Subscription Expiry (Simulated)
1.  **Play Console**: In License Testing settings, set "Subscription duration" to "5 minutes" (effectively 5 minutes for monthly in test).
2.  **Wait**: Wait for the subscription to expire.
3.  **Launch**: Relaunch the app.
4.  **Verify**: `SubscriptionCheckWorker` (or manual refresh) should eventually update status to "Expired" (inactive).

## 5. Testing Device Limits (Optional/Future)
1.  **Multi-Device**: Log in on a 4th device.
2.  **Verify**: Ensure the backend handles this gracefully (currently logs warnings or rejects based on rules).

## 6. AWS Migration Readiness
To migrate to AWS in the future:
1.  Create `AwsBackendService` implementing `BackendService`.
2.  Update `SubscriptionManagerImpl` and `BillingManagerImpl` to use `AwsBackendService` instead of `FirebaseBackendService`.
