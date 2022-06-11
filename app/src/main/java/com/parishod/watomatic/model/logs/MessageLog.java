package com.parishod.watomatic.model.logs;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "message_logs",
        foreignKeys = {@ForeignKey(
                entity = AppPackage.class,
                parentColumns = "index",
                childColumns = "index",
                onDelete = ForeignKey.CASCADE
        )},
        indices = {
                @Index(value = "index")
        })
public class MessageLog {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int id;
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
    @ColumnInfo(name = "notif_event")
    private String notifEvent;

    public MessageLog(int index,
                      String notifTitle,
                      long notifArrivedTime,
                      String notifRepliedMsg,
                      long notifReplyTime,
                      boolean notifIsReplied,
                      String notifEvent
    ) {
        this.index = index;
        this.notifId = null;
        this.notifTitle = notifTitle;
        this.notifArrivedTime = notifArrivedTime;
        this.notifRepliedMsg = notifRepliedMsg;
        this.notifReplyTime = notifReplyTime;
        this.notifIsReplied = notifIsReplied;
        this.notifEvent = notifEvent;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getNotifEvent() {
        return notifEvent;
    }

    public void setNotifEvent(String notifEvent) {
        this.notifEvent = notifEvent;
    }
}
