package com.aislide.studio.di

import android.content.Context
import androidx.room.Room
import com.aislide.studio.data.local.database.AppDatabase
import com.aislide.studio.data.local.dao.ProjectDao
import com.aislide.studio.data.local.dao.SlideDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigrationOnDowngrade()  // only on downgrade, never on upgrade
        .build()

    @Provides
    fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()

    @Provides
    fun provideSlideDao(db: AppDatabase): SlideDao = db.slideDao()
}
