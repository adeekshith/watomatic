package com.parishod.watomatic.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Stub implementation for Default flavor (no Firebase Auth)
 * This class provides the same API but doesn't actually perform token refresh
 */
public class FirebaseTokenRefresher {
    private static final String TAG = "FirebaseTokenRefresher";

    /**
     * Stub method - returns null for Default flavor
     *
     * @param context Application context
     * @return null (Firebase Auth not available in Default flavor)
     */
    @Nullable
    public static String refreshTokenSync(@NonNull Context context) {
        Log.w(TAG, "FirebaseTokenRefresher not available in Default flavor");
        return null;
    }

    /**
     * Stub method - calls onFailure for Default flavor
     *
     * @param context Application context
     * @param callback Callback to receive the result
     */
    public static void refreshTokenAsync(@NonNull Context context, @NonNull TokenRefreshCallback callback) {
        Log.w(TAG, "FirebaseTokenRefresher not available in Default flavor");
        callback.onFailure("Firebase Auth not available in Default flavor");
    }

    /**
     * Callback interface for async token refresh
     */
    public interface TokenRefreshCallback {
        void onSuccess(String newToken);
        void onFailure(String error);
    }
}

