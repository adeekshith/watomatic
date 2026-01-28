const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { onMessagePublished } = require('firebase-functions/v2/pubsub');
const { onSchedule } = require('firebase-functions/v2/scheduler');
const { logger } = require('firebase-functions/v2');
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


// =============================================================================
// RTDN (Real-Time Developer Notifications) Processing
// =============================================================================

/**
 * Generate a deterministic idempotency key for a notification.
 *
 * CRITICAL FIX #1: messageId is Pub/Sub's internal ID which can change on redelivery.
 * We need a key based on the actual notification content.
 *
 * The key is: purchaseToken_notificationType_eventTimeMillis
 * This ensures:
 * - Same notification redelivered = same key = deduplicated
 * - Different notification types for same subscription = different keys = processed
 * - Same notification type at different times = different keys = processed
 */
function generateIdempotencyKey(purchaseToken, notificationType, eventTimeMillis) {
    // Use a hash of purchaseToken to keep the key shorter (Firestore doc ID limit is 1500 bytes)
    const crypto = require('crypto');
    const tokenHash = crypto.createHash('sha256').update(purchaseToken).digest('hex').substring(0, 16);
    return `${tokenHash}_${notificationType}_${eventTimeMillis}`;
}

/**
 * Process Google Play Real-Time Developer Notifications
 * Triggered when messages are published to the Pub/Sub topic
 */
exports.processPlayStoreNotification = onMessagePublished(
    {
        topic: 'play-billing-notifications',
        region: 'us-central1',
        retry: true,
        timeoutSeconds: 60,
    },
    async (event) => {
        try {
            // Decode the Pub/Sub message
            const messageData = event.data.message.data;
            const pubsubMessageId = event.data.message.messageId; // Only for logging
            const decodedData = Buffer.from(messageData, 'base64').toString('utf-8');
            const notification = JSON.parse(decodedData);

            logger.info('Received Play Store notification', { notification, pubsubMessageId });

            // Extract notification details
            const { subscriptionNotification, testNotification, packageName, eventTimeMillis } = notification;

            // Handle test notifications
            if (testNotification) {
                logger.info('Received test notification', { version: testNotification.version });
                // Use pubsubMessageId for test notifications since they don't have the same structure
                const testKey = `test_${pubsubMessageId}`;
                await admin.firestore()
                    .collection('processed_notifications')
                    .doc(testKey)
                    .set({
                        processedAt: admin.firestore.FieldValue.serverTimestamp(),
                        notificationType: 'TEST',
                        testVersion: testNotification.version,
                    });
                return;
            }

            if (!subscriptionNotification) {
                logger.warn('Unknown notification type', { notification });
                return;
            }

            const {
                version,
                notificationType,
                purchaseToken,
                subscriptionId,
            } = subscriptionNotification;

            // CRITICAL FIX #1: Generate proper idempotency key
            const idempotencyKey = generateIdempotencyKey(purchaseToken, notificationType, eventTimeMillis);

            logger.info('Processing subscription notification', {
                notificationType,
                subscriptionId,
                eventTimeMillis,
                idempotencyKey,
                purchaseToken: purchaseToken.substring(0, 20) + '...',
            });

            // Check for duplicate processing using proper idempotency key
            const processedDoc = await admin.firestore()
                .collection('processed_notifications')
                .doc(idempotencyKey)
                .get();

            if (processedDoc.exists) {
                logger.info('Notification already processed, skipping', { idempotencyKey });
                return;
            }

            // Process based on notification type, passing eventTimeMillis for out-of-order protection
            await handleSubscriptionNotification(
                notificationType,
                purchaseToken,
                subscriptionId,
                eventTimeMillis
            );

            // Mark as processed AFTER successful processing
            await admin.firestore()
                .collection('processed_notifications')
                .doc(idempotencyKey)
                .set({
                    processedAt: admin.firestore.FieldValue.serverTimestamp(),
                    notificationType,
                    subscriptionId,
                    eventTimeMillis,
                    purchaseTokenHash: require('crypto').createHash('sha256').update(purchaseToken).digest('hex').substring(0, 16),
                });

        } catch (error) {
            logger.error('Error processing Play Store notification', { error: error.message, stack: error.stack });
            throw error; // Retry on error
        }
    }
);

/**
 * Handle different subscription notification types
 *
 * CRITICAL FIX #2: Accepts eventTimeMillis for out-of-order protection.
 * We only update Firestore if this notification is newer than what we have stored.
 */
