package com.parishod.watomatic.model.logs;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface MessageLogsDao {
    @Query("SELECT notif_reply_time FROM message_logs WHERE notif_title=:title ORDER BY notif_reply_time DESC LIMIT 1")
    long getLastReplyTimeStamp(String title);

    @Insert
    void logReply(MessageLog log);
}
