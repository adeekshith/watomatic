package com.parishod.watomatic.model.logs

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supported_apps")
data class App(
        @ColumnInfo(name = "app_name") val name:String,
        @PrimaryKey @ColumnInfo(name = "package_name") val packageName:String,
)