const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');
const { google } = require('googleapis');

admin.initializeApp();

// Initialize Google Play Developer API
const fs = require('fs');
const keyFilePath = './service-account-key.json';
const authConfig = {
    scopes: ['https://www.googleapis.com/auth/androidpublisher'],
};

if (fs.existsSync(keyFilePath)) {
    authConfig.keyFile = keyFilePath;
}

const androidPublisher = google.androidpublisher({
    version: 'v3',
    auth: new google.auth.GoogleAuth(authConfig),
});

const PACKAGE_NAME = 'com.parishod.atomatic';
const DEVICE_LIMIT = 3;

/**
 * Verify a purchase with Google Play Developer API
 * 
 * IMPORTANT: firebase-functions V2 API - the signature is different from V1!
 * V2: onCall((request) => { ... }) where request.auth contains auth info
 * V1: onCall((data, context) => { ... }) - THIS IS DEPRECATED
 */
exports.verifyPurchase = onCall({ region: 'us-central1' }, async (request) => {
    // V2 API: auth is on request.auth, data is on request.data
    const auth = request.auth;
    const data = request.data;

    // Enhanced auth logging for debugging
    console.log('verifyPurchase called (V2 API)');
    console.log('Auth present:', !!auth);
    console.log('Auth UID:', auth?.uid || 'null');

    if (auth) {
        console.log('Auth token claims:', JSON.stringify(auth.token || {}, null, 2));
    } else {
        console.error('NO AUTH - Client did not send a valid ID token');
        console.error('Possible causes:');
        console.error('1. Client-side FirebaseAuth.currentUser is null');
        console.error('2. ID token expired and was not refreshed');
        console.error('3. Client region mismatch with function region (us-central1)');
        console.error('4. google-services.json / SHA-1 mismatch');
    }

    // Ensure user is authenticated
    if (!auth) {
        throw new HttpsError('unauthenticated', 'User must be authenticated. Please ensure you are signed in and your session has not expired.');
    }

    const { purchaseToken, productId, orderId, packageName } = data;
    const userId = auth.uid;

    // Detailed logging to debug NOT_FOUND errors
    const resolvedPackageName = packageName || PACKAGE_NAME;
    console.log('=== Purchase Verification Request ===');
    console.log(`User ID: ${userId}`);
    console.log(`Package Name: ${resolvedPackageName}`);
    console.log(`Product ID (subscriptionId): ${productId}`);
    console.log(`Order ID: ${orderId}`);
    console.log(`Purchase Token (first 50 chars): ${purchaseToken?.substring(0, 50)}...`);
    console.log(`Purchase Token length: ${purchaseToken?.length || 0}`);

    try {
        // IMPORTANT: Use subscriptionsv2 API instead of deprecated subscriptions API
        // The old purchases.subscriptions.get API doesn't work for subscriptions 
        // created after May 2022 that use base plans
        console.log('Calling Google Play Developer API (subscriptionsv2)...');
        const response = await androidPublisher.purchases.subscriptionsv2.get({
            packageName: resolvedPackageName,
            token: purchaseToken,
        });

        const subscription = response.data;
        console.log('Google Play API V2 response:', JSON.stringify(subscription, null, 2));

        // V2 API response structure is different from V1
        // Check subscription state: https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptionsv2#SubscriptionState
        // SUBSCRIPTION_STATE_ACTIVE = subscription is active
        // SUBSCRIPTION_STATE_CANCELED = canceled but still valid until expiry
        // SUBSCRIPTION_STATE_IN_GRACE_PERIOD = payment failed, in grace period
        // SUBSCRIPTION_STATE_ON_HOLD = payment failed, on hold
        // SUBSCRIPTION_STATE_PAUSED = paused by user
        // SUBSCRIPTION_STATE_EXPIRED = expired
        const subscriptionState = subscription.subscriptionState;
        console.log(`Subscription state: ${subscriptionState}`);

        // Get the line items (there should be one for simple subscriptions)
        const lineItems = subscription.lineItems || [];
        const lineItem = lineItems[0];

        if (!lineItem) {
            console.log('No line items found in subscription');
            return {
                isValid: false,
                expiryTime: 0,
                autoRenewing: false,
                planType: '',
                error: 'No subscription line items found'
            };
        }

        // Extract data from V2 response
        const expiryTime = lineItem.expiryTime; // ISO 8601 format
        const expiryTimeMillis = expiryTime ? new Date(expiryTime).getTime() : 0;
        const retrievedProductId = lineItem.productId;
        const basePlanId = lineItem.offerDetails?.basePlanId;
        const autoRenewing = subscription.subscriptionState === 'SUBSCRIPTION_STATE_ACTIVE' &&
            lineItem.autoRenewingPlan?.autoRenewEnabled === true;

        console.log(`Product ID: ${retrievedProductId}, Base Plan: ${basePlanId}`);
        console.log(`Expiry: ${expiryTime} (${expiryTimeMillis}ms)`);
        console.log(`Auto-renewing: ${autoRenewing}`);

        // Check if subscription is valid (active, canceled but not expired, or in grace period)
        const validStates = [
            'SUBSCRIPTION_STATE_ACTIVE',
            'SUBSCRIPTION_STATE_CANCELED', // still valid until expiry
            'SUBSCRIPTION_STATE_IN_GRACE_PERIOD'
        ];

        const isActive = validStates.includes(subscriptionState) && expiryTimeMillis > Date.now();

        if (!isActive) {
            console.log(`Subscription not active. State: ${subscriptionState}, Expiry: ${expiryTimeMillis}, Now: ${Date.now()}`);
            return {
                isValid: false,
                expiryTime: expiryTimeMillis,
                autoRenewing: false,
                planType: '',
                error: `Subscription state: ${subscriptionState}`
            };
        }

        // Determine plan type from product ID or base plan
        const planType = (retrievedProductId || productId || '').includes('monthly') ? 'monthly' : 'annual';

        console.log('Google Play API verification successful! Now storing in Firestore...');

        // Store verified subscription in Firestore (with separate error handling)
        try {
            await admin.firestore().collection('users').doc(userId).set({
                subscription: {
                    isActive: true,
                    productId: retrievedProductId || productId,
                    basePlanId: basePlanId,
                    planType: planType,
                    purchaseToken: purchaseToken,
                    orderId: orderId,
                    expiryTime: expiryTimeMillis,
                    autoRenewing: autoRenewing,
                    subscriptionState: subscriptionState,
                    lastVerified: admin.firestore.FieldValue.serverTimestamp(),
                    verified: true,
                    verifiedBy: 'backend'
                }
            }, { merge: true });
            console.log('Successfully wrote to users collection');
        } catch (firestoreError) {
            console.error('Firestore write to users failed:', firestoreError.message);
            console.error('Firestore error code:', firestoreError.code);
            // Continue anyway - the purchase is valid, just Firestore storage failed
        }

        // Also store in subscription history (with separate error handling)
        try {
            const startTime = subscription.startTime ? new Date(subscription.startTime).getTime() : Date.now();
            await admin.firestore()
                .collection('users')
                .doc(userId)
                .collection('subscriptions')
                .doc(purchaseToken)
                .set({
                    productId: retrievedProductId || productId,
                    basePlanId: basePlanId,
                    planType: planType,
                    purchaseToken: purchaseToken,
                    orderId: orderId,
                    expiryTime: expiryTimeMillis,
                    autoRenewing: autoRenewing,
                    subscriptionState: subscriptionState,
                    purchaseTime: startTime,
                    verifiedAt: admin.firestore.FieldValue.serverTimestamp()
                });
            console.log('Successfully wrote to subscriptions subcollection');
        } catch (firestoreError) {
            console.error('Firestore write to subscriptions failed:', firestoreError.message);
            console.error('Firestore error code:', firestoreError.code);
            // Continue anyway - the purchase is valid, just Firestore storage failed
        }

        console.log(`Purchase verified successfully for user ${userId}`);

        return {
            isValid: true,
            expiryTime: expiryTimeMillis,
            autoRenewing: autoRenewing,
            planType: planType
        };

    } catch (error) {
        console.error('Verification error:', error);
        console.error('Error details:', JSON.stringify(error.response?.data || error.message, null, 2));
        throw new HttpsError('internal', 'Verification failed: ' + error.message);
    }
});

