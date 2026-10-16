package com.aislide.studio.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.aislide.studio.data.local.dao.ProjectDao
import com.aislide.studio.data.local.dao.SlideDao
import com.aislide.studio.data.local.entity.ProjectEntity
import com.aislide.studio.data.local.entity.SlideEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * App Room database. v1 schema.
 *
 * Rule of android skill (data.md): write migrations; never use
 * `fallbackToDestructiveMigration` in a shipped build. v1 has no migrations yet
 * — they will be added as the schema evolves.
 */
@Database(
    entities = [ProjectEntity::class, SlideEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(AppConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun slideDao(): SlideDao

    companion object {
        const val DATABASE_NAME = "ai_slide_studio.db"
    }
}

class AppConverters(private val json: Json = AppJson.default) {

    @TypeConverter
    fun fromMap(map: Map<String, String>): String =
        json.encodeToString(map)

    @TypeConverter
    fun toMap(value: String): Map<String, String> =
        if (value.isBlank()) emptyMap() else json.decodeFromString(value)
}

object AppJson {
    val default = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = false
    }
}
