package com.parishod.watomatic.network.model.atomatic;

import com.google.gson.annotations.SerializedName;

/**
 * Error response model for Atomatic AI API
 */
public class AtomaticAIErrorResponse {
    @SerializedName("error")
    private String error;

    @SerializedName("message")
    private String message;

    @SerializedName("statusCode")
    private int statusCode;

    public AtomaticAIErrorResponse() {
    }

    public AtomaticAIErrorResponse(String error, String message, int statusCode) {
        this.error = error;
        this.message = message;
        this.statusCode = statusCode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public boolean isAuthError() {
        return statusCode == 401 || statusCode == 403
            || (message != null && (message.toLowerCase().contains("token")
                && (message.toLowerCase().contains("expired")
                    || message.toLowerCase().contains("invalid"))));
    }
}

