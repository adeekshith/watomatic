package com.parishod.watomatic.model.logs;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_packages")
public class AppPackage {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int index;
    @ColumnInfo(name = "package_name")
    private String packageName;

    public AppPackage(String packageName) {
        this.packageName = packageName;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
