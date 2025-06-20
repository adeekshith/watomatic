package com.parishod.watomatic.model.utils;

import android.content.Context;
import android.util.Log;

import com.parishod.watomatic.model.preferences.PreferencesManager;
import com.parishod.watomatic.network.OpenAIService;
import com.parishod.watomatic.network.RetrofitInstance;
import com.parishod.watomatic.network.model.openai.ModelData;
import com.parishod.watomatic.network.model.openai.OpenAIModelsResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OpenAIHelper {

    private static final String TAG = "OpenAIHelper";
    private static List<ModelData> cachedModels;
    private static long lastModelsFetchTimeMillis;
    // Cache duration: 1 hour for now, can be adjusted
    private static final long CACHE_DURATION_MS = 60 * 60 * 1000;

    public interface FetchModelsCallback {
        void onModelsFetched(List<ModelData> models);
        void onError(String errorMessage);
    }

    public static synchronized void fetchModels(Context context, final FetchModelsCallback callback) {
        if (isCacheValid()) {
            Log.d(TAG, "Returning cached models.");
            if (callback != null) {
                callback.onModelsFetched(cachedModels);
            }
            return;
        }

        Log.d(TAG, "Fetching models from API.");
        PreferencesManager prefs = PreferencesManager.getPreferencesInstance(context.getApplicationContext());
        String apiKey = prefs.getOpenAIApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "OpenAI API Key not set.");
            if (callback != null) {
                callback.onError("OpenAI API Key not set. Please set it in General Settings.");
            }
            return;
        }

        OpenAIService service = RetrofitInstance.getOpenAIRetrofitInstance().create(OpenAIService.class);
        Call<OpenAIModelsResponse> call = service.getModels("Bearer " + apiKey);

        call.enqueue(new Callback<OpenAIModelsResponse>() {
            @Override
            public void onResponse(Call<OpenAIModelsResponse> call, Response<OpenAIModelsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    cachedModels = response.body().getData();
                    lastModelsFetchTimeMillis = System.currentTimeMillis();
                    Log.i(TAG, "Models fetched successfully. Count: " + cachedModels.size());
                    if (callback != null) {
                        callback.onModelsFetched(cachedModels);
                    }
                } else {
                    String errorMsg = "Failed to fetch models. Code: " + response.code() + ", Message: " + response.message();
                    Log.e(TAG, errorMsg);
                    if (callback != null) {
                        callback.onError(errorMsg);
                    }
                }
            }

            @Override
            public void onFailure(Call<OpenAIModelsResponse> call, Throwable t) {
                String errorMsg = "Failed to fetch models: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                if (callback != null) {
                    callback.onError(errorMsg);
                }
            }
        });
    }

    public static synchronized List<ModelData> getCachedModels() {
        return cachedModels;
    }

    public static synchronized boolean isCacheValid() {
        return cachedModels != null && !cachedModels.isEmpty() && (System.currentTimeMillis() - lastModelsFetchTimeMillis < CACHE_DURATION_MS);
    }

    public static synchronized void invalidateCache() {
        cachedModels = null;
        lastModelsFetchTimeMillis = 0;
        Log.d(TAG, "OpenAI models cache invalidated.");
    }
}
