package com.parishod.watomatic.model.logs;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.parishod.watomatic.model.logs.whatsapp.WhatsappAutoReplyLogs;
import com.parishod.watomatic.model.logs.whatsapp.WhatsappAutoReplyLogsDao;
import com.parishod.watomatic.model.utils.Constants;

@Database(entities = {WhatsappAutoReplyLogs.class}, version = 1)
public abstract class AutoReplyLogsDB extends RoomDatabase {
    private static final String DB_NAME = Constants.LOGS_DB_NAME;
    private static AutoReplyLogsDB _instance;

    public static synchronized AutoReplyLogsDB getInstance(Context context){
        if(_instance == null){
            _instance = Room.databaseBuilder(context.getApplicationContext(), AutoReplyLogsDB.class, DB_NAME)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build();
        }
        return _instance;
    }

    public abstract WhatsappAutoReplyLogsDao logsDao();
}
