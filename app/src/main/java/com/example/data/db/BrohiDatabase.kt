package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AppSettingsEntity::class, ShortRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BrohiDatabase : RoomDatabase() {
    abstract fun brohiDao(): BrohiDao

    companion object {
        @Volatile
        private var INSTANCE: BrohiDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Reserved for future schema upgrades without data loss.
            }
        }

        fun getInstance(context: Context): BrohiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BrohiDatabase::class.java,
                    "brohi_clipping_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
