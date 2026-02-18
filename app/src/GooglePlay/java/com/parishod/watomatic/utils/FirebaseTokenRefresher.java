package com.parishod.watomatic.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.parishod.watomatic.model.preferences.PreferencesManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility class for refreshing Firebase authentication tokens
 * This class is specific to the GooglePlay flavor and uses Firebase Auth
 */
public class FirebaseTokenRefresher {
    private static final String TAG = "FirebaseTokenRefresher";
    private static final int TOKEN_REFRESH_TIMEOUT_SECONDS = 10;

    /**
     * Synchronously refresh the Firebase ID token and save it to preferences
     * This method blocks the calling thread, so it should be called from a background thread
     * or in a context where blocking is acceptable (like inside a Retrofit callback)
     *
     * @param context Application context
     * @return The refreshed token, or null if refresh failed
     */
    @Nullable
    public static String refreshTokenSync(@NonNull Context context) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Log.e(TAG, "Cannot refresh token: No user logged in");
            return null;
        }

        try {
            Log.d(TAG, "Forcing Firebase token refresh for user: " + user.getUid());

            // Force token refresh (true parameter forces refresh even if token is not expired)
            Task<GetTokenResult> task = user.getIdToken(true);

            // Wait for the task to complete (blocks current thread)
            GetTokenResult tokenResult = Tasks.await(task, TOKEN_REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            String newToken = tokenResult.getToken();

            if (newToken != null && !newToken.isEmpty()) {
                Log.i(TAG, "Firebase token refreshed successfully");

                // Save the new token to preferences
                PreferencesManager prefs = PreferencesManager.getPreferencesInstance(context);
                prefs.setFirebaseToken(newToken);

                return newToken;
            } else {
                Log.e(TAG, "Token refresh returned null or empty token");
                return null;
            }

        } catch (ExecutionException e) {
            Log.e(TAG, "Token refresh failed with execution exception", e);
            return null;
        } catch (InterruptedException e) {
            Log.e(TAG, "Token refresh was interrupted", e);
            Thread.currentThread().interrupt(); // Restore interrupted status
            return null;
        } catch (TimeoutException e) {
            Log.e(TAG, "Token refresh timed out after " + TOKEN_REFRESH_TIMEOUT_SECONDS + " seconds", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during token refresh", e);
            return null;
        }
    }

    /**
     * Asynchronously refresh the Firebase ID token and save it to preferences
     *
     * @param context Application context
     * @param callback Callback to receive the result
     */
    public static void refreshTokenAsync(@NonNull Context context, @NonNull TokenRefreshCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Log.e(TAG, "Cannot refresh token: No user logged in");
            callback.onFailure("No user logged in");
            return;
        }

        Log.d(TAG, "Asynchronously forcing Firebase token refresh for user: " + user.getUid());

        user.getIdToken(true)
            .addOnSuccessListener(tokenResult -> {
                String newToken = tokenResult.getToken();

                if (newToken != null && !newToken.isEmpty()) {
                    Log.i(TAG, "Firebase token refreshed successfully (async)");

                    // Save the new token to preferences
                    PreferencesManager prefs = PreferencesManager.getPreferencesInstance(context);
                    prefs.setFirebaseToken(newToken);

                    callback.onSuccess(newToken);
                } else {
                    Log.e(TAG, "Token refresh returned null or empty token (async)");
                    callback.onFailure("Token refresh returned null");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Token refresh failed (async)", e);
                callback.onFailure(e.getMessage());
            });
    }

    /**
     * Callback interface for async token refresh
     */
    public interface TokenRefreshCallback {
        void onSuccess(String newToken);
        void onFailure(String error);
    }
}

