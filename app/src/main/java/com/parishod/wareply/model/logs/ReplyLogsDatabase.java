package com.parishod.wareply.model.logs;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.parishod.wareply.model.utils.Constants;

@Database(entities = {Logs.class}, version = 1)
public abstract class ReplyLogsDatabase extends RoomDatabase {
    private static final String DB_NAME = Constants.DB_NAME;
    private static ReplyLogsDatabase _instance;

    public static synchronized ReplyLogsDatabase getInstance(Context context){
        if(_instance == null){
            _instance = Room.databaseBuilder(context.getApplicationContext(), ReplyLogsDatabase.class, DB_NAME)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build();
        }
        return _instance;
    }

    public abstract LogsDao logsDao();
}
