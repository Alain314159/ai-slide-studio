package com.aislide.studio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aislide.studio.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeById(id: Long): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE projects SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET slideCount = :count, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSlideCount(id: Long, count: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun count(): Int
}
