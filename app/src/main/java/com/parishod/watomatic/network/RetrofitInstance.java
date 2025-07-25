package com.parishod.watomatic.network;

import com.parishod.watomatic.BuildConfig;
import com.parishod.watomatic.network.model.openai.OpenAIErrorResponse;

import java.io.IOException;
import java.lang.annotation.Annotation;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
// retrofit2.Response will be used with FQN

public class RetrofitInstance {
    private static Retrofit retrofit;
    private static Retrofit openAIRetrofit;
    private static final String BASE_URL = "https://api.github.com";
    public static final String OPENAI_BASE_URL = "https://api.openai.com/";

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            if (BuildConfig.DEBUG) {
                // set your desired log level
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            }
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor) // <-- this is the important line!
                    .build();

            retrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build();
        }
        return retrofit;
    }

    public static Retrofit getOpenAIRetrofitInstance(String baseUrl) {
        return createOpenAIRetrofitInstance(baseUrl);
    }

    public static Retrofit getOpenAIRetrofitInstance() {
        if (openAIRetrofit == null) {
            openAIRetrofit = createOpenAIRetrofitInstance(OPENAI_BASE_URL);
        }
        return openAIRetrofit;
    }

    private static Retrofit createOpenAIRetrofitInstance(String baseUrl) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        // Check if BuildConfig is accessible, otherwise, remove this DEBUG check for simplicity in this subtask
        // if (BuildConfig.DEBUG) {
        //    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        // } else {
        //    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        // }
        // For now, let's set a default or assume DEBUG is available.
        // If subtask fails due to BuildConfig, will adjust.
         if (BuildConfig.DEBUG) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
         } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
         }

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create()) // Only Gson for OpenAI
                .client(okHttpClient)
                .build();
    }

    public static OpenAIErrorResponse parseOpenAIError(retrofit2.Response<?> response) {
        if (response.errorBody() == null) {
            return null;
        }

        // Ensure you use the Retrofit instance configured for OpenAI
        // (the one that knows about OpenAIErrorResponse and GsonConverterFactory)
        Retrofit currentOpenAIRetrofit = getOpenAIRetrofitInstance();
        if (currentOpenAIRetrofit == null) {
            // This shouldn't happen if getOpenAIRetrofitInstance is called correctly before this
            // but as a safeguard:
            android.util.Log.e("RetrofitInstance", "OpenAI Retrofit instance is null, cannot parse error.");
            return null;
        }

        Converter<ResponseBody, OpenAIErrorResponse> converter =
                currentOpenAIRetrofit.responseBodyConverter(OpenAIErrorResponse.class, new Annotation[0]);

        try {
            return converter.convert(response.errorBody());
        } catch (IOException e) {
            // Log this error, as it means parsing failed
            android.util.Log.e("RetrofitInstance", "IOException parsing OpenAI error response", e);
            return null;
        }
    }
}
