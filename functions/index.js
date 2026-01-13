const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { google } = require('googleapis');

admin.initializeApp();

// Initialize Google Play Developer API
const androidPublisher = google.androidpublisher({
    version: 'v3',
    auth: new google.auth.GoogleAuth({
        keyFile: './service-account-key.json',
        scopes: ['https://www.googleapis.com/auth/androidpublisher'],
    }),
});

const PACKAGE_NAME = 'com.parishod.atomatic';
const DEVICE_LIMIT = 3;

/**
 * Verify a purchase with Google Play Developer API
 */
exports.verifyPurchase = functions.https.onCall(async (data, context) => {
    // Ensure user is authenticated
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { purchaseToken, productId, orderId, packageName } = data;
    const userId = context.auth.uid;

    console.log(`Verifying purchase for user ${userId}, product ${productId}`);

    try {
        // Call Google Play Developer API to verify subscription
        const response = await androidPublisher.purchases.subscriptions.get({
            packageName: packageName || PACKAGE_NAME,
            subscriptionId: productId,
            token: purchaseToken,
        });

        const subscription = response.data;
        console.log('Google Play API response:', JSON.stringify(subscription, null, 2));

        // Check if subscription is valid
        const expiryTimeMillis = parseInt(subscription.expiryTimeMillis);
        const isActive = expiryTimeMillis > Date.now() &&
            subscription.paymentState === 1; // 1 = Payment received

        if (!isActive) {
            console.log('Subscription not active or payment pending');
            return {
                isValid: false,
                expiryTime: 0,
                autoRenewing: false,
                planType: '',
                error: 'Subscription not active or payment pending'
            };
        }

        // Determine plan type
        const planType = productId.includes('monthly') ? 'monthly' : 'annual';
        const autoRenewing = subscription.autoRenewing || false;

        // Store verified subscription in Firestore
        await admin.firestore().collection('users').doc(userId).set({
            subscription: {
                isActive: true,
                productId: productId,
                planType: planType,
                purchaseToken: purchaseToken,
                orderId: orderId,
                expiryTime: expiryTimeMillis,
                autoRenewing: autoRenewing,
                lastVerified: admin.firestore.FieldValue.serverTimestamp(),
                verified: true,
                verifiedBy: 'backend'
            }
        }, { merge: true });

        // Also store in subscription history
        await admin.firestore()
            .collection('users')
            .doc(userId)
            .collection('subscriptions')
            .doc(purchaseToken)
            .set({
                productId: productId,
                planType: planType,
                purchaseToken: purchaseToken,
                orderId: orderId,
                expiryTime: expiryTimeMillis,
                autoRenewing: autoRenewing,
                purchaseTime: parseInt(subscription.startTimeMillis),
                verifiedAt: admin.firestore.FieldValue.serverTimestamp()
            });

        console.log(`Purchase verified successfully for user ${userId}`);

        return {
            isValid: true,
            expiryTime: expiryTimeMillis,
            autoRenewing: autoRenewing,
            planType: planType
        };

    } catch (error) {
        console.error('Verification error:', error);
        throw new functions.https.HttpsError('internal', 'Verification failed: ' + error.message);
    }
});

/**
 * Get subscription status for a user
 */
exports.getSubscriptionStatus = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
    }

    const userId = data.userId || context.auth.uid;

    // Only allow users to query their own status
    if (userId !== context.auth.uid) {
        throw new functions.https.HttpsError('permission-denied', 'Cannot query other users');
    }

    try {
        const userDoc = await admin.firestore().collection('users').doc(userId).get();

        if (!userDoc.exists) {
            return {
                isActive: false,
                expiryTime: 0,
                productId: null,
                planType: null,
                autoRenewing: false
            };
        }

        const subscription = userDoc.data().subscription || {};
        const isActive = subscription.isActive && subscription.expiryTime > Date.now();

        return {
            isActive: isActive,
            expiryTime: subscription.expiryTime || 0,
            productId: subscription.productId || null,
            planType: subscription.planType || null,
            autoRenewing: subscription.autoRenewing || false
        };

    } catch (error) {
        console.error('Get status error:', error);
        throw new functions.https.HttpsError('internal', 'Failed to get status');
    }
});

/**
 * Register a device for a user
 */
exports.registerDevice = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { userId, deviceId, deviceName } = data;

    if (userId !== context.auth.uid) {
        throw new functions.https.HttpsError('permission-denied', 'Cannot register for other users');
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
        throw new functions.https.HttpsError('internal', 'Failed to register device');
    }
});

/**
 * Remove a device
 */
exports.removeDevice = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
    }

    const { userId, deviceId } = data;

    if (userId !== context.auth.uid) {
        throw new functions.https.HttpsError('permission-denied', 'Cannot modify other users');
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
 * Get list of devices
 */
exports.getDevices = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
    }

    const userId = data.userId || context.auth.uid;

    if (userId !== context.auth.uid) {
        throw new functions.https.HttpsError('permission-denied', 'Cannot query other users');
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
        throw new functions.https.HttpsError('internal', 'Failed to get devices');
    }
});
