package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val repository = WorkoutRepository(db.workoutDao(), db.checklistDao(), db.userProfileDao())

    // All Workouts flow
    val workoutsState: StateFlow<List<WorkoutLog>> = repository.allWorkouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Equipment Checklist flow
    val checklistState: StateFlow<List<CustomChecklistItem>> = repository.checklistItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User Profile settings flow
    val profileState: StateFlow<UserProfileSettings> = repository.profileSettings
        .map { it ?: UserProfileSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileSettings())

    // UI Navigation/Tab State
    var currentTab by mutableStateOf("dashboard") // "dashboard", "history", "charts", "social"

    // ACTIVE TRACKING STATES
    var selectedActivityType by mutableStateOf("Running") // "Running", "Cycling"
    var isTracking by mutableStateOf(false)
    var elapsedSeconds by mutableStateOf(0L)
    var currentDistanceKm by mutableStateOf(0.0)
    var currentBpm by mutableStateOf(0)
    var isIdleAlertTriggered by mutableStateOf(false)
    var idleSecondsCount by mutableStateOf(0L)

    // Pre-start checkout popup
    var showPreChecklistDialog by mutableStateOf(false)
    var gearCheckError by mutableStateOf<String?>(null)

    // Route coordinates simulator for active recording
    var recordedPath = mutableStateOf<List<Pair<Double, Double>>>(emptyList())

    // Live Workout Photos capturing
    var takenPhotos = mutableStateOf<List<String>>(emptyList())
    var photoPromptActive by mutableStateOf(false)

    // Cloud syncing state
    var isSyncing by mutableStateOf(false)
    var showSyncSuccessAlert by mutableStateOf(false)

    // Motivational Quote System
    var currentQuote by mutableStateOf(
        Quote(
            "The body achieves what the mind believes.",
            "Napoleon Hill",
            "8:00 AM Prompt"
        )
    )
    val quotesList = listOf(
        Quote("It never gets easier, you just go faster.", "Greg LeMond", "8:00 AM Prompt"),
        Quote("Your mind is your only limit. Push past it.", "Apex Co", "8:00 AM Prompt"),
        Quote("Pain is temporary. Quitting lasts forever.", "Lance Armstrong", "8:00 AM Prompt"),
        Quote("The miracle isn't that I finished. The miracle is that I had the courage to start.", "John Bingham", "8:00 AM Prompt"),
        Quote("Clear your mind. Watch your feet. Focus on the ride.", "Summit", "8:00 AM Prompt"),
        Quote("Run when you can, walk if you must, crawl if you have to; just never give up.", "Dean Karnazes", "8:00 AM Prompt")
    )

    // Garmin / Watch Connection simulator
    var showWatchConnectDialog by mutableStateOf(false)

    // Support/Donation settings states
    var showDonationSuccessDialog by mutableStateOf(false)
    val lastDonatedAmount = mutableStateOf(0.0)

    // Custom Item Checklist text field
    var newGearItemName by mutableStateOf("")

    // WEATHER SYNC SYSTEM STATES
    var isWeatherSyncing by mutableStateOf(false)
    var weatherTemperature by mutableStateOf(19) // °C
    var weatherCondition by mutableStateOf("Clear Skies")
    var weatherIcon by mutableStateOf("☀️")
    var lastWeatherSyncTime by mutableStateOf<String?>(null)

    // MUSIC / MEDIA PLAYER STATE & LIBRARY
    var activeMusicService by mutableStateOf("Spotify") // "Spotify", "YouTube Music", "Deezer"
    var isMusicPlaying by mutableStateOf(false)
    var currentTrackTitle by mutableStateOf("Peak Endurance Pace")
    var currentTrackArtist by mutableStateOf("Apex Wave")
    var musicPlaybackProgress by mutableStateOf(0.35f) // 0.0 to 1.0
    var cadenceSyncEnabled by mutableStateOf(false)
    var selectSongDialogActive by mutableStateOf(false)

    val spotifyPlaylist = listOf(
        Pair("Peak Endurance Pace", "Apex Wave"),
        Pair("High Cadence Drive", "Lazer Runner"),
        Pair("Oxygen Breath Control", "Prana Flow"),
        Pair("Midnight Skyline Sprint", "Synth Horizon")
    )
    val youtubePlaylist = listOf(
        Pair("Heavy Metal Hills Climber", "Iron Torque"),
        Pair("Adrenaline Kick Mix", "Bass Boosters"),
        Pair("Lo-Fi Trail Cruise", "Chill Nature"),
        Pair("Uphill Power Blast", "Sonic Energy")
    )
    val deezerPlaylist = listOf(
        Pair("Deep Techno Flow", "Reverb Beats"),
        Pair("Interval Training Hype", "Club Cadence"),
        Pair("Sunset Slowdown Outro", "Acoustic Trail"),
        Pair("High Altitude Ascent", "Chill Climber")
    )

    fun getActivePlaylist(): List<Pair<String, String>> {
        return when (activeMusicService) {
            "Spotify" -> spotifyPlaylist
            "YouTube Music" -> youtubePlaylist
            else -> deezerPlaylist
        }
    }

    fun playNextTrack() {
        val playlist = getActivePlaylist()
        val currIdx = playlist.indexOfFirst { it.first == currentTrackTitle }
        val nextIdx = if (currIdx == -1) 0 else (currIdx + 1) % playlist.size
        val nextSong = playlist[nextIdx]
        currentTrackTitle = nextSong.first
        currentTrackArtist = nextSong.second
        musicPlaybackProgress = 0.0f
    }

    fun playPreviousTrack() {
        val playlist = getActivePlaylist()
        val currIdx = playlist.indexOfFirst { it.first == currentTrackTitle }
        val prevIdx = if (currIdx <= 0) playlist.size - 1 else currIdx - 1
        val nextSong = playlist[prevIdx]
        currentTrackTitle = nextSong.first
        currentTrackArtist = nextSong.second
        musicPlaybackProgress = 0.0f
    }

    fun selectTrack(title: String, artist: String) {
        currentTrackTitle = title
        currentTrackArtist = artist
        musicPlaybackProgress = 0.0f
        isMusicPlaying = true
    }

    fun switchMusicService(service: String) {
        activeMusicService = service
        val playlist = getActivePlaylist()
        currentTrackTitle = playlist[0].first
        currentTrackArtist = playlist[0].second
        musicPlaybackProgress = 0.0f
    }

    fun checkAndSyncCadenceBpm(bpm: Int) {
        if (!cadenceSyncEnabled) return
        val targetTitle = when {
            bpm <= 120 -> {
                when (activeMusicService) {
                    "Spotify" -> "Oxygen Breath Control"
                    "YouTube Music" -> "Lo-Fi Trail Cruise"
                    else -> "Sunset Slowdown Outro"
                }
            }
            bpm <= 145 -> {
                when (activeMusicService) {
                    "Spotify" -> "Peak Endurance Pace"
                    "YouTube Music" -> "Uphill Power Blast"
                    else -> "High Altitude Ascent"
                }
            }
            else -> {
                when (activeMusicService) {
                    "Spotify" -> "High Cadence Drive"
                    "YouTube Music" -> "Adrenaline Kick Mix"
                    else -> "Interval Training Hype"
                }
            }
        }
        
        if (currentTrackTitle != targetTitle) {
            val playlist = getActivePlaylist()
            val song = playlist.firstOrNull { it.first == targetTitle }
            if (song != null) {
                currentTrackTitle = song.first
                currentTrackArtist = song.second
                musicPlaybackProgress = 0.0f
            }
        }
    }

    private var trackingJob: Job? = null

    init {
        // Ensure defaults are always populated
        viewModelScope.launch {
            repository.ensureDefaultChecklistPopulated()
            // Randomly pick a quote of the day
            currentQuote = quotesList.random()
            // Initial weather fetch if enabled
            if (profileState.value.weatherSyncEnabled) {
                syncWithLocalWeatherApp()
            }
        }

        // Live mock music track timeline tick
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (isMusicPlaying) {
                    val nextProgress = musicPlaybackProgress + 0.04f
                    if (nextProgress >= 1.0f) {
                        playNextTrack()
                    } else {
                        musicPlaybackProgress = nextProgress
                    }
                }
            }
        }
    }

    // Connect smart wristband
    fun connectWearable(deviceName: String) {
        viewModelScope.launch {
            val profile = profileState.value
            repository.saveProfile(profile.copy(connectedDevice = deviceName))
            // Instantly sync heart rate tracking simulated states
            if (isTracking) {
                currentBpm = (120..170).random()
            }
        }
    }

    // Google Sign in simulation
    fun connectGoogleAccount(email: String, name: String) {
        viewModelScope.launch {
            val profile = profileState.value
            repository.saveProfile(
                profile.copy(
                    isGoogleConnected = true,
                    email = email,
                    userName = name
                )
            )
        }
    }

    fun disconnectGoogleAccount() {
        viewModelScope.launch {
            val profile = profileState.value
            repository.saveProfile(
                profile.copy(
                    isGoogleConnected = false,
                    email = "offline-user@gmail.com",
                    userName = "Local Athlete"
                )
            )
        }
    }

    // Trigger local cloud sync simulation
    fun syncDataWithCloud() {
        if (isSyncing) return
        viewModelScope.launch {
            isSyncing = true
            delay(1500) // beautiful simulated delay
            isSyncing = false
            showSyncSuccessAlert = true
            val profile = profileState.value
            repository.saveProfile(profile.copy(lastSyncTimestamp = System.currentTimeMillis()))
        }
    }

    // Checklist Management
    fun addNewGearItem() {
        if (newGearItemName.isBlank()) return
        viewModelScope.launch {
            repository.addChecklistItem(newGearItemName.trim())
            newGearItemName = ""
        }
    }

    fun toggleChecklistItem(item: CustomChecklistItem) {
        viewModelScope.launch {
            repository.toggleChecklistItem(item)
        }
    }

    fun deleteChecklistItem(id: Long) {
        viewModelScope.launch {
            repository.deleteChecklistItem(id)
        }
    }

    // Tracking Loop
    fun startWorkoutSequence() {
        // Uncheck all checklist items at start so user has to check them fresh
        viewModelScope.launch {
            repository.uncheckAllChecklist()
            showPreChecklistDialog = true
            gearCheckError = null
        }
    }

    fun verifyAndStartWorkout() {
        val allChecked = checklistState.value.all { it.isChecked }
        if (!allChecked) {
            gearCheckError = "You must carry and check all gear items before starting!"
            return
        }

        gearCheckError = null
        showPreChecklistDialog = false
        startTracking()
    }

    private fun startTracking() {
        isTracking = true
        elapsedSeconds = 0L
        currentDistanceKm = 0.0
        recordedPath.value = listOf(Pair(45.100, 15.200)) // Base starting point
        takenPhotos.value = emptyList()
        currentBpm = if (profileState.value.connectedDevice != "None") (110..150).random() else 85
        idleSecondsCount = 0L
        isIdleAlertTriggered = false

        trackingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive && isTracking) {
                delay(1000)
                withContext(Dispatchers.Main) {
                    elapsedSeconds++

                    // Simulate distance changes based on activity
                    val speedFactor = if (selectedActivityType == "Running") 0.003 else 0.007
                    currentDistanceKm += speedFactor + ((0..5).random() / 5000.0)

                    // Simulate subtle coordinates drifting
                    val lastPt = recordedPath.value.lastOrNull() ?: Pair(45.100, 15.200)
                    val nextPt = Pair(
                        lastPt.first + ((-10..10).random() * 0.0001),
                        lastPt.second + ((-11..11).random() * 0.00015)
                    )
                    recordedPath.value = recordedPath.value + nextPt

                    // Wearable BPM heart rate dynamics
                    val isWearableConnected = profileState.value.connectedDevice != "None"
                    if (isWearableConnected) {
                        // Fluctuates between active thresholds
                        val diff = (-4..5).random()
                        val newBpm = currentBpm + diff
                        currentBpm = newBpm.coerceIn(115, 178)
                    } else {
                        // General mobile sensor simulation
                        currentBpm = (90..120).random()
                    }

                    // Keep music pacing synced
                    checkAndSyncCadenceBpm(currentBpm)

                    // Photo prompt randomly every 150-200 seconds of activity to encourage looking around
                    if (elapsedSeconds > 0 && elapsedSeconds % 120 == 0L) {
                        photoPromptActive = true
                    }

                    // Passive idle tracking simulator
                    // If moving, we are active. For simulation, let's say every 40s there's an active idle alert demo option,
                    // or let's increment idle count if user explicitly triggers idle scenario or simply automatically
                    // alert after simulated "30-min threshold" (which we simulate after 60 seconds of idle to make it testable!)
                    if (isIdleAlertTriggered) {
                        idleSecondsCount++
                    }
                }
            }
        }
    }

    fun forceIdleScenario() {
        idleSecondsCount = 1800L // Simulate exact 30 minutes of sitting idle
        isIdleAlertTriggered = true
    }

    fun dismissIdleAlert() {
        isIdleAlertTriggered = false
        idleSecondsCount = 0L
    }

    fun stopAndSaveWorkout(notes: String) {
        isTracking = false
        trackingJob?.cancel()

        viewModelScope.launch {
            // Serialize path to coordinates
            val pathStr = recordedPath.value.joinToString(";") { "${it.first},${it.second}" }
            val photoStr = takenPhotos.value.joinToString(",")

            repository.saveWorkout(
                activityType = selectedActivityType,
                distanceKm = Math.round(currentDistanceKm * 100.0) / 100.0,
                durationSeconds = elapsedSeconds,
                notes = notes.trim(),
                photoUrls = photoStr,
                routeCoordinatesJson = pathStr,
                averageHeartRate = currentBpm
            )

            currentTab = "history" // jump to history after saving
        }
    }

    fun cancelActiveWorkout() {
        isTracking = false
        trackingJob?.cancel()
        elapsedSeconds = 0
        currentDistanceKm = 0.0
    }

    // Simulated beautiful camera snapshot triggers
    fun captureScenicPhoto() {
        val scenicPrompts = listOf(
            "🌅 Glorious sunrise over the park trail",
            "🌳 Golden sunbeams piercing through valley trees",
            "🏔️ Rocky path panoramic mountain lookout",
            "🐕 Smiling golden retriever met during interval pace",
            "🍂 Vibrant autumn leaves carpet on forest pathway",
            "🚲 Crisp blue sky shining over resting cockpit view"
        )
        val selectedPhotoText = scenicPrompts.random()
        takenPhotos.value = takenPhotos.value + selectedPhotoText
        photoPromptActive = false
    }

    fun removePhoto(photo: String) {
        takenPhotos.value = takenPhotos.value.filter { it != photo }
    }

    // Toggle theme simulation
    fun toggleMetricSystem() {
        viewModelScope.launch {
            val profile = profileState.value
            repository.saveProfile(profile.copy(metricSystem = !profile.metricSystem))
        }
    }

    fun toggleDarkTheme() {
        viewModelScope.launch {
            val profile = profileState.value
            repository.saveProfile(profile.copy(darkTheme = !profile.darkTheme))
        }
    }

    fun toggleWeatherSync() {
        viewModelScope.launch {
            val profile = profileState.value
            val isEnabledNow = !profile.weatherSyncEnabled
            repository.saveProfile(profile.copy(weatherSyncEnabled = isEnabledNow))
            if (isEnabledNow) {
                syncWithLocalWeatherApp()
            }
        }
    }

    fun syncWithLocalWeatherApp() {
        viewModelScope.launch {
            isWeatherSyncing = true
            delay(1200) // simulated local device weather synchronization delay
            val temps = listOf(16, 17, 19, 21, 22, 24)
            val conditionsList = listOf(
                Pair("Optimal Training Sun", "☀️"),
                Pair("Light Cooling Wind", "💨"),
                Pair("Nice Partly Cloudy", "⛅"),
                Pair("Mild Breezy Air", "🍃"),
                Pair("Dry Cool Shadows", "☁️")
            )
            val picked = conditionsList.random()
            weatherTemperature = temps.random()
            weatherCondition = picked.first
            weatherIcon = picked.second
            
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            lastWeatherSyncTime = sdf.format(Date())
            isWeatherSyncing = false
        }
    }

    // Support and donations
    fun recordDonation(amount: Double) {
        viewModelScope.launch {
            val profile = profileState.value
            val newTotal = profile.totalDonatedEuro + amount
            repository.saveProfile(profile.copy(totalDonatedEuro = newTotal))
            lastDonatedAmount.value = amount
            showDonationSuccessDialog = true
        }
    }
}

data class Quote(
    val message: String,
    val author: String,
    val deliveryTime: String
)
