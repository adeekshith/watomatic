package com.parishod.watomatic.model.logs;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.parishod.watomatic.model.utils.Constants;

@Database(entities = {MessageLog.class, AppPackage.class}, version = 3)
public abstract class MessageLogsDB extends RoomDatabase {
    private static final String DB_NAME = Constants.LOGS_DB_NAME;
    private static MessageLogsDB _instance;

    public static synchronized MessageLogsDB getInstance(Context context) {
        if (_instance == null) {
            _instance = Room.databaseBuilder(context.getApplicationContext(), MessageLogsDB.class, DB_NAME)
                    .addMigrations(MIGRATION_1_3, MIGRATION_2_3)
                    .allowMainThreadQueries()
                    .build();
        }
        return _instance;
    }

    public abstract MessageLogsDao logsDao();

    public abstract AppPackageDao appPackageDao();

    static final Migration MIGRATION_1_3 = new Migration(1, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE message_logs "
                    + " ADD COLUMN notif_event TEXT");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE message_logs "
                    + " ADD COLUMN notif_event TEXT");
        }
    };

}
