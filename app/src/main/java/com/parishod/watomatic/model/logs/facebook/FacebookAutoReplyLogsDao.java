package com.parishod.watomatic.model.logs.facebook;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface FacebookAutoReplyLogsDao {
    @Query("SELECT created_at FROM FacebookAutoReplyLogs WHERE user_id=:userId ORDER BY created_at DESC LIMIT 1")
    long getLastReplyTimeStamp(String userId);

    @Insert
    void logReply(FacebookAutoReplyLogs log);
}
