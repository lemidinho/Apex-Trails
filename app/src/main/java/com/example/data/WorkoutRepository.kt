package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val checklistDao: ChecklistDao,
    private val userProfileDao: UserProfileDao
) {
    val allWorkouts: Flow<List<WorkoutLog>> = workoutDao.getAllWorkouts()
    val checklistItems: Flow<List<CustomChecklistItem>> = checklistDao.getChecklistItems()
    val profileSettings: Flow<UserProfileSettings?> = userProfileDao.getProfileSettings()

    suspend fun saveWorkout(
        activityType: String,
        distanceKm: Double,
        durationSeconds: Long,
        notes: String,
        photoUrls: String,
        routeCoordinatesJson: String,
        averageHeartRate: Int
    ): WorkoutLog {
        // Query old workouts same type to verify personal bests
        val pastWorkouts = workoutDao.getWorkoutsForActivity(activityType)

        var isDistancePB = false
        var isDurationPB = false
        var isSpeedPB = false

        if (pastWorkouts.isEmpty()) {
            // First workout of this type => automatic PBs if non-zero
            if (distanceKm > 0.0) isDistancePB = true
            if (durationSeconds > 0L) isDurationPB = true
            isSpeedPB = true
        } else {
            val maxDistance = pastWorkouts.maxOfOrNull { it.distanceKm } ?: 0.0
            val maxDuration = pastWorkouts.maxOfOrNull { it.durationSeconds } ?: 0L
            
            val currentSpeed = if (durationSeconds > 0) distanceKm / (durationSeconds / 3600.0) else 0.0
            val maxSpeed = pastWorkouts.maxOfOrNull { 
                if (it.durationSeconds > 0) it.distanceKm / (it.durationSeconds / 3600.0) else 0.0 
            } ?: 0.0

            if (distanceKm > maxDistance) isDistancePB = true
            if (durationSeconds > maxDuration) isDurationPB = true
            if (currentSpeed > maxSpeed && currentSpeed > 0.0) isSpeedPB = true
        }

        // Calculate pace string: e.g. "5:30 min/km"
        val paceStr = if (distanceKm > 0.0) {
            val totalMinutes = (durationSeconds / 60.0)
            val minPart = totalMinutes.toInt()
            val secPart = ((totalMinutes - minPart) * 60).toInt()
            String.format("%d:%02d/km", minPart, secPart)
        } else {
            "--:--"
        }

        val log = WorkoutLog(
            activityType = activityType,
            distanceKm = distanceKm,
            durationSeconds = durationSeconds,
            paceMinPerKm = paceStr,
            dateTimestamp = System.currentTimeMillis(),
            notes = notes,
            photoUrls = photoUrls,
            averageHeartRate = averageHeartRate,
            isPersonalBestDistance = isDistancePB,
            isPersonalBestDuration = isDurationPB,
            isPersonalBestSpeed = isSpeedPB,
            routeCoordinatesJson = routeCoordinatesJson
        )

        val id = workoutDao.insertWorkout(log)
        return log.copy(id = id)
    }

    suspend fun deleteWorkout(id: Long) {
        workoutDao.deleteWorkout(id)
    }

    suspend fun addChecklistItem(name: String) {
        checklistDao.insertItem(CustomChecklistItem(name = name, isChecked = false, isDefault = false))
    }

    suspend fun toggleChecklistItem(item: CustomChecklistItem) {
        checklistDao.updateItem(item.copy(isChecked = !item.isChecked))
    }

    suspend fun uncheckAllChecklist() {
        // Collect current list and uncheck all
        val currentList = checklistItems.firstOrNull() ?: emptyList()
        currentList.forEach {
            checklistDao.updateItem(it.copy(isChecked = false))
        }
    }

    suspend fun deleteChecklistItem(id: Long) {
        checklistDao.deleteItem(id)
    }

    suspend fun saveProfile(profile: UserProfileSettings) {
        userProfileDao.saveProfileSettings(profile)
    }

    // High quality safety backup checker - if DB initially empty, injects values
    suspend fun ensureDefaultChecklistPopulated() {
        val current = checklistItems.firstOrNull() ?: emptyList()
        if (current.isEmpty()) {
            val defaults = listOf(
                CustomChecklistItem(name = "Water Bottle / Hydration", isDefault = true),
                CustomChecklistItem(name = "House Keys & Fully Charged Phone", isDefault = true),
                CustomChecklistItem(name = "Proper Training Shoes", isDefault = true),
                CustomChecklistItem(name = "Sun Glasses / Helmet / Protective Gear", isDefault = true),
                CustomChecklistItem(name = "Energy Gels / Snacks for long tours", isDefault = true)
            )
            checklistDao.insertItems(defaults)
        }
        val profile = profileSettings.firstOrNull()
        if (profile == null) {
            userProfileDao.saveProfileSettings(UserProfileSettings())
        }
    }
}
