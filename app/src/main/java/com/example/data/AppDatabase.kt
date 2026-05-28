package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [WorkoutLog::class, CustomChecklistItem::class, UserProfileSettings::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "apex_track_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDefaultChecklist(database.checklistDao())
                    populateDefaultSettings(database.userProfileDao())
                }
            }
        }

        private suspend fun populateDefaultChecklist(checklistDao: ChecklistDao) {
            val defaults = listOf(
                CustomChecklistItem(name = "Water Bottle / Hydration", isDefault = true),
                CustomChecklistItem(name = "House Keys & Fully Charged Phone", isDefault = true),
                CustomChecklistItem(name = "Proper Training Shoes", isDefault = true),
                CustomChecklistItem(name = "Sun Glasses / Helmet / Protective Gear", isDefault = true),
                CustomChecklistItem(name = "Energy Gels / Snacks for long tours", isDefault = true)
            )
            checklistDao.insertItems(defaults)
        }

        private suspend fun populateDefaultSettings(userProfileDao: UserProfileDao) {
            userProfileDao.saveProfileSettings(UserProfileSettings())
        }
    }
}
