package com.parishod.watomatic.model.utils;

import android.content.Context;
import android.service.notification.StatusBarNotification;

import com.parishod.watomatic.model.CustomRepliesData;
import com.parishod.watomatic.model.logs.AppPackage;
import com.parishod.watomatic.model.logs.MessageLog;
import com.parishod.watomatic.model.logs.MessageLogsDB;

import java.util.List;

public class DbUtils {
    private final Context mContext;

    public DbUtils(Context context) {
        mContext = context;
    }

    public long getNunReplies() {
        MessageLogsDB messageLogsDB = MessageLogsDB.getInstance(mContext.getApplicationContext());
        return messageLogsDB.logsDao().getNumReplies();
    }

    public void purgeMessageLogs() {
        MessageLogsDB messageLogsDB = MessageLogsDB.getInstance(mContext.getApplicationContext());
        messageLogsDB.logsDao().purgeMessageLogs();
    }

    public void logReply(StatusBarNotification sbn, String title, boolean isReplied, String event) {
        CustomRepliesData customRepliesData = CustomRepliesData.getInstance(mContext);
        MessageLogsDB messageLogsDB = MessageLogsDB.getInstance(mContext.getApplicationContext());
        int packageIndex = messageLogsDB.appPackageDao().getPackageIndex(sbn.getPackageName());
        if (packageIndex <= 0) {
            AppPackage appPackage = new AppPackage(sbn.getPackageName());
            messageLogsDB.appPackageDao().insertAppPackage(appPackage);
            packageIndex = messageLogsDB.appPackageDao().getPackageIndex(sbn.getPackageName());
        }
        MessageLog logs = new MessageLog(packageIndex, title, sbn.getNotification().when, customRepliesData.getTextToSendOrElse(),
                System.currentTimeMillis(), isReplied, event);
        messageLogsDB.logsDao().logReply(logs);
    }

    public long getLastRepliedTime(String packageName, String title) {
        MessageLogsDB messageLogsDB = MessageLogsDB.getInstance(mContext.getApplicationContext());
        return messageLogsDB.logsDao().getLastReplyTimeStamp(title, packageName);
    }

    public long getFirstRepliedTime() {
        MessageLogsDB messageLogsDB = MessageLogsDB.getInstance(mContext.getApplicationContext());
        return messageLogsDB.logsDao().getFirstRepliedTime();
    }

    public List<MessageLog> getAppLogs(){
        MessageLogsDB messageLogsDB = MessageLogsDB.getInstance(mContext.getApplicationContext());
        return messageLogsDB.logsDao().getAppLogs();
    }

    public String getPackageName(int index){
        MessageLogsDB messageLogsDB = MessageLogsDB.getInstance(mContext.getApplicationContext());
        return messageLogsDB.appPackageDao().getPackageName(index);
    }
}
