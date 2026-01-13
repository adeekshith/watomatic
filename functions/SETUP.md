# Firebase Cloud Functions Setup Guide

## Prerequisites
- Node.js 18 or higher
- Firebase CLI installed: `npm install -g firebase-tools`
- Google Play Console access
- Firebase project created

## Step 1: Google Play Developer API Setup

### 1.1 Enable API in Google Cloud Console
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your Firebase project
3. Navigate to **APIs & Services** → **Library**
4. Search for "Google Play Android Developer API"
5. Click **Enable**

### 1.2 Create Service Account
1. In Cloud Console, go to **IAM & Admin** → **Service Accounts**
2. Click **Create Service Account**
3. Name: `play-developer-api`
4. Grant role: **Service Account User**
5. Click **Create and Continue**
6. Click **Done**

### 1.3 Create JSON Key
1. Click on the service account you just created
2. Go to **Keys** tab
3. Click **Add Key** → **Create new key**
4. Select **JSON** format
5. Click **Create**
6. Save the downloaded file as `service-account-key.json`

### 1.4 Grant Play Console Access
1. Go to [Google Play Console](https://play.google.com/console/)
2. Navigate to **Setup** → **API access**
3. Find your service account in the list
4. Click **Grant access**
5. Select permissions:
   - ✅ View financial data
   - ✅ Manage orders and subscriptions
6. Click **Invite user** → **Send invitation**

## Step 2: Firebase Functions Setup

### 2.1 Install Dependencies
```bash
cd functions
npm install
```

### 2.2 Add Service Account Key
```bash
# Copy your service account key to functions directory
cp /path/to/downloaded-key.json ./service-account-key.json

# Verify it's in .gitignore (it should be)
cat .gitignore | grep service-account-key.json
```

### 2.3 Initialize Firebase (if not already done)
```bash
# Login to Firebase
firebase login

# Initialize project
firebase init functions

# Select:
# - Use existing project
# - JavaScript
# - No ESLint
# - No install dependencies (already done)
```

### 2.4 Update Package Name
Edit `functions/index.js` and update line 15:
```javascript
const PACKAGE_NAME = 'com.parishod.atomatic'; // Your actual package name
```

## Step 3: Test Locally

### 3.1 Start Firebase Emulator
```bash
cd functions
npm run serve
```

This starts the emulator at `http://localhost:5001`

### 3.2 Test Functions
You can test functions using curl or the Firebase console:

```bash
# Test verifyPurchase (requires auth token)
curl -X POST http://localhost:5001/YOUR_PROJECT_ID/us-central1/verifyPurchase \
  -H "Content-Type: application/json" \
  -d '{
    "data": {
      "purchaseToken": "test-token",
      "productId": "automatic_ai_pro_monthly",
      "orderId": "test-order"
    }
  }'
```

## Step 4: Deploy to Firebase

### 4.1 Deploy Functions
```bash
firebase deploy --only functions
```

### 4.2 Verify Deployment
```bash
# List deployed functions
firebase functions:list

# View logs
firebase functions:log
```

## Step 5: Configure Firestore Security Rules

Create/update `firestore.rules`:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      match /subscriptions/{purchaseToken} {
        allow read: if request.auth != null && request.auth.uid == userId;
        // Only backend can write subscriptions
        allow write: if false;
      }
    }
  }
}
```

Deploy rules:
```bash
firebase deploy --only firestore:rules
```

## Step 6: Create Subscription Products in Play Console

1. Go to Google Play Console
2. Navigate to **Monetize** → **Subscriptions**
3. Click **Create subscription**

### Monthly Plan
- **Product ID**: `automatic_ai_pro_monthly`
- **Name**: Automatic AI Pro Monthly
- **Description**: AI-powered automatic replies - Monthly subscription
- **Price**: $1.99/month
- **Billing period**: 1 month
- **Free trial**: Optional (e.g., 7 days)

### Annual Plan
- **Product ID**: `automatic_ai_pro_annual`
- **Name**: Automatic AI Pro Annual
- **Description**: AI-powered automatic replies - Annual subscription (Save 54%)
- **Price**: $10.99/year
- **Billing period**: 1 year
- **Free trial**: Optional (e.g., 7 days)

## Step 7: Add Test Accounts

1. In Play Console, go to **Setup** → **License testing**
2. Add test Gmail accounts
3. These accounts can make test purchases without being charged

## Troubleshooting

### Error: "Service account not found"
- Make sure you granted access in Play Console (Step 1.4)
- Wait 10-15 minutes for permissions to propagate

### Error: "Invalid credentials"
- Verify `service-account-key.json` is in the functions directory
- Check the file has valid JSON format
- Ensure the service account has the correct permissions

### Error: "Package name mismatch"
- Update `PACKAGE_NAME` in `functions/index.js`
- Must match your app's package name exactly

### Functions not deploying
```bash
# Check Firebase project
firebase projects:list

# Use correct project
firebase use YOUR_PROJECT_ID

# Try deploying with verbose logging
firebase deploy --only functions --debug
```

## Monitoring

### View Function Logs
```bash
# Real-time logs
firebase functions:log --only verifyPurchase

# All functions
firebase functions:log
```

### Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **Functions** to see:
   - Invocation count
   - Execution time
   - Error rate
   - Logs

## Cost Estimation

Firebase Cloud Functions pricing (as of 2024):
- **Free tier**: 2M invocations/month, 400K GB-seconds
- **After free tier**: $0.40 per million invocations

For a typical subscription app:
- ~10 verifications per user per month
- 1000 users = 10,000 invocations/month
- **Cost**: FREE (well within free tier)

## Security Best Practices

1. ✅ Never commit `service-account-key.json` to git
2. ✅ Use Firestore security rules to restrict access
3. ✅ Validate all inputs in Cloud Functions
4. ✅ Log all verification attempts for audit trail
5. ✅ Monitor function invocations for unusual activity

## Next Steps

After setup is complete:
1. Test purchase flow in your app
2. Verify backend verification is working
3. Check Firestore for subscription data
4. Monitor Cloud Functions logs
5. Test device limit enforcement

## Support

If you encounter issues:
1. Check Firebase Functions logs
2. Verify Google Play API credentials
3. Test with Firebase emulator first
4. Check Firestore security rules
