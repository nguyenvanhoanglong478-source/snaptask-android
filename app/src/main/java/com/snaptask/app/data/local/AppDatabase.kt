package com.snaptask.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TaskEntity::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "snaptask.db"
                )
                    // Guide: When incrementing version (>= 4), define Migration (e.g. MIGRATION_3_4)
                    // and pass via .addMigrations(MIGRATION_3_4) to preserve user data.
                    .fallbackToDestructiveMigrationFrom(1, 2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
