package com.parishod.watomatic.network.model.atomatic;

import com.google.gson.annotations.SerializedName;

public class AtomaticAIRequest {
    @SerializedName("message")
    private String message;

    public AtomaticAIRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

