package com.parishod.watomatic.network.model.openai;

import com.google.gson.annotations.SerializedName;

public class OpenAIErrorResponse {

    @SerializedName("error")
    private OpenAIErrorDetail error;

    // Getter
    public OpenAIErrorDetail getError() {
        return error;
    }

    // Optionally, a setter
}
