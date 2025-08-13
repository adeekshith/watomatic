package com.parishod.watomatic;

import static com.parishod.watomatic.model.utils.Constants.DEFAULT_LLM_MODEL;
import static com.parishod.watomatic.model.utils.Constants.DEFAULT_LLM_PROMPT;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.text.TextUtils; // Added import
import android.util.Log;
// import Constants.kt


import androidx.annotation.NonNull;
import androidx.core.app.RemoteInput;

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

        RemoteInput[] remoteInputs = new RemoteInput[notificationWear.getRemoteInputs().size()];

        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle localBundle = new Bundle(); // notificationWear.bundle;
        int i = 0;
        for (RemoteInput remoteIn : notificationWear.getRemoteInputs()) {
            remoteInputs[i] = remoteIn;
            localBundle.putCharSequence(remoteInputs[i].getResultKey(), replyText);
            i++;
        }

        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle);
        try {
            if (notificationWear.getPendingIntent() != null) {
                if (dbUtils == null) {
                    dbUtils = new DbUtils(getApplicationContext());
                }
                dbUtils.logReply(sbn, NotificationUtils.getTitle(sbn));
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
        String fallbackReplyText = customRepliesData.getTextToSendOrElse();

        CharSequence incomingMessageChars = sbn.getNotification().extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
        String incomingMessage = (incomingMessageChars != null) ? incomingMessageChars.toString() : null;

        if (preferencesManager.isOpenAIRepliesEnabled() &&
            incomingMessage != null && !incomingMessage.trim().isEmpty() &&
            preferencesManager.getOpenAIApiKey() != null && !preferencesManager.getOpenAIApiKey().trim().isEmpty()) {

            Log.d(TAG, "OpenAI conditions met. Attempting to get AI reply.");
            OpenAIService openAIService = RetrofitInstance.getOpenAIRetrofitInstance().create(OpenAIService.class);

            List<Message> messages = new ArrayList<>();
            String customPrompt = preferencesManager.getOpenAICustomPrompt();
            if (customPrompt == null || customPrompt.trim().isEmpty()) {
                customPrompt = DEFAULT_LLM_PROMPT;
            }

            messages.add(new Message("system", customPrompt));
            messages.add(new Message("user", incomingMessage));

            String modelForRequest = preferencesManager.getSelectedOpenAIModel();
            if (TextUtils.isEmpty(modelForRequest)) { // Safety fallback
                modelForRequest = DEFAULT_LLM_MODEL;
                Log.w(TAG, "Selected OpenAI model was empty, defaulting to gpt-3.5-turbo.");
            }

            OpenAIRequest request = new OpenAIRequest(modelForRequest, messages);
            String bearerToken = "Bearer " + preferencesManager.getOpenAIApiKey();
            final String originalModelId = modelForRequest; // Capture for logging in case of retry

            openAIService.getChatCompletion(bearerToken, request).enqueue(new Callback<OpenAIResponse>() {
                @Override
                public void onResponse(@NonNull Call<OpenAIResponse> call, @NonNull Response<OpenAIResponse> response) {
                    if (response.isSuccessful() && response.body() != null &&
                        response.body().getChoices() != null && !response.body().getChoices().isEmpty() &&
                        response.body().getChoices().get(0).getMessage() != null &&
                        response.body().getChoices().get(0).getMessage().getContent() != null) {

                        String aiReply = response.body().getChoices().get(0).getMessage().getContent().trim();
                        Log.i(TAG, "OpenAI successful response with model " + originalModelId + ": " + aiReply);
                        sendActualReply(sbn, notificationWear, aiReply);
                    } else {
                        // Enhanced error parsing for initial call
                        OpenAIErrorResponse parsedError = RetrofitInstance.parseOpenAIError(response);
                        String openAIErrorMessage = (parsedError != null && parsedError.getError() != null && parsedError.getError().getMessage() != null) ? parsedError.getError().getMessage() : "No specific OpenAI error message.";
                        String detailedApiError = "Original API call failed with model " + originalModelId + ". Code: " + response.code() + ". Message: " + response.message() + ". OpenAI: " + openAIErrorMessage;
                        Log.e(TAG, detailedApiError);
                        // No longer need to log generic errorBody here as parseOpenAIError would have tried.

                        boolean shouldRetry = false;
                        String specificErrorCode = (parsedError != null && parsedError.getError() != null) ? parsedError.getError().getCode() : null;
                        String specificErrorType = (parsedError != null && parsedError.getError() != null) ? parsedError.getError().getType() : null;

                        if (specificErrorCode != null && specificErrorCode.equals("insufficient_quota")) {
                            String userFacingErrorMessage = "OpenAI: Insufficient quota. Please check your plan and billing details.";
                            preferencesManager.saveOpenAILastPersistentError(userFacingErrorMessage, System.currentTimeMillis());
                            Log.e(TAG, userFacingErrorMessage + " (Model: " + originalModelId + ")");
                            shouldRetry = false;
                        } else if (response.code() == 401) { // Unauthorized - likely API key issue
                            String userFacingErrorMessage = "OpenAI: Invalid API Key. Please check your API Key in settings.";
                            preferencesManager.saveOpenAILastPersistentError(userFacingErrorMessage, System.currentTimeMillis());
                            Log.e(TAG, userFacingErrorMessage);
                            shouldRetry = false;
                        } else if (response.code() == 400 || response.code() == 404 || (specificErrorCode != null && specificErrorCode.equals("model_not_found")) || (specificErrorType != null && specificErrorType.equals("invalid_request_error"))) {
                            // For model_not_found or general invalid_request_error that might be model related, we attempt retry.
                            // No persistent error saved here as retry might fix it for the user temporarily.
                            // If retry also fails for similar reasons, then we might save a persistent error.
                            Log.w(TAG, "Suspected invalid model (" + originalModelId + ") or bad request. Attempting retry with default model. Details: " + detailedApiError);
                            shouldRetry = true;
                        }
                        // Add more conditions for specificErrorCode if needed for other non-retryable errors

                        if (shouldRetry) {
                            // Log.w(TAG, "Attempting fallback to default model gpt-3.5-turbo due to error with model: " + originalModelId + ". Details: " + detailedApiError); // Already logged above with more detail
                            List<Message> retryMessages = new ArrayList<>();
                            retryMessages.add(new Message("system",  DEFAULT_LLM_PROMPT));
                            retryMessages.add(new Message("user", incomingMessage)); // Ensure incomingMessage is accessible

                            OpenAIRequest retryRequest = new OpenAIRequest("gpt-3.5-turbo", retryMessages);
                            // Bearer token is the same
                            openAIService.getChatCompletion(bearerToken, retryRequest).enqueue(new Callback<OpenAIResponse>() {
                                @Override
                                public void onResponse(@NonNull Call<OpenAIResponse> retryCall, @NonNull Response<OpenAIResponse> retryResponse) {
                                    if (retryResponse.isSuccessful() && retryResponse.body() != null &&
                                        retryResponse.body().getChoices() != null && !retryResponse.body().getChoices().isEmpty() &&
                                        retryResponse.body().getChoices().get(0).getMessage() != null &&
                                        retryResponse.body().getChoices().get(0).getMessage().getContent() != null) {

                                        String retryAiReply = retryResponse.body().getChoices().get(0).getMessage().getContent().trim();
                                        Log.i(TAG, "OpenAI successful response with fallback model gpt-3.5-turbo: " + retryAiReply);
                                        sendActualReply(sbn, notificationWear, retryAiReply);
                                    } else {
                                        OpenAIErrorResponse parsedRetryError = RetrofitInstance.parseOpenAIError(retryResponse);
                                        String retryOpenAIErrorMessage = (parsedRetryError != null && parsedRetryError.getError() != null && parsedRetryError.getError().getMessage() != null) ? parsedRetryError.getError().getMessage() : "No specific OpenAI error message on retry.";
                                        String detailedRetryApiError = "OpenAI fallback request (default model) also failed. Code: " + retryResponse.code() + ". Message: " + retryResponse.message() + ". OpenAI: " + retryOpenAIErrorMessage;
                                        Log.e(TAG, detailedRetryApiError);

                                        String retrySpecificErrorCode = (parsedRetryError != null && parsedRetryError.getError() != null) ? parsedRetryError.getError().getCode() : null;
                                        if (retrySpecificErrorCode != null && retrySpecificErrorCode.equals("insufficient_quota")) {
                                            String userFacingErrorMessage = "OpenAI: Insufficient quota (even for default model). Please check your plan and billing details.";
                                            preferencesManager.saveOpenAILastPersistentError(userFacingErrorMessage, System.currentTimeMillis());
                                            Log.e(TAG, userFacingErrorMessage);
                                        }
                                        // Potentially save other persistent errors from retry if needed

                                        sendActualReply(sbn, notificationWear, fallbackReplyText);
                                    }
                                }

                                @Override
                                public void onFailure(@NonNull Call<OpenAIResponse> retryCall, @NonNull Throwable t) {
                                    Log.e(TAG, "OpenAI fallback API call (network) failed for default model.", t);
                                    // Could save a generic network error for OpenAI if it happens consistently.
                                    // preferencesManager.saveOpenAILastPersistentError("OpenAI: Network error reaching API. Check connection.", System.currentTimeMillis());
                                    sendActualReply(sbn, notificationWear, fallbackReplyText);
                                }
                            });
                        } else {
                            // For errors not leading to a retry (e.g., quota, API key), ensure persistent error was saved if applicable.
                            sendActualReply(sbn, notificationWear, fallbackReplyText);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<OpenAIResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "OpenAI API call failed", t);
                    sendActualReply(sbn, notificationWear, fallbackReplyText);
                }
            });
            return; // Return after initiating async call
        } else {
            Log.d(TAG, "OpenAI conditions not met. Using default reply.");
            // Log reasons if needed:
            // if (!preferencesManager.isOpenAIRepliesEnabled()) Log.d(TAG, "OpenAI disabled.");
            // if (incomingMessage == null || incomingMessage.trim().isEmpty()) Log.d(TAG, "Incoming message is null or empty.");
            // if (preferencesManager.getOpenAIApiKey() == null || preferencesManager.getOpenAIApiKey().trim().isEmpty()) Log.d(TAG, "OpenAI API key is missing.");
            sendActualReply(sbn, notificationWear, fallbackReplyText);
        }
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
}
