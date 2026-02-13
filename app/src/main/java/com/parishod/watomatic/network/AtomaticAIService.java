package com.parishod.watomatic.network;

import com.parishod.watomatic.network.model.atomatic.AtomaticAIRequest;
import com.parishod.watomatic.network.model.atomatic.AtomaticAIResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AtomaticAIService {
    @POST("/")
    Call<AtomaticAIResponse> getAIReply(
        @Header("Authorization") String authorization,
        @Header("Content-Type") String contentType,
        @Body AtomaticAIRequest requestBody
    );
}