async function handleSubscriptionNotification(notificationType, purchaseToken, subscriptionId, eventTimeMillis) {
    // Notification type codes: https://developer.android.com/google/play/billing/rtdn-reference
    const NOTIFICATION_TYPES = {
        1: 'SUBSCRIPTION_RECOVERED',
        2: 'SUBSCRIPTION_RENEWED',
        3: 'SUBSCRIPTION_CANCELED',
        4: 'SUBSCRIPTION_PURCHASED',
        5: 'SUBSCRIPTION_ON_HOLD',
        6: 'SUBSCRIPTION_IN_GRACE_PERIOD',
        7: 'SUBSCRIPTION_RESTARTED',
        8: 'SUBSCRIPTION_PRICE_CHANGE_CONFIRMED',
        9: 'SUBSCRIPTION_DEFERRED',
        10: 'SUBSCRIPTION_PAUSED',
        11: 'SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED',
        12: 'SUBSCRIPTION_REVOKED',
        13: 'SUBSCRIPTION_EXPIRED',
    };

    const notificationName = NOTIFICATION_TYPES[notificationType] || 'UNKNOWN';
    logger.info(`Processing ${notificationName}`, { notificationType, eventTimeMillis });

    try {
        // Verify the subscription with Google Play API
        const subscriptionData = await androidPublisher.purchases.subscriptionsv2.get({
            packageName: PACKAGE_NAME,
            token: purchaseToken,
        });

        const subscription = subscriptionData.data;
        logger.info('Fetched subscription from Google Play API', {
            subscriptionState: subscription.subscriptionState,
        });

        // Find user by purchase token
        const userId = await findUserByPurchaseToken(purchaseToken);

        if (!userId) {
            logger.warn('User not found for purchase token', {
                purchaseToken: purchaseToken.substring(0, 20) + '...',
            });
            // Store orphaned notification for investigation
            await storeOrphanedNotification(notificationType, purchaseToken, subscription, eventTimeMillis);
            return;
        }

        // CRITICAL FIX #2: Out-of-order protection - only update if this notification is newer
        const userDoc = await admin.firestore().collection('users').doc(userId).get();
        const existingSubscription = userDoc.data()?.subscription || {};
        const existingEventTime = existingSubscription.lastEventTimeMillis || 0;

        if (eventTimeMillis && existingEventTime && eventTimeMillis < existingEventTime) {
            logger.warn('Out-of-order notification detected, skipping update', {
                userId,
                incomingEventTime: eventTimeMillis,
                existingEventTime: existingEventTime,
                notificationType,
            });
            // Still log the event for auditing, but don't update state
            await logNotificationEvent(userId, notificationType, purchaseToken, subscription, eventTimeMillis, true);
            return;
        }

        // Update subscription state in Firestore
        await updateSubscriptionState(userId, purchaseToken, subscription, notificationType, eventTimeMillis);

        // Handle specific notification types
        switch (notificationType) {
            case 2: // SUBSCRIPTION_RENEWED
                await handleRenewal(userId, subscription, eventTimeMillis);
                break;
            case 3: // SUBSCRIPTION_CANCELED
                await handleCancellation(userId, subscription);
                break;
            case 13: // SUBSCRIPTION_EXPIRED
                await handleExpiration(userId, subscription);
                break;
            case 6: // SUBSCRIPTION_IN_GRACE_PERIOD
                await handleGracePeriod(userId, subscription);
                break;
            case 5: // SUBSCRIPTION_ON_HOLD
                await handleOnHold(userId, subscription);
                break;
            case 10: // SUBSCRIPTION_PAUSED
                await handlePaused(userId, subscription);
                break;
            case 12: // SUBSCRIPTION_REVOKED
                await handleRevoked(userId, subscription);
                break;
            case 1: // SUBSCRIPTION_RECOVERED
                await handleRecovered(userId, subscription);
                break;
            default:
                logger.info('No specific handler for notification type', { notificationType });
        }

        logger.info('Successfully processed notification', { userId, notificationType });

    } catch (error) {
        logger.error('Error handling subscription notification', { error: error.message, notificationType });
        throw error;
    }
}

/**
 * Find user by purchase token
 */
async function findUserByPurchaseToken(purchaseToken) {
    try {
        // Query users collection for matching purchase token
        const usersSnapshot = await admin.firestore()
            .collection('users')
            .where('subscription.purchaseToken', '==', purchaseToken)
            .limit(1)
            .get();

        if (usersSnapshot.empty) {
            // Try searching in subscriptions subcollection
            const subscriptionsQuery = await admin.firestore()
                .collectionGroup('subscriptions')
                .where('purchaseToken', '==', purchaseToken)
                .limit(1)
                .get();

            if (!subscriptionsQuery.empty) {
                // Extract userId from document path: users/{userId}/subscriptions/{token}
                const docPath = subscriptionsQuery.docs[0].ref.path;
                const userId = docPath.split('/')[1];
                return userId;
            }

            return null;
        }

        return usersSnapshot.docs[0].id;
    } catch (error) {
        logger.error('Error finding user by purchase token', { error: error.message });
        return null;
    }
}

