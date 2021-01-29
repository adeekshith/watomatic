package com.parishod.wareply.model.whatsapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.parishod.wareply.model.utils.Constants;

@Database(entities = {WhatsappAutoReplyLogs.class}, version = 1)
public abstract class WhatsappAutoReplyLogsDB extends RoomDatabase {
    private static final String DB_NAME = Constants.DB_NAME;
    private static WhatsappAutoReplyLogsDB _instance;

    public static synchronized WhatsappAutoReplyLogsDB getInstance(Context context){
        if(_instance == null){
            _instance = Room.databaseBuilder(context.getApplicationContext(), WhatsappAutoReplyLogsDB.class, DB_NAME)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build();
        }
        return _instance;
    }

    public abstract WhatsappAutoReplyLogsDao logsDao();
}