/**
 * Get subscription status for a user (V2 API)
 */
exports.getSubscriptionStatus = onCall({ region: 'us-central1' }, async (request) => {
    const auth = request.auth;
    const data = request.data;

    if (!auth) {
        throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const userId = data.userId || auth.uid;

    // Only allow users to query their own status
    if (userId !== auth.uid) {
        throw new HttpsError('permission-denied', 'Cannot query other users');
    }

    try {
        console.log(`Getting subscription status for user: ${userId}`);
        const userDoc = await admin.firestore().collection('users').doc(userId).get();

        if (!userDoc.exists) {
            console.log(`User document does not exist for ${userId}`);
            return {
                isActive: false,
                expiryTime: 0,
                productId: null,
                planType: null,
                autoRenewing: false
            };
        }

        const subscription = userDoc.data().subscription || {};
        console.log(`Subscription data from Firestore:`, JSON.stringify(subscription, null, 2));
        console.log(`subscription.isActive: ${subscription.isActive} (type: ${typeof subscription.isActive})`);
        console.log(`subscription.expiryTime: ${subscription.expiryTime}`);
        console.log(`Date.now(): ${Date.now()}`);
        console.log(`Expiry > Now: ${subscription.expiryTime > Date.now()}`);

        // Explicitly convert to boolean to handle both boolean and string "true"/"false"
        const isActiveInFirestore = subscription.isActive === true || subscription.isActive === 'true';
        const isNotExpired = subscription.expiryTime && subscription.expiryTime > Date.now();
        const isActive = isActiveInFirestore && isNotExpired;

        console.log(`isActiveInFirestore: ${isActiveInFirestore}`);
        console.log(`isNotExpired: ${isNotExpired}`);
        console.log(`Final isActive result: ${isActive}`);

        return {
            isActive: isActive,
            expiryTime: subscription.expiryTime || 0,
            productId: subscription.productId || null,
            planType: subscription.planType || null,
            autoRenewing: subscription.autoRenewing || false
        };

    } catch (error) {
        console.error('Get status error:', error);
        throw new HttpsError('internal', 'Failed to get status');
    }
});

/**
 * Register a device for a user (V2 API)
 */
exports.registerDevice = onCall({ region: 'us-central1' }, async (request) => {
    const auth = request.auth;
    const data = request.data;

    if (!auth) {
        throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { userId, deviceId, deviceName } = data;

    if (userId !== auth.uid) {
        throw new HttpsError('permission-denied', 'Cannot register for other users');
    }

    try {
        const userRef = admin.firestore().collection('users').doc(userId);
        const userDoc = await userRef.get();

        const deviceLimit = userDoc.data()?.deviceLimit || DEVICE_LIMIT;
        const devices = userDoc.data()?.devices || {};

        // Check if device already registered
        if (devices[deviceId]) {
            // Update last active time
            await userRef.update({
                [`devices.${deviceId}.lastActive`]: admin.firestore.FieldValue.serverTimestamp()
            });

            console.log(`Device ${deviceId} updated for user ${userId}`);

            return {
                success: true,
                deviceCount: Object.keys(devices).length,
                deviceLimit: deviceLimit
            };
        }

        // Check device limit
        if (Object.keys(devices).length >= deviceLimit) {
            console.log(`Device limit reached for user ${userId}`);
            return {
                success: false,
                deviceCount: Object.keys(devices).length,
                deviceLimit: deviceLimit,
                error: `Device limit reached (${deviceLimit} devices maximum)`
            };
        }

        // Register new device
        await userRef.set({
            devices: {
                [deviceId]: {
                    deviceName: deviceName,
                    firstRegistered: admin.firestore.FieldValue.serverTimestamp(),
                    lastActive: admin.firestore.FieldValue.serverTimestamp()
                }
            }
        }, { merge: true });

        console.log(`Device ${deviceId} registered for user ${userId}`);

        return {
            success: true,
            deviceCount: Object.keys(devices).length + 1,
            deviceLimit: deviceLimit
        };

    } catch (error) {
        console.error('Register device error:', error);
        throw new HttpsError('internal', 'Failed to register device');
    }
});

/**
 * Remove a device (V2 API)
 */
exports.removeDevice = onCall({ region: 'us-central1' }, async (request) => {
    const auth = request.auth;
    const data = request.data;

    if (!auth) {
        throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { userId, deviceId } = data;

    if (userId !== auth.uid) {
        throw new HttpsError('permission-denied', 'Cannot modify other users');
    }

    try {
        await admin.firestore().collection('users').doc(userId).update({
            [`devices.${deviceId}`]: admin.firestore.FieldValue.delete()
        });

        console.log(`Device ${deviceId} removed for user ${userId}`);

        return { success: true };
    } catch (error) {
        console.error('Remove device error:', error);
        return { success: false };
    }
});

/**
 * Get list of devices (V2 API)
 */
exports.getDevices = onCall({ region: 'us-central1' }, async (request) => {
    const auth = request.auth;
    const data = request.data;

    if (!auth) {
        throw new HttpsError('unauthenticated', 'User must be authenticated');
    }

    const userId = data.userId || auth.uid;

    if (userId !== auth.uid) {
        throw new HttpsError('permission-denied', 'Cannot query other users');
    }

    try {
        const userDoc = await admin.firestore().collection('users').doc(userId).get();
        const devices = userDoc.data()?.devices || {};

        const deviceList = Object.entries(devices).map(([deviceId, deviceData]) => ({
            deviceId: deviceId,
            deviceName: deviceData.deviceName,
            lastActive: deviceData.lastActive?._seconds * 1000 || 0,
            firstRegistered: deviceData.firstRegistered?._seconds * 1000 || 0
        }));

        return { devices: deviceList };
    } catch (error) {
        console.error('Get devices error:', error);
        throw new HttpsError('internal', 'Failed to get devices');
    }
});
