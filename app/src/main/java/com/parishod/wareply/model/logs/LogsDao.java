package com.parishod.wareply.model.logs;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface LogsDao {
    @Query("SELECT created_at FROM log WHERE user_id=:userId ORDER BY created_at DESC LIMIT 1")
    long getLastReplyTimeStamp(String userId);

    @Insert
    void logReply(Logs log);
}
