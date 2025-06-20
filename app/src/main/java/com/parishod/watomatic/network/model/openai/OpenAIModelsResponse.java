package com.parishod.watomatic.network.model.openai;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class OpenAIModelsResponse {
    @SerializedName("data")
    private List<ModelData> data;

    @SerializedName("object")
    private String objectType; // "object" is a Java keyword

    // Getters and Setters
    public List<ModelData> getData() {
        return data;
    }

    public void setData(List<ModelData> data) {
        this.data = data;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }
}
