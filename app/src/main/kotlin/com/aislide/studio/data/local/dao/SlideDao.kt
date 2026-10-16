package com.aislide.studio.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aislide.studio.data.local.entity.SlideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SlideDao {

    @Query("SELECT * FROM slides WHERE projectId = :projectId ORDER BY slideIndex ASC")
    fun observeByProject(projectId: Long): Flow<List<SlideEntity>>

    @Query("SELECT * FROM slides WHERE projectId = :projectId ORDER BY slideIndex ASC")
    suspend fun getByProject(projectId: Long): List<SlideEntity>

    @Query("SELECT * FROM slides WHERE projectId = :projectId AND slideIndex = :index LIMIT 1")
    suspend fun getByIndex(projectId: Long, index: Int): SlideEntity?

    @Query("SELECT * FROM slides WHERE id = :id")
    suspend fun getById(id: Long): SlideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(slide: SlideEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slides: List<SlideEntity>): List<Long>

    @Update
    suspend fun update(slide: SlideEntity)

    @Delete
    suspend fun delete(slide: SlideEntity)

    @Query("DELETE FROM slides WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM slides WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: Long)

    @Query("UPDATE slides SET slideIndex = :newIndex, updatedAt = :now WHERE id = :id")
    suspend fun updateIndex(id: Long, newIndex: Int, now: Long = System.currentTimeMillis())

    @Transaction
    suspend fun reorder(projectId: Long, orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            updateIndex(id, index)
        }
    }

    @Query("SELECT MAX(slideIndex) FROM slides WHERE projectId = :projectId")
    suspend fun maxIndex(projectId: Long): Int?
}
