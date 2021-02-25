package com.parishod.watomatic.model.logs.whatsapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "WhatsappAutoReplyLogs")
public class WhatsappAutoReplyLogs {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int id;
    @ColumnInfo(name = "user_id")
    private String userId;
    @ColumnInfo(name = "created_at")
    private long createdAt;
    @ColumnInfo(name = "is_replied")
    private boolean isReplied;
    @ColumnInfo(name = "reply_message", defaultValue = "'NULL'")
    @Nullable
    private String replyMessage;

    public boolean isReplied() {
        return isReplied;
    }

    public void setReplied(boolean replied) {
        isReplied = replied;
    }

    public WhatsappAutoReplyLogs(String userId, long createdAt) {
        this.userId = userId;
        this.createdAt = createdAt;
        this.isReplied = true; //Setting by default to true
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
