package com.parishod.watomatic.model.logs;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.parishod.watomatic.model.utils.Constants;

@Database(entities = {MessageLog.class, AppPackage.class, App.class}, version = 3)
public abstract class MessageLogsDB extends RoomDatabase {
    private static final String DB_NAME = Constants.LOGS_DB_NAME;
    private static MessageLogsDB _instance;

    public static synchronized MessageLogsDB getInstance(Context context) {
        if (_instance == null) {
            _instance = Room.databaseBuilder(context.getApplicationContext(), MessageLogsDB.class, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return _instance;
    }

    public abstract MessageLogsDao logsDao();

    public abstract AppPackageDao appPackageDao();

    public abstract SupportedAppsDao supportedAppsDao();
}
