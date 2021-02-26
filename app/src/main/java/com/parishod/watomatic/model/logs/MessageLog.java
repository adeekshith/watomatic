package com.parishod.watomatic.model.logs;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "message_logs",
        foreignKeys = {@ForeignKey(
                entity = AppPackage.class,
                parentColumns = "index",
                childColumns = "index",
                onDelete = ForeignKey.CASCADE
        )})
public class MessageLog {
    @NonNull
    private int index;
    @ColumnInfo(name = "notif_id")
    private String notifId;
    @ColumnInfo(name = "notif_title")
    private String notifTitle;
    @ColumnInfo(name = "notif_arrived_time")
    private long notifArrivedTime;
    @ColumnInfo(name = "notif_is_replied")
    private boolean notifIsReplied;
    @ColumnInfo(name = "notif_replied_msg")
    private String notifRepliedMsg;
    @ColumnInfo(name = "notif_reply_time")
    private long notifReplyTime;

    public MessageLog(int index,
                      String notif_title,
                      long notif_arrived_time,
                      String notif_replied_msg,
                      long notif_reply_time
        ) {
        this.index = index;
        this.notifTitle = notif_title;
        this.notifArrivedTime = notif_arrived_time;
        this.notifRepliedMsg = notif_replied_msg;
        this.notifReplyTime = notif_reply_time;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getNotifId() {
        return notifId;
    }

    public void setNotifId(String notifId) {
        this.notifId = notifId;
    }

    public String getNotifTitle() {
        return notifTitle;
    }

    public void setNotifTitle(String notifTitle) {
        this.notifTitle = notifTitle;
    }

    public long getNotifArrivedTime() {
        return notifArrivedTime;
    }

    public void setNotifArrivedTime(long notifArrivedTime) {
        this.notifArrivedTime = notifArrivedTime;
    }

    public boolean isNotifIsReplied() {
        return notifIsReplied;
    }

    public void setNotifIsReplied(boolean notifIsReplied) {
        this.notifIsReplied = notifIsReplied;
    }

    public String getNotifRepliedMsg() {
        return notifRepliedMsg;
    }

    public void setNotifRepliedMsg(String notifRepliedMsg) {
        this.notifRepliedMsg = notifRepliedMsg;
    }

    public long getNotifReplyTime() {
        return notifReplyTime;
    }

    public void setNotifReplyTime(long notifReplyTime) {
        this.notifReplyTime = notifReplyTime;
    }
}
