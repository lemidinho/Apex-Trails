package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY dateTimestamp DESC")
    fun getAllWorkouts(): Flow<List<WorkoutLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutLog): Long

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    @Query("SELECT MAX(distanceKm) FROM workouts WHERE activityType = :activityType")
    suspend fun getMaxDistanceForActivity(activityType: String): Double?

    @Query("SELECT MAX(durationSeconds) FROM workouts WHERE activityType = :activityType")
    suspend fun getMaxDurationForActivity(activityType: String): Long?

    // Average speed is (distance / (duration / 3600.0)). Let's query based on high-speed run logs.
    @Query("SELECT * FROM workouts WHERE activityType = :activityType")
    suspend fun getWorkoutsForActivity(activityType: String): List<WorkoutLog>
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM custom_checklist_items ORDER BY id ASC")
    fun getChecklistItems(): Flow<List<CustomChecklistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CustomChecklistItem): Long

    @Update
    suspend fun updateItem(item: CustomChecklistItem)

    @Query("DELETE FROM custom_checklist_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM custom_checklist_items WHERE isDefault = 0")
    suspend fun clearUserCustomItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<CustomChecklistItem>)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile_settings WHERE id = 1 LIMIT 1")
    fun getProfileSettings(): Flow<UserProfileSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfileSettings(profile: UserProfileSettings)
}
