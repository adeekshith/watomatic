package com.parishod.watomatic.network.model.atomatic;

import com.google.gson.annotations.SerializedName;

public class AtomaticAIRequest {
    @SerializedName("message")
    private String message;
    @SerializedName("custom_prompt")
    private String custom_prompt;

    public String getPrompt() {
        return custom_prompt;
    }

    public void setPrompt(String prompt) {
        this.custom_prompt = prompt;
    }

    public AtomaticAIRequest(String message, String prompt) {
        this.message = message;
        this.custom_prompt = prompt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

