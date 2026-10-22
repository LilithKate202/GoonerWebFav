package com.example.goonerwebfav

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "zones")
data class ZoneEntity(
    @PrimaryKey
    val name: String,
    val isDefault: Boolean = false,
    val isRandomStart: Boolean = false, // 是否为启动随机分区
    val createdAt: Long = System.currentTimeMillis()
)