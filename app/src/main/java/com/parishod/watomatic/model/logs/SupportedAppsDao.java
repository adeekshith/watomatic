package com.parishod.watomatic.model.logs;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SupportedAppsDao {
    @Query("SELECT * FROM supported_apps")
    List<App> getSupportedApps();
    @Insert
    void insertSupportedApp(App app);
    @Query("SELECT * FROM supported_apps WHERE package_name=:packageName")
    App getAppData(String packageName);
    @Delete
    void removeSupportedApp(App app);
}
