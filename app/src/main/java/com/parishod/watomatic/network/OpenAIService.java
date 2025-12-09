package com.parishod.watomatic.network;

import com.parishod.watomatic.network.model.openai.OpenAIRequest;
import com.parishod.watomatic.network.model.openai.OpenAIResponse;
import com.parishod.watomatic.network.model.openai.OpenAIModelsResponse; // Added import

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET; // Added import
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

public interface OpenAIService {
    @POST("v1/chat/completions")
    Call<OpenAIResponse> getChatCompletion(
        @Header("Authorization") String authorization,
        @Body OpenAIRequest requestBody
    );

    @GET("v1/models")
    Call<OpenAIModelsResponse> getModels(@Header("Authorization") String authorization);

    @GET("v1/models")
    Call<OpenAIModelsResponse> getModels(@HeaderMap java.util.Map<String, String> headers);
}
