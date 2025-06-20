package com.parishod.watomatic.network.model.openai;

// Using google-gson annotations for field names if they differ from Java conventions
import com.google.gson.annotations.SerializedName;

public class ModelData {
    @SerializedName("id")
    private String id;

    @SerializedName("object")
    private String objectType; // "object" is a Java keyword, so renamed

    @SerializedName("created")
    private long created;

    @SerializedName("owned_by")
    private String ownedBy;

    // Add other fields if necessary, e.g., permission, root, parent

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getOwnedBy() {
        return ownedBy;
    }

    public void setOwnedBy(String ownedBy) {
        this.ownedBy = ownedBy;
    }
}
