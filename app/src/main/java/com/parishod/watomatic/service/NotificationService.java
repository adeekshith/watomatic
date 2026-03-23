package com.parishod.watomatic.service;

import static com.parishod.watomatic.model.utils.Constants.DEFAULT_LLM_MODEL;
import static com.parishod.watomatic.model.utils.Constants.DEFAULT_LLM_PROMPT;

import android.app.PendingIntent;
import android.content.res.Resources;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
// import Constants.kt
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import com.parishod.watomatic.model.utils.Constants;


import androidx.annotation.NonNull;
import androidx.core.app.RemoteInput;

import com.parishod.watomatic.NotificationWear;
import com.parishod.watomatic.R;
import com.parishod.watomatic.model.CustomRepliesData;
import com.parishod.watomatic.network.AtomaticAIService;
import com.parishod.watomatic.network.OpenAIService;
import com.parishod.watomatic.network.RetrofitInstance;
import com.parishod.watomatic.network.model.atomatic.AtomaticAIErrorResponse;
import com.parishod.watomatic.network.model.atomatic.AtomaticAIRequest;
import com.parishod.watomatic.network.model.atomatic.AtomaticAIResponse;
import com.parishod.watomatic.network.model.openai.Message;
import com.parishod.watomatic.network.model.openai.OpenAIRequest;
import com.parishod.watomatic.network.model.openai.OpenAIResponse;
import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.model.utils.ContactsHelper;
import com.parishod.watomatic.model.utils.DbUtils;
import com.parishod.watomatic.model.utils.NotificationHelper;
import com.parishod.watomatic.model.utils.NotificationUtils;
import com.parishod.watomatic.utils.FirebaseTokenRefresher;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static java.lang.Math.max;

