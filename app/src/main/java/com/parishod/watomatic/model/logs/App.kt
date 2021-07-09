package com.parishod.watomatic.model.logs

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supported_apps")
data class App(
        @PrimaryKey(autoGenerate = true) val id:Int,
        @ColumnInfo(name = "app_name") val name:String,
        @ColumnInfo(name = "package_name") val packageName:String,
)