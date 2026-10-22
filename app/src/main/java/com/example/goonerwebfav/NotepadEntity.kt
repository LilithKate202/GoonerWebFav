package com.example.goonerwebfav

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notepad")
data class NotepadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 词条标题（比如：你好、苹果、或者你自己定的名字）
    val title: String,

    // 语种内容，存为 JSON 字符串。格式例如：
    // [{"lang":"English","text":"Hello"},{"lang":"中文","text":"你好"}]
    val translations: String,

    // 最后修改时间
    val updatedAt: Long = System.currentTimeMillis()
)