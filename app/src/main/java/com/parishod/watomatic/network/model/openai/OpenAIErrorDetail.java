package com.parishod.watomatic.network.model.openai;

import com.google.gson.annotations.SerializedName;

public class OpenAIErrorDetail {

    @SerializedName("message")
    private String message;

    @SerializedName("type")
    private String type;

    @SerializedName("param")
    private String param; // Can be null

    @SerializedName("code")
    private String code; // Can be null

    // Getters
    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getParam() {
        return param;
    }

    public String getCode() {
        return code;
    }

    // Optionally, setters if needed, though typically not for response parsing
}
