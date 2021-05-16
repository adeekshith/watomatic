package com.parishod.watomatic.model.utils;

import android.content.Context;

import com.parishod.watomatic.model.logs.MessageLogsDB;

public class DbUtils {
    private Context mContext;

    public DbUtils(Context context){
        mContext = context;
    }

    public long getNunReplies(){
        MessageLogsDB messageLogsDB = MessageLogsDB.getInstance(mContext.getApplicationContext());
        return messageLogsDB.logsDao().getNumReplies();
    }

    public void purgeMessageLogs(){
        MessageLogsDB messageLogsDB = MessageLogsDB.getInstance(mContext.getApplicationContext());
        messageLogsDB.logsDao().purgeMessageLogs();
    }
}
