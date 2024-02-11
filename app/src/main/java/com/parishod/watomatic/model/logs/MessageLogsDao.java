package com.parishod.watomatic.model.logs;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageLogsDao {
    @Query("SELECT message_logs.notif_reply_time FROM MESSAGE_LOGS " +
            "INNER JOIN app_packages ON app_packages.`index` = message_logs.`index` " +
            "WHERE app_packages.package_name=:packageName AND message_logs.notif_title=:title ORDER BY notif_reply_time DESC LIMIT 1"
    )
    long getLastReplyTimeStamp(String title, String packageName);

    @Insert
    void logReply(MessageLog log);

    @Query("SELECT COUNT(id) FROM MESSAGE_LOGS")
    long getNumReplies();

    //https://stackoverflow.com/questions/11771580/deleting-android-sqlite-rows-older-than-x-days
    @Query("DELETE FROM message_logs WHERE notif_reply_time <= strftime('%s', datetime('now', '-30 days'));")
    void purgeMessageLogs();

    @Query("SELECT notif_reply_time FROM MESSAGE_LOGS ORDER BY notif_reply_time DESC LIMIT 1")
    long getFirstRepliedTime();

    @Query("SELECT * FROM MESSAGE_LOGS")
    List<MessageLog> getAppLogs();
}
