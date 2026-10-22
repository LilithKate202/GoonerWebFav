package com.example.goonerwebfav

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "websites")
data class WebsiteEntity(
    @PrimaryKey
    val url: String, // 网址作为全局唯一标识（满足需求：重复时跳过）

    val zoneName: String, // 属于哪个分区（移动分区时只改这个字段）

    val title: String = "",
    val favicon: String = "",
    val tags: String = "", // 标签，多个用逗号分隔
    val note: String = "", // 备注

    val visitCount: Int = 0, // 访问次数
    val lastVisit: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),

    val isBg: Boolean = false // 是否设为背景图
)