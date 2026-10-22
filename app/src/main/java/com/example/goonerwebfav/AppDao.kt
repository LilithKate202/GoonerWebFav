package com.example.goonerwebfav

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ================== 分区表操作 ==================
    @Query("SELECT * FROM zones ORDER BY createdAt ASC")
    fun getAllZonesFlow(): Flow<List<ZoneEntity>>

    @Query("SELECT * FROM zones ORDER BY createdAt ASC")
    suspend fun getAllZones(): List<ZoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: ZoneEntity)

    @Query("DELETE FROM zones WHERE name = :zoneName")
    suspend fun deleteZone(zoneName: String)

    @Query("UPDATE zones SET isRandomStart = 0")
    suspend fun clearAllRandomStart()

    @Query("UPDATE zones SET isRandomStart = 1 WHERE name = :zoneName")
    suspend fun setRandomStart(zoneName: String)


    // ================== 收藏表操作 ==================
    // 获取某个分区的所有收藏
    @Query("SELECT * FROM websites WHERE zoneName = :zoneName ORDER BY createdAt DESC")
    suspend fun getWebsitesInZone(zoneName: String): List<WebsiteEntity>

    // 插入收藏（遇到重复网址直接忽略，满足“已存在”跳过需求）
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWebsite(website: WebsiteEntity): Long

    // 更新收藏（编辑、访问次数+1等）
    @Update
    suspend fun updateWebsite(website: WebsiteEntity)

    // 删除收藏
    @Delete
    suspend fun deleteWebsite(website: WebsiteEntity)

    // 获取总数
    @Query("SELECT COUNT(*) FROM websites")
    suspend fun getTotalCount(): Int

    // 更新网址所在分区（用于批量移动或单条移动）
    @Query("UPDATE websites SET zoneName = :newZone WHERE url = :url")
    suspend fun moveWebsiteToZone(url: String, newZone: String)

    // 获取全部分区的网址（用于导出/统计）
    @Query("SELECT * FROM websites")
    suspend fun getAllWebsites(): List<WebsiteEntity>


    // ================== 记事本操作 ==================
    @Query("SELECT * FROM notepad ORDER BY updatedAt DESC")
    fun getAllNotepadFlow(): Flow<List<NotepadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotepad(notepad: NotepadEntity): Long

    @Update
    suspend fun updateNotepad(notepad: NotepadEntity)

    @Delete
    suspend fun deleteNotepad(notepad: NotepadEntity)

    // 搜索记事本（标题或内容包含关键词）
    @Query("SELECT * FROM notepad WHERE title LIKE '%' || :keyword || '%' OR translations LIKE '%' || :keyword || '%'")
    suspend fun searchNotepad(keyword: String): List<NotepadEntity>
}