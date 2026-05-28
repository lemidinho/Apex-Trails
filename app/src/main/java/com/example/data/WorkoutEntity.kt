package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityType: String, // "Running" or "Cycling"
    val distanceKm: Double,
    val durationSeconds: Long,
    val paceMinPerKm: String,
    val dateTimestamp: Long,
    val notes: String = "",
    val photoUrls: String = "", // comma-separated simulated photo tags/names
    val averageHeartRate: Int = 0,
    val isPersonalBestDistance: Boolean = false,
    val isPersonalBestDuration: Boolean = false,
    val isPersonalBestSpeed: Boolean = false,
    val routeCoordinatesJson: String = "" // simple string route representation
)

@Entity(tableName = "custom_checklist_items")
data class CustomChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isChecked: Boolean = false,
    val isDefault: Boolean = false
)

@Entity(tableName = "user_profile_settings")
data class UserProfileSettings(
    @PrimaryKey val id: Int = 1, // single profile row
    val userName: String = "John Doe",
    val email: String = "john.doe@gmail.com",
    val isGoogleConnected: Boolean = false,
    val connectedDevice: String = "None", // e.g., "Apple Watch Ultra", "Garmin Forerunner", "None"
    val metricSystem: Boolean = true,
    val darkTheme: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val targetWeeklyKm: Double = 30.0,
    val totalDonatedEuro: Double = 0.0,
    val weatherSyncEnabled: Boolean = true
)
