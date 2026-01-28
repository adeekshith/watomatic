package com.parishod.watomatic.service;

import static com.parishod.watomatic.model.utils.Constants.DEFAULT_LLM_MODEL;
import static com.parishod.watomatic.model.utils.Constants.DEFAULT_LLM_PROMPT;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.text.TextUtils; // Added import
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
import com.parishod.watomatic.network.OpenAIService;
import com.parishod.watomatic.network.RetrofitInstance; // Ensure this is available
import com.parishod.watomatic.network.model.openai.Message;
import com.parishod.watomatic.network.model.openai.OpenAIErrorResponse; // Added import
import com.parishod.watomatic.network.model.openai.OpenAIRequest;
import com.parishod.watomatic.network.model.openai.OpenAIResponse;
import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.model.utils.ContactsHelper;
import com.parishod.watomatic.model.utils.DbUtils;
import com.parishod.watomatic.model.utils.NotificationHelper;
import com.parishod.watomatic.model.utils.NotificationUtils;
import com.parishod.watomatic.service.ReplyService;

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

        if(finalRemoteIn == null ) return;
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
        if(preferencesManager.isOpenAIRepliesEnabled()){
            replyText = getString(R.string.auto_reply_default_message);
        }
        String fallbackReplyText = replyText; // needs to be final to access in innere class hence one more variable

        CharSequence incomingMessageChars = sbn.getNotification().extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
        String incomingMessage = (incomingMessageChars != null) ? incomingMessageChars.toString() : null;

        if (preferencesManager.isOpenAIRepliesEnabled() &&
            preferencesManager.isSubscriptionActive() &&
            incomingMessage != null && !incomingMessage.trim().isEmpty() &&
            preferencesManager.getOpenAIApiKey() != null && !preferencesManager.getOpenAIApiKey().trim().isEmpty()) {

            Log.d(TAG, "AI conditions met. Attempting to get AI reply.");
            fetchAiReply(sbn, notificationWear, incomingMessage, fallbackReplyText);
        } else {
            Log.d(TAG, "AI conditions not met. Using default reply.");
            sendActualReply(sbn, notificationWear, fallbackReplyText);
        }
    }

    private void fetchAiReply(StatusBarNotification sbn, NotificationWear notificationWear, String incomingMessage, String fallbackReplyText) {
        PreferencesManager prefs = PreferencesManager.getPreferencesInstance(this);
        String provider = prefs.getOpenApiSource();
        if (provider == null) provider = "OpenAI";

        String apiKey = prefs.getOpenAIApiKey();
        String model = prefs.getSelectedOpenAIModel();
        String systemPrompt = prefs.getOpenAICustomPrompt();
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) systemPrompt = DEFAULT_LLM_PROMPT;
        if (model == null || model.isEmpty()) model = DEFAULT_LLM_MODEL;

        String baseUrl = Constants.INSTANCE.getPROVIDER_URLS().get(provider);
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