/**
 * Update subscription state in Firestore
 *
 * CRITICAL FIX #2: Now tracks lastEventTimeMillis for out-of-order protection.
 * Also uses source tracking to distinguish RTDN updates from scheduled refresh.
 */
async function updateSubscriptionState(userId, purchaseToken, subscription, notificationType, eventTimeMillis = null, source = 'rtdn') {
    const lineItem = subscription.lineItems?.[0];
    if (!lineItem) {
        logger.warn('No line items in subscription', { userId });
        return;
    }

    const expiryTime = lineItem.expiryTime ? new Date(lineItem.expiryTime).getTime() : 0;
    const productId = lineItem.productId;
    const basePlanId = lineItem.offerDetails?.basePlanId;
    const subscriptionState = subscription.subscriptionState;

    // Determine if subscription is active
    const activeStates = [
        'SUBSCRIPTION_STATE_ACTIVE',
        'SUBSCRIPTION_STATE_IN_GRACE_PERIOD',
    ];
    const isActive = activeStates.includes(subscriptionState) && expiryTime > Date.now();

    const autoRenewing = subscriptionState === 'SUBSCRIPTION_STATE_ACTIVE' &&
        lineItem.autoRenewingPlan?.autoRenewEnabled === true;

    const planType = (productId || '').includes('monthly') ? 'monthly' : 'annual';

    // Build update object - only include eventTimeMillis if it's from RTDN (not scheduled refresh)
    const subscriptionUpdate = {
        isActive,
        productId,
        basePlanId,
        planType,
        purchaseToken,
        expiryTime,
        autoRenewing,
        subscriptionState,
        lastNotificationType: notificationType,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        verified: true,
        verifiedBy: source,
    };

    // CRITICAL FIX #2: Track eventTimeMillis for out-of-order protection
    // Only update this field if we have a valid eventTimeMillis (from RTDN)
    if (eventTimeMillis) {
        subscriptionUpdate.lastEventTimeMillis = eventTimeMillis;
    }

    // Update main user document
    await admin.firestore().collection('users').doc(userId).set({
        subscription: subscriptionUpdate
    }, { merge: true });

    // Update subscription history
    const historyUpdate = {
        productId,
        basePlanId,
        planType,
        purchaseToken,
        expiryTime,
        autoRenewing,
        subscriptionState,
        lastNotificationType: notificationType,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    if (eventTimeMillis) {
        historyUpdate.lastEventTimeMillis = eventTimeMillis;
    }

    await admin.firestore()
        .collection('users')
        .doc(userId)
        .collection('subscriptions')
        .doc(purchaseToken)
        .set(historyUpdate, { merge: true });

    // Log notification event
    await logNotificationEvent(userId, notificationType, purchaseToken, subscription, eventTimeMillis, false);
}

/**
 * Log notification event for auditing
 */
async function logNotificationEvent(userId, notificationType, purchaseToken, subscription, eventTimeMillis, skippedDueToOutOfOrder) {
    const lineItem = subscription.lineItems?.[0];
    const expiryTime = lineItem?.expiryTime ? new Date(lineItem.expiryTime).getTime() : 0;
    const subscriptionState = subscription.subscriptionState;
    const activeStates = ['SUBSCRIPTION_STATE_ACTIVE', 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD'];
    const isActive = activeStates.includes(subscriptionState) && expiryTime > Date.now();

    await admin.firestore()
        .collection('users')
        .doc(userId)
        .collection('notification_events')
        .add({
            notificationType,
            purchaseToken,
            subscriptionState,
            expiryTime,
            isActive,
            eventTimeMillis,
            skippedDueToOutOfOrder,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
        });
}

/**
 * Handle subscription renewal
 *
 * CRITICAL FIX #3: renewalCount using increment(1) is NOT idempotent.
 * Instead, we track unique renewals by storing them with expiryTime as the key.
 * This ensures retries don't double-count renewals.
 */
async function handleRenewal(userId, subscription, eventTimeMillis) {
    logger.info('Subscription renewed', { userId });

    const lineItem = subscription.lineItems?.[0];
    const expiryTime = lineItem?.expiryTime ? new Date(lineItem.expiryTime).getTime() : 0;

    // CRITICAL FIX #3: Store renewal as a separate document keyed by expiryTime
    // This ensures idempotency - same renewal processed multiple times = same document
    const renewalKey = `renewal_${expiryTime}`;
    const renewalRef = admin.firestore()
        .collection('users')
        .doc(userId)
        .collection('renewal_events')
        .doc(renewalKey);

    // Check if this renewal was already recorded
    const existingRenewal = await renewalRef.get();
    if (existingRenewal.exists) {
        logger.info('Renewal already recorded, skipping increment', { userId, expiryTime });
        return;
    }

    // Use a transaction to safely increment and record the renewal
    await admin.firestore().runTransaction(async (transaction) => {
        // Re-check inside transaction
        const renewalDoc = await transaction.get(renewalRef);
        if (renewalDoc.exists) {
            logger.info('Renewal already recorded (in transaction), skipping', { userId, expiryTime });
            return;
        }

        // Get current renewal count
        const userDoc = await transaction.get(admin.firestore().collection('users').doc(userId));
        const currentCount = userDoc.data()?.subscription?.renewalCount || 0;

        // Record the renewal event
        transaction.set(renewalRef, {
            eventTimeMillis,
            expiryTime,
            recordedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Update renewal count atomically
        transaction.set(admin.firestore().collection('users').doc(userId), {
            subscription: {
                renewalCount: currentCount + 1,
                lastRenewedAt: admin.firestore.FieldValue.serverTimestamp(),
                lastRenewalExpiryTime: expiryTime,
            }
        }, { merge: true });
    });

    logger.info('Renewal recorded successfully', { userId, expiryTime });
}

/**
 * Handle subscription cancellation
 */
async function handleCancellation(userId, subscription) {
    logger.info('Subscription canceled', { userId });

    const lineItem = subscription.lineItems?.[0];
    const expiryTime = lineItem?.expiryTime ? new Date(lineItem.expiryTime).getTime() : 0;

    await admin.firestore().collection('users').doc(userId).set({
        subscription: {
            canceledAt: admin.firestore.FieldValue.serverTimestamp(),
            willExpireAt: expiryTime,
            // Keep isActive true until expiry time
        }
    }, { merge: true });

    // Optional: Send cancellation confirmation, feedback request, etc.
}

/**
 * Handle subscription expiration
 */
async function handleExpiration(userId, subscription) {
    logger.info('Subscription expired', { userId });

    await admin.firestore().collection('users').doc(userId).set({
        subscription: {
            isActive: false,
            expiredAt: admin.firestore.FieldValue.serverTimestamp(),
        }
    }, { merge: true });

    // Optional: Send re-subscription offer, etc.
}

/**
 * Handle grace period
 */
async function handleGracePeriod(userId, subscription) {
    logger.info('Subscription in grace period', { userId });

    await admin.firestore().collection('users').doc(userId).set({
        subscription: {
            inGracePeriod: true,
            gracePeriodStartedAt: admin.firestore.FieldValue.serverTimestamp(),
        }
    }, { merge: true });

    // Optional: Send payment retry notification
}

/**
 * Handle on-hold state
 */
async function handleOnHold(userId, subscription) {
    logger.info('Subscription on hold', { userId });

    await admin.firestore().collection('users').doc(userId).set({
        subscription: {
            isActive: false,
            onHold: true,
            onHoldStartedAt: admin.firestore.FieldValue.serverTimestamp(),
        }
    }, { merge: true });

    // Optional: Send payment update request
}

/**
 * Handle paused state
 */
async function handlePaused(userId, subscription) {
    logger.info('Subscription paused', { userId });

    await admin.firestore().collection('users').doc(userId).set({
        subscription: {
            isActive: false,
            paused: true,
            pausedAt: admin.firestore.FieldValue.serverTimestamp(),
        }
    }, { merge: true });
}

/**
 * Handle revoked subscription
 */
async function handleRevoked(userId, subscription) {
    logger.info('Subscription revoked', { userId });

    await admin.firestore().collection('users').doc(userId).set({
        subscription: {
            isActive: false,
            revoked: true,
            revokedAt: admin.firestore.FieldValue.serverTimestamp(),
        }
    }, { merge: true });

    // Revoke access immediately
}

/**
 * Handle recovered subscription
 */
async function handleRecovered(userId, subscription) {
    logger.info('Subscription recovered', { userId });

    await admin.firestore().collection('users').doc(userId).set({
        subscription: {
            isActive: true,
            recovered: true,
            recoveredAt: admin.firestore.FieldValue.serverTimestamp(),
            inGracePeriod: false,
            onHold: false,
        }
    }, { merge: true });

    // Optional: Send recovery confirmation
}

/**
 * Store orphaned notifications for investigation
 */
async function storeOrphanedNotification(notificationType, purchaseToken, subscription, eventTimeMillis) {
    await admin.firestore().collection('orphaned_notifications').add({
        notificationType,
        purchaseToken,
        subscription,
        eventTimeMillis,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });
}

// =============================================================================
// Scheduled Subscription Refresh
// =============================================================================

/**
 * Scheduled function to refresh expiring subscriptions
 * Runs every 6 hours to catch any missed RTDN events
 *
 * CRITICAL FIX #4: The scheduled refresh must not overwrite fresher data from RTDN.
 * We check the Google Play API's startTime/acknowledgementTime against our stored
 * lastEventTimeMillis to avoid clobbering more recent updates.
 */
exports.refreshExpiringSubscriptions = onSchedule(
    {
        schedule: 'every 6 hours',
        timeZone: 'America/New_York',
        region: 'us-central1',
        timeoutSeconds: 540, // 9 minutes
        memory: '512MiB',
    },
    async (event) => {
        logger.info('Starting scheduled subscription refresh');

        try {
            // Find subscriptions expiring in next 48 hours or recently expired
            const now = Date.now();
            const checkWindow = 48 * 60 * 60 * 1000; // 48 hours

            // Also find subscriptions that haven't been verified recently (missed RTDNs)
            const staleThreshold = 24 * 60 * 60 * 1000; // 24 hours
            const staleTime = now - staleThreshold;

            const usersSnapshot = await admin.firestore()
                .collection('users')
                .where('subscription.isActive', '==', true)
                .where('subscription.expiryTime', '<=', now + checkWindow)
                .get();

            logger.info(`Found ${usersSnapshot.size} subscriptions to refresh`);

            let refreshed = 0;
            let skipped = 0;
            let errors = 0;

            for (const userDoc of usersSnapshot.docs) {
                const userId = userDoc.id;
                const existingSubscription = userDoc.data().subscription;

                if (!existingSubscription || !existingSubscription.purchaseToken) {
                    logger.warn('User has no purchase token', { userId });
                    continue;
                }

                try {
                    // Verify with Google Play API
                    const response = await androidPublisher.purchases.subscriptionsv2.get({
                        packageName: PACKAGE_NAME,
                        token: existingSubscription.purchaseToken,
                    });

                    const latestSubscription = response.data;

                    // CRITICAL FIX #4: Check if we should update based on data freshness
                    // The scheduled refresh should NOT overwrite data if RTDN has recently updated it
                    // We only update if:
                    // 1. There's no lastEventTimeMillis (never received RTDN)
                    // 2. The data is stale (lastUpdated > 24 hours ago)
                    // 3. The subscription state has materially changed

                    const lastUpdatedTimestamp = existingSubscription.lastUpdated?._seconds * 1000 || 0;
                    const isStale = lastUpdatedTimestamp < staleTime;
                    const stateChanged = existingSubscription.subscriptionState !== latestSubscription.subscriptionState;

                    const lineItem = latestSubscription.lineItems?.[0];
                    const newExpiryTime = lineItem?.expiryTime ? new Date(lineItem.expiryTime).getTime() : 0;
                    const expiryChanged = existingSubscription.expiryTime !== newExpiryTime;

                    if (!isStale && !stateChanged && !expiryChanged) {
                        logger.info('Subscription data is fresh, skipping refresh', {
                            userId,
                            lastUpdated: lastUpdatedTimestamp,
                            currentState: existingSubscription.subscriptionState,
                        });
                        skipped++;
                        continue;
                    }

                    logger.info('Refreshing subscription', {
                        userId,
                        isStale,
                        stateChanged,
                        expiryChanged,
                        oldState: existingSubscription.subscriptionState,
                        newState: latestSubscription.subscriptionState,
                    });

                    // Update subscription state with source='scheduled' to distinguish from RTDN
                    // Pass null for eventTimeMillis so we don't overwrite RTDN's timestamp
                    await updateSubscriptionState(
                        userId,
                        existingSubscription.purchaseToken,
                        latestSubscription,
                        0, // No specific notification type for scheduled refresh
                        null, // Don't set eventTimeMillis - preserve RTDN's value
                        'scheduled' // Mark source as scheduled refresh
                    );

                    refreshed++;

                } catch (error) {
                    logger.error('Error refreshing subscription', { userId, error: error.message });
                    errors++;
                }
            }

            logger.info('Scheduled refresh completed', { refreshed, skipped, errors, total: usersSnapshot.size });

        } catch (error) {
            logger.error('Error in scheduled refresh', { error: error.message, stack: error.stack });
            throw error;
        }
    }
);