public class NotificationService extends NotificationListenerService {
    private final String TAG = NotificationService.class.getSimpleName();
    // CustomRepliesData customRepliesData; // Will be initialized locally where needed or passed
    private DbUtils dbUtils;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        if (canReply(sbn) && shouldReply(sbn)) {
            sendReply(sbn);
        }
    }

    private boolean canReply(StatusBarNotification sbn) {
        return isServiceEnabled() &&
                isSupportedPackage(sbn) &&
                NotificationUtils.isNewNotification(sbn) &&
                isGroupMessageAndReplyAllowed(sbn) &&
                canSendReplyNow(sbn);
    }

    private boolean shouldReply(StatusBarNotification sbn) {
        PreferencesManager prefs = PreferencesManager.getPreferencesInstance(this);
        boolean isGroup = sbn.getNotification().extras.getBoolean("android.isGroupConversation");

        //Check contact based replies
        if (prefs.isContactReplyEnabled() && !isGroup) {
            //Title contains sender name (at least on WhatsApp)
            String senderName = sbn.getNotification().extras.getString("android.title");
            //Check if should reply to contact
            boolean isNameSelected =
                    (ContactsHelper.Companion.getInstance(this).hasContactPermission()
                            && prefs.getReplyToNames().contains(senderName)) ||
                            prefs.getCustomReplyNames().contains(senderName);
            if ((isNameSelected && prefs.isContactReplyBlacklistMode()) ||
                    !isNameSelected && !prefs.isContactReplyBlacklistMode()) {
                //If contact is on the list and contact reply is on blacklist mode, 
                // or contact is not in the list and reply is on whitelist mode,
                // we don't want to reply
                return false;
            }
        }

        //Check more conditions on future feature implementations

        //If we got here, all conditions to reply are met
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //START_STICKY  to order the system to restart your service as soon as possible when it was killed.
        return START_STICKY;
    }

    private void sendActualReply(StatusBarNotification sbn, NotificationWear notificationWear, String replyText) {
        // customRepliesData = CustomRepliesData.getInstance(this); // Initialize if other methods from it are needed beyond replyText

        RemoteInput finalRemoteIn = null;
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        Bundle localBundle = new Bundle(); // notificationWear.bundle;
        for (RemoteInput remoteIn : notificationWear.getRemoteInputs()) {
            if(remoteIn.getAllowFreeFormInput()) {
                finalRemoteIn = remoteIn;
                localBundle.putCharSequence(finalRemoteIn.getResultKey(), replyText);
                break;
            }
        }

        if(finalRemoteIn == null) return;

        RemoteInput.addResultsToIntent(new RemoteInput[]{ finalRemoteIn }, localIntent, localBundle);
        try {
            if (notificationWear.getPendingIntent() != null) {
                if (dbUtils == null) {
                    dbUtils = new DbUtils(getApplicationContext());
                }
                dbUtils.logReply(sbn, NotificationUtils.getTitle(sbn));
                
                // Use ReplyService to send the reply in foreground
                /*Intent replyServiceIntent = new Intent(this, ReplyService.class);
                replyServiceIntent.putExtra("pendingIntent", notificationWear.getPendingIntent());
                replyServiceIntent.putExtra("fillInIntent", localIntent);
                
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(replyServiceIntent);
                    } else {
                        startService(replyServiceIntent);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start ReplyService: " + e.getMessage());
                    // Fallback to direct send if service start fails (though likely to fail for FB)
                    notificationWear.getPendingIntent().send(this, 0, localIntent);
                }*/

                notificationWear.getPendingIntent().send(this, 0, localIntent);
                if (PreferencesManager.getPreferencesInstance(this).isShowNotificationEnabled()) {
                    NotificationHelper.getInstance(getApplicationContext()).sendNotification(sbn.getNotification().extras.getString("android.title"), sbn.getNotification().extras.getString("android.text"), sbn.getPackageName());
                }
                cancelNotification(sbn.getKey());
                if (canPurgeMessages()) {
                    dbUtils.purgeMessageLogs();
                    PreferencesManager.getPreferencesInstance(this).setPurgeMessageTime(System.currentTimeMillis());
                }
            }
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "sendActualReply error: " + e.getLocalizedMessage());
        }
    }

    private void sendReply(StatusBarNotification sbn) {
        final NotificationWear notificationWear = NotificationUtils.extractWearNotification(sbn);
        if (notificationWear.getRemoteInputs().isEmpty()) {
            return;
        }

        PreferencesManager preferencesManager = PreferencesManager.getPreferencesInstance(this);
        CustomRepliesData customRepliesData = CustomRepliesData.getInstance(this); // For fallback
        String replyText = customRepliesData.getTextToSendOrElse();
        if(preferencesManager.isAnyAiRepliesEnabled()){
            try {
                replyText = getString(R.string.auto_reply_default_message);
            } catch (Resources.NotFoundException e) {
                replyText = "I am currently busy. Will reply later.";
            }
        }
        String fallbackReplyText = replyText; // needs to be final to access in inner class hence one more variable

        CharSequence incomingMessageChars = sbn.getNotification().extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
        String incomingMessage = (incomingMessageChars != null) ? incomingMessageChars.toString() : null;

        // Determine if AI should be used based on the selected reply method
        boolean shouldUseAI = false;

        if (preferencesManager.isAutomaticAiRepliesEnabled()) {
            // Automatic AI: requires subscription but no API key
            shouldUseAI = preferencesManager.isSubscriptionActive() &&
                         incomingMessage != null && !incomingMessage.trim().isEmpty();
            Log.d(TAG, "Automatic AI mode - Subscription active: " + preferencesManager.isSubscriptionActive());
        } else if (preferencesManager.isByokRepliesEnabled()) {
            // BYOK: requires API key but no subscription
            String apiKey = preferencesManager.getOpenAIApiKey();
            shouldUseAI = apiKey != null && !apiKey.trim().isEmpty() &&
                         !apiKey.equals("PENDING_CONFIGURATION") &&
                         incomingMessage != null && !incomingMessage.trim().isEmpty();
            Log.d(TAG, "BYOK mode - API key configured: " + (apiKey != null && !apiKey.trim().isEmpty()));
        }

        if (shouldUseAI) {
            Log.d(TAG, "AI conditions met. Attempting to get AI reply.");
            fetchAiReply(sbn, notificationWear, incomingMessage, fallbackReplyText);
        } else {
            Log.d(TAG, "AI conditions not met. Using default reply.");
            sendActualReply(sbn, notificationWear, fallbackReplyText);
        }
    }

    private void fetchAiReply(StatusBarNotification sbn, NotificationWear notificationWear, String incomingMessage, String fallbackReplyText) {
        PreferencesManager prefs = PreferencesManager.getPreferencesInstance(this);

        // Determine which backend API to use based on the selected reply method
        String apiKey;
        String provider;
        String baseUrl;

        if (prefs.isAutomaticAiRepliesEnabled()) {
            // Automatic AI: Use server-side API (Atomatic backend)
            Log.d(TAG, "Using Automatic AI (server-based) backend");
            fetchAtomaticAiReply(sbn, notificationWear, incomingMessage, fallbackReplyText);
            return;
        } else if (prefs.isByokRepliesEnabled()) {
            // BYOK: Use user's own API key with configured provider
            Log.d(TAG, "Using BYOK (client-based) backend");
            apiKey = prefs.getOpenAIApiKey();
            provider = prefs.getOpenApiSource();
            if (provider == null) provider = "OpenAI";
        } else {
            // No AI mode enabled, use fallback
            sendActualReply(sbn, notificationWear, fallbackReplyText);
            return;
        }

        // BYOK flow continues here
        String model = prefs.getSelectedOpenAIModel();
        String systemPrompt = prefs.getOpenAICustomPrompt();
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) systemPrompt = DEFAULT_LLM_PROMPT;
        if (model == null || model.isEmpty()) model = DEFAULT_LLM_MODEL;

        baseUrl = Constants.INSTANCE.getPROVIDER_URLS().get(provider);
        if ("Custom".equals(provider)) {
            baseUrl = prefs.getCustomOpenAIApiUrl();
        }
        if (baseUrl == null) baseUrl = "https://api.openai.com/";

        if (!baseUrl.endsWith("/")) baseUrl += "/";

        OpenAIService service = RetrofitInstance.getOpenAIRetrofitInstance(baseUrl).create(OpenAIService.class);

        if ("Claude".equals(provider)) {
            fetchClaudeReply(service, baseUrl, apiKey, model, systemPrompt, incomingMessage, sbn, notificationWear, fallbackReplyText);
        } else if ("Gemini".equals(provider)) {
            fetchGeminiReply(service, baseUrl, apiKey, model, systemPrompt, incomingMessage, sbn, notificationWear, fallbackReplyText);
        } else {
            // OpenAI, Grok, DeepSeek, Mistral, Custom
            fetchOpenAiCompatibleReply(service, apiKey, model, systemPrompt, incomingMessage, sbn, notificationWear, fallbackReplyText);
        }
    }

    private void fetchAtomaticAiReply(StatusBarNotification sbn, NotificationWear notificationWear, String incomingMessage, String fallbackReplyText) {
        // Call the method without retry flag (first attempt)
        fetchAtomaticAiReplyInternal(sbn, notificationWear, incomingMessage, fallbackReplyText, false);
    }

    private void fetchAtomaticAiReplyInternal(StatusBarNotification sbn, NotificationWear notificationWear, String incomingMessage, String defaultReply, boolean isRetryAfterTokenRefresh) {
        PreferencesManager prefs = PreferencesManager.getPreferencesInstance(this);

        String fallbackReply;
        if(!TextUtils.isEmpty(prefs.getFallbackMessage())){
            fallbackReply = prefs.getFallbackMessage();
        } else {
            fallbackReply = defaultReply;
        }
        // Get Firebase ID token
        String firebaseToken = prefs.getFirebaseToken();

        if (firebaseToken == null || firebaseToken.trim().isEmpty()) {
            Log.e(TAG, "Firebase token not available, falling back to default reply");
            sendActualReply(sbn, notificationWear, fallbackReply);
            return;
        }

        // Create the request
        AtomaticAIRequest request = new AtomaticAIRequest(incomingMessage, prefs.getAtomaticAICustomPrompt());

        // Create the service
        AtomaticAIService service = RetrofitInstance.getAtomaticAIRetrofitInstance()
                .create(AtomaticAIService.class);

        // Make the API call
        String authHeader = "Bearer " + firebaseToken;
        Log.d(TAG, "Atomatic AI API call - isRetry: " + isRetryAfterTokenRefresh);

        service.getAIReply(authHeader, "application/json", request).enqueue(new Callback<AtomaticAIResponse>() {
            @Override
            public void onResponse(@NonNull Call<AtomaticAIResponse> call, @NonNull Response<AtomaticAIResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AtomaticAIResponse aiResponse = response.body();
                    String reply = aiResponse.getReply();
                    int remainingAtoms = aiResponse.getRemainingAtoms();

                    if (reply != null && !reply.trim().isEmpty()) {
                        Log.i(TAG, "Atomatic AI successful response. Remaining atoms: " + remainingAtoms);
                        prefs.setRemainingAtoms(remainingAtoms);
                        checkAndNotifyQuotaExhausted(remainingAtoms);
                        sendActualReply(sbn, notificationWear, reply);
                    } else {
                        Log.e(TAG, "Atomatic AI returned empty reply, using fallback");
                        sendActualReply(sbn, notificationWear, fallbackReply);
                    }
                } else {
                    // Check if this is an authentication error (401 or 403)
                    boolean isAuthError = response.code() == 401 || response.code() == 403;

                    // Try to parse error response
                    if (!isAuthError && response.errorBody() != null) {
                        AtomaticAIErrorResponse errorResponse = RetrofitInstance.parseAtomaticAIError(response);
                        if (errorResponse != null) {
                            isAuthError = errorResponse.isAuthError();
                            Log.e(TAG, "Atomatic AI error: " + errorResponse.getMessage());
                        }
                    }

                    // If it's an auth error and we haven't retried yet, refresh token and retry
                    if (isAuthError && !isRetryAfterTokenRefresh) {
                        Log.w(TAG, "Atomatic AI authentication failed (code: " + response.code() + "). Attempting token refresh...");
                        handleTokenExpirationAndRetry(sbn, notificationWear, incomingMessage, fallbackReply);
                    } else {
                        // Either not an auth error, or already retried - use fallback
                        if (isRetryAfterTokenRefresh) {
                            Log.e(TAG, "Atomatic AI still failed after token refresh, using fallback");
                        } else {
                            Log.e(TAG, "Atomatic AI API failed: " + response.code() + " " + response.message());
                        }
                        sendActualReply(sbn, notificationWear, fallbackReply);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AtomaticAIResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Atomatic AI API network error", t);
                sendActualReply(sbn, notificationWear, defaultReply);
            }
        });
    }

    private void handleTokenExpirationAndRetry(StatusBarNotification sbn, NotificationWear notificationWear, String incomingMessage, String fallbackReplyText) {
        Log.i(TAG, "Handling token expiration - refreshing Firebase token...");

        // Refresh token asynchronously to avoid blocking the main thread
        FirebaseTokenRefresher.refreshTokenAsync(this, new FirebaseTokenRefresher.TokenRefreshCallback() {
            @Override
            public void onSuccess(String newToken) {
                Log.i(TAG, "Token refresh successful. Retrying Atomatic AI request...");
                // Retry the request with the new token (pass true to indicate this is a retry)
                fetchAtomaticAiReplyInternal(sbn, notificationWear, incomingMessage, fallbackReplyText, true);
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Token refresh failed: " + error + ". Using fallback reply.");
                sendActualReply(sbn, notificationWear, fallbackReplyText);
            }
        });
    }

    private void fetchClaudeReply(OpenAIService service, String baseUrl, String apiKey, String model, String systemPrompt, String incomingMessage, StatusBarNotification sbn, NotificationWear notificationWear, String fallbackReplyText) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("max_tokens", 1024);
        requestBody.addProperty("system", systemPrompt);

        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", incomingMessage);
        messages.add(userMessage);
        requestBody.add("messages", messages);

        Map<String, String> headers = new HashMap<>();
        headers.put("x-api-key", apiKey);
        headers.put("anthropic-version", "2023-06-01");
        headers.put("content-type", "application/json");

        String url = baseUrl + "v1/messages";

        service.getClaudeCompletion(url, headers, requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray content = response.body().getAsJsonArray("content");
                        if (content != null && content.size() > 0) {
                            String reply = content.get(0).getAsJsonObject().get("text").getAsString();
                            sendActualReply(sbn, notificationWear, reply);
                            return;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing Claude response", e);
                    }
                }
                Log.e(TAG, "Claude API failed: " + response.code() + " " + response.message());
                sendActualReply(sbn, notificationWear, fallbackReplyText);
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "Claude API network error", t);
                sendActualReply(sbn, notificationWear, fallbackReplyText);
            }
        });
    }

    private void fetchGeminiReply(OpenAIService service, String baseUrl, String apiKey, String model, String systemPrompt, String incomingMessage, StatusBarNotification sbn, NotificationWear notificationWear, String fallbackReplyText) {
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject contentObj = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        // Combine system prompt and user message for simplicity
        part.addProperty("text", systemPrompt + "\n\nUser: " + incomingMessage);
        parts.add(part);
        contentObj.add("parts", parts);
        contents.add(contentObj);
        requestBody.add("contents", contents);

        String url = baseUrl + "v1beta/models/" + model + ":generateContent";

        service.getGeminiCompletion(url, apiKey, requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray candidates = response.body().getAsJsonArray("candidates");
                        if (candidates != null && candidates.size() > 0) {
                            JsonObject candidate = candidates.get(0).getAsJsonObject();
                            JsonObject content = candidate.getAsJsonObject("content");
                            JsonArray parts = content.getAsJsonArray("parts");
                            if (parts != null && parts.size() > 0) {
                                String reply = parts.get(0).getAsJsonObject().get("text").getAsString();
                                sendActualReply(sbn, notificationWear, reply);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing Gemini response", e);
                    }
                }
                Log.e(TAG, "Gemini API failed: " + response.code() + " " + response.message());
                sendActualReply(sbn, notificationWear, fallbackReplyText);
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "Gemini API network error", t);
                sendActualReply(sbn, notificationWear, fallbackReplyText);
            }
        });
    }

    private void fetchOpenAiCompatibleReply(OpenAIService service, String apiKey, String model, String systemPrompt, String incomingMessage, StatusBarNotification sbn, NotificationWear notificationWear, String fallbackReplyText) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.add(new Message("user", incomingMessage));

        OpenAIRequest request = new OpenAIRequest(model, messages);
        String bearerToken = "Bearer " + apiKey;

        service.getChatCompletion(bearerToken, request).enqueue(new Callback<OpenAIResponse>() {
            @Override
            public void onResponse(@NonNull Call<OpenAIResponse> call, @NonNull Response<OpenAIResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                    response.body().getChoices() != null && !response.body().getChoices().isEmpty() &&
                    response.body().getChoices().get(0).getMessage() != null &&
                    response.body().getChoices().get(0).getMessage().getContent() != null) {

                    String aiReply = response.body().getChoices().get(0).getMessage().getContent().trim();
                    Log.i(TAG, "OpenAI/Compatible successful response: " + aiReply);
                    sendActualReply(sbn, notificationWear, aiReply);
                } else {
                    Log.e(TAG, "OpenAI/Compatible API failed: " + response.code() + " " + response.message());
                    // Fallback to default reply
                    sendActualReply(sbn, notificationWear, fallbackReplyText);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OpenAIResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "OpenAI/Compatible API network error", t);
                sendActualReply(sbn, notificationWear, fallbackReplyText);
            }
        });
    }

    private boolean canPurgeMessages() {
        //Added L to avoid numeric overflow expression
        //https://stackoverflow.com/questions/43801874/numeric-overflow-in-expression-manipulating-timestamps
        long daysBeforePurgeInMS = 30 * 24 * 60 * 60 * 1000L;
        return (System.currentTimeMillis() - PreferencesManager.getPreferencesInstance(this).getLastPurgedTime()) > daysBeforePurgeInMS;
    }

    private boolean isSupportedPackage(StatusBarNotification sbn) {
        return PreferencesManager.getPreferencesInstance(this)
                .getEnabledApps()
                .contains(sbn.getPackageName());
    }

    private boolean canSendReplyNow(StatusBarNotification sbn) {
        // Do not reply to consecutive notifications from same person/group that arrive in below time
        // This helps to prevent infinite loops when users on both end uses Watomatic or similar app
        int DELAY_BETWEEN_REPLY_IN_MILLISEC = 10 * 1000;

        String title = NotificationUtils.getTitle(sbn);
        String selfDisplayName = sbn.getNotification().extras.getString("android.selfDisplayName");
        if (title != null && title.equalsIgnoreCase(selfDisplayName)) { //to protect double reply in case where if notification is not dismissed and existing notification is updated with our reply
            return false;
        }
        if (dbUtils == null) {
            dbUtils = new DbUtils(getApplicationContext());
        }
        long timeDelay = PreferencesManager.getPreferencesInstance(this).getAutoReplyDelay();
        return (System.currentTimeMillis() - dbUtils.getLastRepliedTime(sbn.getPackageName(), title) >= max(timeDelay, DELAY_BETWEEN_REPLY_IN_MILLISEC));
    }

    private boolean isGroupMessageAndReplyAllowed(StatusBarNotification sbn) {
        String rawTitle = NotificationUtils.getTitleRaw(sbn);
        //android.text returning SpannableString
        SpannableString rawText = SpannableString.valueOf("" + sbn.getNotification().extras.get("android.text"));
        // Detect possible group image message by checking for colon and text starts with camera icon #181
        boolean isPossiblyAnImageGrpMsg = ((rawTitle != null) && (": ".contains(rawTitle) || "@ ".contains(rawTitle)))
                && ((rawText != null) && rawText.toString().contains("\uD83D\uDCF7"));
        if (!sbn.getNotification().extras.getBoolean("android.isGroupConversation")) {
            return !isPossiblyAnImageGrpMsg;
        } else {
            return PreferencesManager.getPreferencesInstance(this).isGroupReplyEnabled();
        }
    }

    private boolean isServiceEnabled() {
        return PreferencesManager.getPreferencesInstance(this).isServiceEnabled();
    }

    /**
     * Check if the user's quota is exhausted and show a notification if needed.
     * Rate-limited to at most once every 24 hours.
     */
    private void checkAndNotifyQuotaExhausted(int remainingAtoms) {
        Log.d("QuotaExhaustedChecker", "Remaining atoms: " + remainingAtoms);
        if (remainingAtoms > 0) return;

        PreferencesManager prefs = PreferencesManager.getPreferencesInstance(this);
        long lastShown = prefs.getQuotaNotificationLastShown();
        long now = System.currentTimeMillis();
        long TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L;

        if (lastShown == 0 || (now - lastShown) >= TWENTY_FOUR_HOURS_MS) {
            prefs.setQuotaNotificationLastShown(now);

            // Determine if user is on highest plan (pro)
            boolean isHighestPlan = isOnHighestPlan(prefs);
            String renewalDate = null;
            if (isHighestPlan) {
                long expiryTime = prefs.getSubscriptionExpiryTime();
                if (expiryTime > 0) {
                    renewalDate = new java.text.SimpleDateFormat(
                            "MMMM dd, yyyy", java.util.Locale.getDefault()
                    ).format(new java.util.Date(expiryTime));
                }
            }
            NotificationUtils.showQuotaExhaustedNotification(this, isHighestPlan, renewalDate);
        }
    }

    /**
     * Check if the user is on the highest subscription plan (pro).
     */
    private boolean isOnHighestPlan(PreferencesManager prefs) {
        String productId = prefs.getSubscriptionProductId();
        return productId != null && productId.toLowerCase().contains("pro");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "Listener disconnected! Requesting rebind...");
        ComponentName componentName = new ComponentName(this, NotificationService.class);
        requestRebind(componentName);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Toast.makeText(getApplicationContext(), "Listener connected!", Toast.LENGTH_SHORT).show();
    }

}
