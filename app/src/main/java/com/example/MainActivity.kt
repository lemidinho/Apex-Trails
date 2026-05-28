package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CustomChecklistItem
import com.example.data.UserProfileSettings
import com.example.data.WorkoutLog
import com.example.ui.theme.*
import com.example.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: WorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val profile by viewModel.profileState.collectAsStateWithLifecycle()
            var showSettingsMenu by remember { mutableStateOf(false) }
            MyApplicationTheme(darkTheme = profile.darkTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavBar(
                            currentTab = viewModel.currentTab,
                            onTabSelected = { viewModel.currentTab = it }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        AppNavigation(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                            onOpenSettings = { showSettingsMenu = true }
                        )
                        
                        // Passive battery idle alert popup
                        if (viewModel.isIdleAlertTriggered) {
                            IdleBatteryAlertOverlay(
                                onDismiss = { viewModel.dismissIdleAlert() },
                                onStopSession = {
                                    viewModel.stopAndSaveWorkout("Stopped automatically due to 30 mins idling.")
                                    viewModel.dismissIdleAlert()
                                }
                            )
                        }

                        // Select song dialog popup
                        if (viewModel.selectSongDialogActive) {
                            SelectSongDialog(
                                viewModel = viewModel,
                                onDismiss = { viewModel.selectSongDialogActive = false }
                            )
                        }

                        // Donation success dialog popup
                        if (viewModel.showDonationSuccessDialog) {
                            DonationSuccessDialog(
                                amount = viewModel.lastDonatedAmount.value,
                                onDismiss = { viewModel.showDonationSuccessDialog = false }
                            )
                        }

                        // Athlete Portal & Settings Menu Popup Dialog
                        if (showSettingsMenu) {
                            AthletePortalSettingsDialog(
                                viewModel = viewModel,
                                profile = profile,
                                onDismiss = { showSettingsMenu = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit
) {
    val workouts by viewModel.workoutsState.collectAsStateWithLifecycle()
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val checklist by viewModel.checklistState.collectAsStateWithLifecycle()

    when (viewModel.currentTab) {
        "dashboard" -> DashboardScreen(
            viewModel = viewModel,
            profile = profile,
            checklist = checklist,
            modifier = modifier,
            onOpenSettings = onOpenSettings
        )
        "history" -> {
            val scope = rememberCoroutineScope()
            HistoryScreen(
                workouts = workouts,
                profile = profile,
                onDeleteWorkout = { id -> scope.launch { viewModel.repository.deleteWorkout(id) } },
                modifier = modifier
            )
        }
        "charts" -> WeeklyChartsScreen(
            workouts = workouts,
            profile = profile,
            modifier = modifier
        )
        "social" -> SocialLeaderboardScreen(
            viewModel = viewModel,
            profile = profile,
            modifier = modifier
        )
    }
}

// NAVIGATION BAR
@Composable
fun BottomNavBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0F0F0F),
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentTab == "dashboard",
            onClick = { onTabSelected("dashboard") },
            icon = { Icon(Icons.Default.DirectionsRun, contentDescription = "Dashboard") },
            label = { Text("Record", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = NeonOrangePrimary,
                indicatorColor = NeonOrangePrimary,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("tab_dashboard")
        )
        NavigationBarItem(
            selected = currentTab == "history",
            onClick = { onTabSelected("history") },
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text("Logbook", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = NeonOrangePrimary,
                indicatorColor = NeonOrangePrimary,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("tab_history")
        )
        NavigationBarItem(
            selected = currentTab == "charts",
            onClick = { onTabSelected("charts") },
            icon = { Icon(Icons.Default.ShowChart, contentDescription = "Progress") },
            label = { Text("Charts", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = NeonOrangePrimary,
                indicatorColor = NeonOrangePrimary,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("tab_charts")
        )
        NavigationBarItem(
            selected = currentTab == "social",
            onClick = { onTabSelected("social") },
            icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Social") },
            label = { Text("Social", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = NeonOrangePrimary,
                indicatorColor = NeonOrangePrimary,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("tab_social")
        )
    }
}

// SCREEN 1: DASHBOARD & ACTIVE TRACKER
@Composable
fun DashboardScreen(
    viewModel: WorkoutViewModel,
    profile: UserProfileSettings,
    checklist: List<CustomChecklistItem>,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var notesInput by remember { mutableStateOf("") }
    var showStopDialog by remember { mutableStateOf(false) }

    var donationAmountInput by remember { mutableStateOf("15") }
    var selectedDonationPreset by remember { mutableStateOf("15") }
    var showDonationOverlay by remember { mutableStateOf(false) }
    var donationStatusStep by remember { mutableStateOf(0) } // 0 = Connecting, 1 = Processing, 2 = Success
    var simulatedReceiptId by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Status Segment in Vibrant Palette Theme
        item {
            Spacer(modifier = Modifier.height(20.dp))
            
            // SPECTACULAR MOUNTAIN TRAIL BRAND LOGO HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MountainTrailLogoIcon(modifier = Modifier.size(48.dp))
                Column {
                    Text(
                        text = "APEX TRAIL",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFF00E676), Color(0xFFFF6D00))
                            )
                        )
                    )
                    Text(
                        text = "VIBRANT OFF-ROAD TRACKING",
                        color = Color(0xFF94A3B8), // Slate-400
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Circle Avatar with custom modern layout colors
                    val avatarBorder = Brush.sweepGradient(
                        colors = listOf(Color(0xFF00E5FF), Color(0xFF00E676), Color(0xFFFF6D00), Color(0xFF00E5FF))
                    )
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .border(BorderStroke(2.dp, avatarBorder), CircleShape)
                            .clickable { onOpenSettings() }
                            .testTag("profile_avatar_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (profile.userName.length >= 2) profile.userName.take(2).uppercase() else "AR",
                            color = Color(0xFF00E5FF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column {
                        Text(
                            text = "Welcome back,",
                            color = Color(0xFF94A3B8), // slate-400
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile.userName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (profile.totalDonatedEuro > 0.0) {
                                val (badgeText, badgeColor) = when {
                                    profile.totalDonatedEuro >= 100.0 -> "👑 Legend" to Color(0xFFFFD700)
                                    profile.totalDonatedEuro >= 50.0 -> "🥇 Gold" to Color(0xFFFFD700)
                                    profile.totalDonatedEuro >= 20.0 -> "🥈 Silver" to Color(0xFFCFD8DC)
                                    else -> "🥉 Bronze" to Color(0xFFD7CCC8)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(badgeColor.copy(alpha = 0.15f))
                                        .border(0.5.dp, badgeColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        color = badgeColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // Small Wearable & Notif & Settings icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Watch button",
                        tint = if (profile.connectedDevice != "None") Color(0xFF00E5FF) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(24.dp).testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "App settings",
                            tint = NeonOrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                }
            }
        }

        // Active Offline Sync Sync Status Line card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(SyncedGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CLOUD SYNCED",
                    color = Color(0xFF64748B), // slate-500
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // 🌦️ WEATHER INTEGRATION COMPONENT
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (profile.weatherSyncEnabled) DarkBorderAccent else Color.Gray.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().testTag("weather_sync_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (profile.weatherSyncEnabled) viewModel.weatherIcon else "⚠️",
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Column {
                                Text(
                                    text = if (profile.weatherSyncEnabled) "TRAINING WEATHER" else "WEATHER APP COLD",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (profile.weatherSyncEnabled) "Synced with Local Weather App" else "Weather synchronization is disabled",
                                    color = Color.Gray,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        if (profile.weatherSyncEnabled) {
                            if (viewModel.isWeatherSyncing) {
                                CircularProgressIndicator(
                                    color = NeonOrangePrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                IconButton(
                                    onClick = { viewModel.syncWithLocalWeatherApp() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync locations weather info data",
                                        tint = NeonOrangePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.currentTab = "social" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("ACTIVATE", color = NeonOrangePrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (profile.weatherSyncEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSlateBackground)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = viewModel.weatherCondition.uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (viewModel.lastWeatherSyncTime != null) "Updated: ${viewModel.lastWeatherSyncTime}" else "Waiting first sync...",
                                    color = Color.Gray,
                                    fontSize = 8.sp
                                )
                            }

                            val displayTemp = if (profile.metricSystem) {
                                "${viewModel.weatherTemperature}°C"
                            } else {
                                "${(viewModel.weatherTemperature * 9/5 + 32)}°F"
                            }
                            
                            Text(
                                text = displayTemp,
                                color = NeonOrangePrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // Quote of the day at 8:00 AM Prompt panel in Vibrant style
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "“",
                        color = NeonOrangePrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.height(28.dp)
                    )
                    Column {
                        Text(
                            text = viewModel.currentQuote.message,
                            color = Color(0xFFCBD5E1), // slate-300
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Daily Push • 08:00 AM",
                            color = Color(0xFF64748B), // slate-500
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // ACTIVITY & WORKOUT TRACKER STATUS OR MAIN VIEW
        if (!viewModel.isTracking) {
            // STEP 1: SELECT ACTIVITY TYPE & START
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CharcoalCard)
                        .border(1.dp, DarkBorderAccent, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "READY FOR YOUR NEXT TRAINING?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Segmented Button Toggle for Activity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(DarkSlateBackground)
                            .border(1.dp, DarkBorderAccent, RoundedCornerShape(26.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (viewModel.selectedActivityType == "Running") NeonOrangePrimary else Color.Transparent)
                                .clickable { viewModel.selectedActivityType = "Running" }
                                .testTag("activity_type_run"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsRun,
                                    contentDescription = "RunIcon",
                                    tint = if (viewModel.selectedActivityType == "Running") Color.Black else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "RUNNING",
                                    color = if (viewModel.selectedActivityType == "Running") Color.Black else Color.Gray,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (viewModel.selectedActivityType == "Cycling") NeonOrangePrimary else Color.Transparent)
                                .clickable { viewModel.selectedActivityType = "Cycling" }
                                .testTag("activity_type_cycle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBike,
                                    contentDescription = "BikeIcon",
                                    tint = if (viewModel.selectedActivityType == "Cycling") Color.Black else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "CYCLING",
                                    color = if (viewModel.selectedActivityType == "Cycling") Color.Black else Color.Gray,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Large Start button (triggers equipment popup checklist first)
                    Button(
                        onClick = { viewModel.startWorkoutSequence() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonOrangePrimary),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_session_button")
                    ) {
                        Text(
                            text = "START TRAINING",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // PERSISTENT COMPANION AUDIO DECK FOR SESSIONS
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎵 AUDIO SESSION COMPANION",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("INTEGRATED", color = Color(0xFF00E5FF), fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(
                        text = "Set up your workout music flow on Spotify, Deezer or YouTube Music before taking off",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    MusicPlayerControlPanel(viewModel = viewModel)
                }
            }

            // CHECKLIST MANAGEMENT FROM HOME NOTE WINDOW
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CharcoalCard)
                        .border(1.dp, DarkBorderAccent, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "🎒 SPORTS GEAR CHECKLIST",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize list of stuff to check before leaving home",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Add Custom Checklist item text field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = viewModel.newGearItemName,
                            onValueChange = { viewModel.newGearItemName = it },
                            placeholder = { Text("e.g. Helmet, Water Flask...", color = Color.DarkGray, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonOrangePrimary,
                                unfocusedBorderColor = DarkBorderAccent,
                                focusedContainerColor = DarkSlateBackground,
                                unfocusedContainerColor = DarkSlateBackground
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.addNewGearItem() },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonOrangePrimary)
                                .size(48.dp)
                                .testTag("equipment_add_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add equipment item", tint = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    checklist.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Check item logo",
                                    tint = if (item.isChecked) NeonOrangePrimary else Color.LightGray,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { viewModel.toggleChecklistItem(item) }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.name,
                                    color = if (item.isChecked) Color.Gray else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!item.isDefault) {
                                IconButton(
                                    onClick = { viewModel.deleteChecklistItem(item.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete item toggle",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("DEFAULT", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }

        } else {
            // MAIN STEP 2: ACTIVE SESSION IN PROGRESS RUNNING
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CharcoalCard)
                        .border(1.dp, NeonOrangePrimary, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${viewModel.selectedActivityType.uppercase()} TRACKING ACTIVE",
                            color = NeonOrangePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            Text(
                                text = "LIVE GPS",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // HUGE RUN CLOCK TIMER
                    val minutes = viewModel.elapsedSeconds / 60
                    val seconds = viewModel.elapsedSeconds % 60
                    val clockStr = String.format("%02d:%02d", minutes, seconds)
                    Text(
                        text = clockStr,
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "DURATION TIME spent",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // METRICS GRIDS: Distance & HR & Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format("%.2f km", viewModel.currentDistanceKm),
                                color = NeonOrangeSecondary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text("DISTANCE COVERED", color = Color.Gray, fontSize = 9.sp)
                        }

                        // Wearable Heart Rate Display
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val heartScale = rememberInfiniteTransition().animateFloat(
                                    initialValue = 0.9f,
                                    targetValue = 1.25f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(450, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Pulse beat icon",
                                    tint = ActiveBpmCrimson,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer(scaleX = heartScale.value, scaleY = heartScale.value)
                                )
                                Text(
                                    text = "${viewModel.currentBpm}",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            val devName = profile.connectedDevice
                            Text(
                                text = if (devName != "None") "BPM ($devName)" else "ESTIMATED BPM (Heart Rate)",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Speed Metrics
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val speed = if (viewModel.elapsedSeconds > 0) {
                                viewModel.currentDistanceKm / (viewModel.elapsedSeconds / 3600.0)
                            } else {
                                0.0
                            }
                            Text(
                                text = String.format("%.1f km/h", speed),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text("CURRENT PACE SPEED", color = Color.Gray, fontSize = 9.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // PORTABLE CAMERA SHOT ALONG THE WAY PROMPTS
                    if (viewModel.photoPromptActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonOrangePrimary.copy(alpha = 0.12f))
                                .border(1.dp, NeonOrangePrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Snap camera prompt icon",
                                        tint = NeonOrangePrimary
                                    )
                                    Text(
                                        text = "PHOTO SPOT! TAKE A LOOK AROUND!",
                                        color = NeonOrangePrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Text(
                                    text = "Taking quick scenery photos turns running/cycling into a subconscious background meditation. Look at other beautiful things!",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.captureScenicPhoto() },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonOrangePrimary),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                    ) {
                                        Text("SNAP PHOTO", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                    Button(
                                        onClick = { viewModel.photoPromptActive = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        border = BorderStroke(1.dp, Color.Gray),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                    ) {
                                        Text("DISMISS", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    } else {
                        // Regular snapping options
                        OutlinedButton(
                            onClick = { viewModel.photoPromptActive = true },
                            border = BorderStroke(1.dp, NeonOrangePrimary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera icon", tint = NeonOrangePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TAKE PHOTO TO SHUT OUT EXERTION", color = NeonOrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Photo gallery taken during route
                    if (viewModel.takenPhotos.value.isNotEmpty()) {
                        Text(
                            text = "🛣️ CAPTURED ON ROUTE (${viewModel.takenPhotos.value.size})",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            viewModel.takenPhotos.value.forEach { photo ->
                                Box(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray)
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = photo,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        IconButton(
                                            onClick = { viewModel.removePhoto(photo) },
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete mock photo", tint = Color.Red, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // MOCKING GPS GOOGLE MAPS VISUALIZER ON JETPACK COMPOSE CANVAS
                    Text(
                        text = "🗺️ LIVE MAP PATH VISUALIZATION",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 6.dp)
                    )

                    SimulatedMapView(
                        gpsPoints = viewModel.recordedPath.value,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSlateBackground)
                            .border(1.dp, DarkBorderAccent, RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // MUSIC CONTROLLER IN ACTIVE WORKOUT
                    Text(
                        text = "🎵 ACTIVE SESSION MUSIC STREAM",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 6.dp)
                    )
                    MusicPlayerControlPanel(viewModel = viewModel)

                    Spacer(modifier = Modifier.height(20.dp))

                    // PAUSE AND SAVE CONTROLS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                notesInput = ""
                                showStopDialog = true 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonOrangePrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_session_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop workout", tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FINISH & SAVE", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.cancelActiveWorkout() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("cancel_session_button")
                        ) {
                            Text("CANCEL WORKOUT", color = Color.LightGray)
                        }
                    }

                    // Trigger mock idle scenarios buttons for user testing
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { viewModel.forceIdleScenario() },
                        border = BorderStroke(1.dp, Color.DarkGray),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Test Idle Alert (30 min)", color = Color.LightGray, fontSize = 9.sp)
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // PRE-START CHECKLIST MANDATORY DIALOG SCREEN
    if (viewModel.showPreChecklistDialog) {
        Dialog(onDismissRequest = { viewModel.showPreChecklistDialog = false }) {
            Surface(
                color = CharcoalCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DarkBorderAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Safety Alert logo",
                        tint = NeonOrangePrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "SAFETY DEPARTURE GEARY CHECKLIST",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Ensure all customizable checklist boxes are ticked before leaving home:",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Checked Items Iteration
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        checklist.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSlateBackground)
                                    .border(1.dp, if (item.isChecked) NeonOrangePrimary.copy(alpha = 0.5f) else DarkBorderAccent, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.toggleChecklistItem(item) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = "Checkbox check logo",
                                    tint = if (item.isChecked) NeonOrangePrimary else Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.name,
                                    color = if (item.isChecked) Color.Gray else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (viewModel.gearCheckError != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = viewModel.gearCheckError!!,
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.verifyAndStartWorkout() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonOrangePrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("I HAVE EVERYTHING!", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.showPreChecklistDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier
                                .weight(0.8f)
                                .height(44.dp)
                        ) {
                            Text("ABORT", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // SAVE INTERACTIVE FORM POPUP
    if (showStopDialog) {
        Dialog(onDismissRequest = { showStopDialog = false }) {
            Surface(
                color = CharcoalCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DarkBorderAccent)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SAVE YOUR SESSION",
                        color = NeonOrangePrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nice execution! Type a personal best progress review or routing notes:",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = { Text("How was your workout? 'Took gorgeous pictures and focused on breathing, my mind went completely silent!'", color = Color.DarkGray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonOrangePrimary,
                            unfocusedBorderColor = DarkBorderAccent,
                            focusedContainerColor = DarkSlateBackground,
                            unfocusedContainerColor = DarkSlateBackground
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.stopAndSaveWorkout(notesInput)
                                showStopDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonOrangePrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SAVE LOGBOOK", color = Color.Black, fontWeight = FontWeight.Black)
                        }

                        Button(
                            onClick = { showStopDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("BACK", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // INTERACTIVE SIMULATED DONATION PAYMENT PROCESS OVERLAY
    if (showDonationOverlay) {
        Dialog(onDismissRequest = {}) {
            Surface(
                color = CharcoalCard,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, DarkBorderAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (donationStatusStep) {
                        0 -> {
                            // Stage 0: Connecting to Gateway
                            CircularProgressIndicator(
                                color = NeonOrangePrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "CONNECTING GATEWAY...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Contacting European Euro bank processing clearing centers securely via SSL connection...",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            
                            // Let's advance stages based on timer simulation!
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(1200)
                                donationStatusStep = 1
                            }
                        }
                        1 -> {
                            // Stage 1: Authenticating Transaction
                            CircularProgressIndicator(
                                color = NeonOrangePrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "AUTHORIZING PAYMENT...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            val finalAmount = donationAmountInput.toDoubleOrNull() ?: 10.0
                            Text(
                                text = "Charging simulated card account for €${String.format("%.2f", finalAmount)}...",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp)
                            )

                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(1500)
                                val finalAmt = donationAmountInput.toDoubleOrNull() ?: 10.0
                                showDonationOverlay = false
                                viewModel.recordDonation(finalAmt)
                            }
                        }
                    }
                }
            }
        }
    }
}

// SIMULATED CUSTOM CANVAS DRAWN GOOGLE MAP COMPONENT
@Composable
fun SimulatedMapView(
    gpsPoints: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // A. Draw Map Grid Background to simulate maps coordinates
        val gridSpacing = 40.dp.toPx()
        val gridPaint = Color.White.copy(alpha = 0.04f)

        if (gridSpacing > 1f) {
            // Draw vertical gridlines
            var x = 0f
            while (x < width) {
                drawLine(gridPaint, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
                x += gridSpacing
            }
            // Draw horizontal gridlines
            var y = 0f
            while (y < height) {
                drawLine(gridPaint, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
                y += gridSpacing
            }
        }

        // B. Plot the track coordinates if they exist
        if (gpsPoints.isNotEmpty()) {
            val polylinePath = Path()

            // Map latitude/longitude offsets into coordinate bounds in Canvas
            val minLat = gpsPoints.minOf { it.first }
            val maxLat = gpsPoints.maxOf { it.first }
            val minLng = gpsPoints.minOf { it.second }
            val maxLng = gpsPoints.maxOf { it.second }

            val latSpan = maxLat - minLat
            val lngSpan = maxLng - minLng

            gpsPoints.forEachIndexed { index, pair ->
                // Calculate scale offsets relative to canvas size
                val px = if (lngSpan > 0.0) {
                    20f + ((pair.second - minLng) / lngSpan).toFloat() * (width - 40f)
                } else {
                    width / 2f
                }

                val py = if (latSpan > 0.0) {
                    20f + (1f - ((pair.first - minLat) / latSpan).toFloat()) * (height - 40f)
                } else {
                    height / 2f
                }

                if (index == 0) {
                    polylinePath.moveTo(px, py)
                } else {
                    polylinePath.lineTo(px, py)
                }
            }

            // Draw route in striking neon orange line
            drawPath(
                path = polylinePath,
                color = NeonOrangePrimary,
                style = Stroke(width = 6f, miter = 1f)
            )

            // C. Highlight start marker with small white dot and current marker with active circular pulse
            val lastPair = gpsPoints.last()
            val lpx = if (lngSpan > 0.0) {
                20f + ((lastPair.second - minLng) / lngSpan).toFloat() * (width - 40f)
            } else {
                width / 2f
            }

            val lpy = if (latSpan > 0.0) {
                20f + (1f - ((lastPair.first - minLat) / latSpan).toFloat()) * (height - 40f)
            } else {
                height / 2f
            }

            // First start point dot
            val firstPair = gpsPoints.first()
            val fpx = if (lngSpan > 0.0) {
                20f + ((firstPair.second - minLng) / lngSpan).toFloat() * (width - 40f)
            } else {
                width / 2f
            }

            val fpy = if (latSpan > 0.0) {
                20f + (1f - ((firstPair.first - minLat) / latSpan).toFloat()) * (height - 40f)
            } else {
                height / 2f
            }

            drawCircle(
                color = Color.White,
                radius = 8f,
                center = Offset(fpx, fpy)
            )

            // Pulse glowing indicator for current point
            drawCircle(
                color = NeonOrangeSecondary.copy(alpha = 0.35f),
                radius = 24f,
                center = Offset(lpx, lpy)
            )
            drawCircle(
                color = Color.Cyan,
                radius = 8f,
                center = Offset(lpx, lpy)
            )
        }
    }
}

// SCREEN 2: HISTORY LIST WITH PERSONAL BEST BADGES
@Composable
fun HistoryScreen(
    workouts: List<WorkoutLog>,
    profile: UserProfileSettings,
    onDeleteWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "YOUR TRAINING LOGBOOK",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Track your historical workouts & Personal Best (PR) achievements below",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (workouts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "Empty state icon",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No workouts recorded yet",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Complete your checklist and start tracking on the Record tab!",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 30.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(workouts) { log ->
                    WorkoutLogCard(log = log, onDelete = { onDeleteWorkout(log.id) })
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun WorkoutLogCard(
    log: WorkoutLog,
    onDelete: () -> Unit
) {
    val dateStr = remember(log.dateTimestamp) {
        val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(log.dateTimestamp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        border = BorderStroke(1.dp, if (log.isPersonalBestDistance || log.isPersonalBestSpeed || log.isPersonalBestDuration) NeonOrangePrimary.copy(alpha = 0.5f) else DarkBorderAccent),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Activity Icon, Date, Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(NeonOrangePrimary.copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (log.activityType == "Running") Icons.Default.DirectionsRun else Icons.Default.DirectionsBike,
                            contentDescription = "Activity type icon",
                            tint = NeonOrangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (log.activityType == "Running") "RUNNING WORKOUT" else "CYCLING WORKOUT",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = dateStr,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete workout", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // STATS ROW Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSlateBackground)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.2f km", log.distanceKm),
                        color = NeonOrangePrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("DISTANCE", color = Color.Gray, fontSize = 9.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val minutes = log.durationSeconds / 60
                    val seconds = log.durationSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("DURATION", color = Color.Gray, fontSize = 9.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = log.paceMinPerKm,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("AV PACE", color = Color.Gray, fontSize = 9.sp)
                }

                if (log.averageHeartRate > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${log.averageHeartRate} bpm",
                            color = ActiveBpmCrimson,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text("HR BPM", color = Color.Gray, fontSize = 9.sp)
                    }
                }
            }

            // PERSONAL BESTS CHIPS FLAGS
            if (log.isPersonalBestDistance || log.isPersonalBestDuration || log.isPersonalBestSpeed) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (log.isPersonalBestDistance) {
                        PersonalBestBadge(text = "🔥 BEST DISTANCE")
                    }
                    if (log.isPersonalBestSpeed) {
                        PersonalBestBadge(text = "⚡ BEST PACE / SPEED")
                    }
                    if (log.isPersonalBestDuration) {
                        PersonalBestBadge(text = "⏱️ LONGEST ENDUR")
                    }
                }
            }

            // Route Photos Captured Showcase
            if (log.photoUrls.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📸 Captured scenery photo logs:",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                val photoList = log.photoUrls.split(",")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    photoList.forEach { photoText ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .padding(6.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera check logo", tint = NeonOrangeTertiary, modifier = Modifier.size(10.dp))
                            Text(
                                text = photoText,
                                color = Color.LightGray,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // Workout Notes Summary
            if (log.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Notes: \"${log.notes}\"",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            // Saved route visualizer preview thumbnail
            if (log.routeCoordinatesJson.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                val savedPoints = remember(log.routeCoordinatesJson) {
                    log.routeCoordinatesJson.split(";").mapNotNull {
                        val pts = it.split(",")
                        if (pts.size == 2) {
                            pts[0].toDoubleOrNull()?.let { lat ->
                                pts[1].toDoubleOrNull()?.let { lng ->
                                    Pair(lat, lng)
                                }
                            }
                        } else null
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSlateBackground)
                ) {
                    SimulatedMapView(gpsPoints = savedPoints, modifier = Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("SAVED MAP ROUTE", color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalBestBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NeonOrangePrimary.copy(alpha = 0.2f))
            .border(1.dp, NeonOrangePrimary, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = NeonOrangePrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black
        )
    }
}

// SCREEN 3: WEEKLY CHARTS & TREND VISUALIZER
@Composable
fun WeeklyChartsScreen(
    workouts: List<WorkoutLog>,
    profile: UserProfileSettings,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf("Distance") } // "Distance", "Duration", "Calories/Speed"

    // Simulate standard aggregate days for weekly comparison
    // We group workouts by some simulated days or partition them into standard interval slots
    val lastSevenDaysWorkouts = remember(workouts) {
        // Group workouts of the current week (e.g. last 7 days)
        val sorted = workouts.sortedBy { it.dateTimestamp }
        // Let's bucket them into standard Mon/Tue/Wed/Thu/Fri/Sat/Sun representing recent achievements
        val map = mutableMapOf(
            "Mon" to 0.0,
            "Tue" to 0.0,
            "Wed" to 0.0,
            "Thu" to 0.0,
            "Fri" to 0.0,
            "Sat" to 0.0,
            "Sun" to 0.0
        )
        // Set high values if empty database for demo preview so charts are not blank or boring!
        if (workouts.isEmpty()) {
            map["Mon"] = 4.5
            map["Tue"] = 0.0
            map["Wed"] = 6.8
            map["Thu"] = 2.1
            map["Fri"] = 0.0
            map["Sat"] = 10.2
            map["Sun"] = 5.0
        } else {
            // Distribute user logs into Mon-Sun depending on calendar
            val sdf = SimpleDateFormat("EEE", Locale.US)
            workouts.forEach {
                val day = sdf.format(Date(it.dateTimestamp))
                if (map.containsKey(day)) {
                    map[day] = (map[day] ?: 0.0) + it.distanceKm
                } else {
                    map["Sat"] = (map["Sat"] ?: 0.0) + it.distanceKm
                }
            }
        }
        map.toList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "WEEKLY PROGRESS CHARTS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Track improvements weekly. Solid line represents Goal targets",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Metrics Picker Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CharcoalCard)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Distance", "Duration").forEach { metric ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedMetric == metric) NeonOrangePrimary else Color.Transparent)
                            .clickable { selectedMetric = metric }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = metric.uppercase(),
                            color = if (selectedMetric == metric) Color.Black else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // CUSTOM HIGH-FIDELITY DRAWN CANVAS CHART WINDOW
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                border = BorderStroke(1.dp, DarkBorderAccent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WEEKLY ACHIEVEMENTS (Mon-Sun)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    WeeklyProgressBarChart(
                        data = lastSevenDaysWorkouts,
                        metricName = selectedMetric,
                        targetGoal = profile.targetWeeklyKm / 7.0, // Divided per day
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(NeonOrangePrimary, RoundedCornerShape(2.dp))
                            )
                            Text("Current Progress", color = Color.Gray, fontSize = 10.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color.Cyan, RoundedCornerShape(2.dp))
                            )
                            Text("Standard Target Goal", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // STATS HIGHLIGHT COMPONENT
        item {
            val totalWeeklyAchieved = remember(lastSevenDaysWorkouts) {
                lastSevenDaysWorkouts.sumOf { it.second }
            }
            val percentage = (totalWeeklyAchieved / profile.targetWeeklyKm) * 100.0

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CharcoalCard)
                    .border(1.dp, DarkBorderAccent, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Weekly Target Analysis",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "ACCUMULATED WEEKLY", color = Color.Gray, fontSize = 9.sp)
                        Text(
                            text = String.format("%.1f km / %.1f km", totalWeeklyAchieved, profile.targetWeeklyKm),
                            color = NeonOrangePrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "TARGET COMPLETED", color = Color.Gray, fontSize = 9.sp)
                        Text(
                            text = String.format("%.0f%%", percentage),
                            color = if (percentage >= 100.0) SyncedGreen else Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (totalWeeklyAchieved / profile.targetWeeklyKm).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = NeonOrangePrimary,
                    trackColor = DarkSlateBackground,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// THE CUSTOM DRAWN CANVAS WEEKLY BAR CHART METRIC VISUALIZER
@Composable
fun WeeklyProgressBarChart(
    data: List<Pair<String, Double>>,
    metricName: String,
    targetGoal: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val maxOfData = if (data.isNotEmpty()) data.maxOf { it.second } else 0.0
        val maxVal = maxOf(maxOfData, targetGoal, 1.0)
        val chartPadding = 24.dp.toPx()
        val graphHeight = height - chartPadding - 10.dp.toPx()
        val cellWidth = if (data.isNotEmpty()) (width - chartPadding * 2) / data.size else 1f

        // 1. Draw Axis Lines
        drawLine(
            color = Color.Gray.copy(alpha = 0.2f),
            start = Offset(chartPadding, graphHeight),
            end = Offset(width - chartPadding, graphHeight),
            strokeWidth = 2f
        )

        // 2. Iterate each column
        data.forEachIndexed { i, pair ->
            val colX = chartPadding + (i * cellWidth) + (cellWidth / 2f)

            // Draw Bar
            val barHeight = ((pair.second / maxVal) * graphHeight).toFloat()
            val barWidth = 14.dp.toPx()

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFF00E676), Color(0xFFFF6D00)),
                    startY = graphHeight,
                    endY = graphHeight - barHeight
                ),
                topLeft = Offset(colX - barWidth / 2f, graphHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // Draw Daily text labels or days
            // Note: Since standard Canvas drawText has complex typeface loading, we can draw neat markers or indicators,
            // or we draw tiny points. Let's draw horizontal indicator marks & text is managed cleanly in labels!
        }

        // 3. Draw horizontal target Goal dotted line
        val targetY = (graphHeight - ((targetGoal / maxVal) * graphHeight)).toFloat()
        drawLine(
            color = Color.Cyan,
            start = Offset(chartPadding, targetY),
            end = Offset(width - chartPadding, targetY),
            strokeWidth = 3f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }

    // Days text indicator label row directly in compose below canvas inside container
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        data.forEach { pair ->
            Text(
                text = pair.first,
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

// SCREEN 4: SOCIAL LEADERBOARD & USER PROFILE SETTINGS
@Composable
fun SocialLeaderboardScreen(
    viewModel: WorkoutViewModel,
    profile: UserProfileSettings,
    modifier: Modifier = Modifier
) {
    var googleEmailInput by remember { mutableStateOf("") }
    var googleNameInput by remember { mutableStateOf("") }
    var showGoogleDialog by remember { mutableStateOf(false) }
    var isAccountDropdownExpanded by remember { mutableStateOf(false) }

    // Mock friends on leaderboard
    val friendsList = remember {
        listOf(
            Friend("Liam Davies", 54.2, "Cycling", "3 hours ago"),
            Friend("Emma Stone", 42.5, "Running", "6 hours ago"),
            Friend("John Doe (You)", 0.0, "Running", "Just now"), // We will map this dynamically to profile settings metrics
            Friend("Mason Williams", 31.8, "Cycling", "1 day ago"),
            Friend("Sophia Miller", 22.1, "Running", "2 days ago")
        )
    }

    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SOCIAL LEADERboard",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Stay motivated! Compare weekly totals and share your active maps",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // LEADERBOARD LIST DISPLAY
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                border = BorderStroke(1.dp, DarkBorderAccent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WEEKLY LEADERS",
                            color = NeonOrangeSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("FRIENDS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    friendsList.forEachIndexed { rank, friend ->
                        val isCurrentUser = friend.name.contains("You")
                        // If current user is you, fetch dynamically accumulated workouts
                        val currentWorkouts = viewModel.workoutsState.value
                        val currentDistance = if (isCurrentUser) {
                            currentWorkouts.sumOf { it.distanceKm }
                        } else {
                            friend.weeklyKm
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrentUser) NeonOrangePrimary.copy(alpha = 0.07f) else Color.Transparent)
                                .border(1.dp, if (isCurrentUser) NeonOrangePrimary.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Ranking medal colors
                                val rankColor = when (rank) {
                                    0 -> LeaderboardGold
                                    1 -> LeaderboardSilver
                                    2 -> LeaderboardBronze
                                    else -> Color.Gray
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(rankColor)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${rank + 1}",
                                        color = if (rank < 3) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (isCurrentUser) "${profile.userName} (You)" else friend.name,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrentUser) FontWeight.Black else FontWeight.Bold
                                        )
                                        if (isCurrentUser && profile.totalDonatedEuro > 0.0) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(NeonOrangePrimary.copy(alpha = 0.15f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "🏅 DONOR",
                                                    color = NeonOrangePrimary,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (friend.primaryActivity == "Running") Icons.Default.DirectionsRun else Icons.Default.DirectionsBike,
                                            contentDescription = "Leader activity tracking logo",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "${friend.primaryActivity} • Completed ${friend.recentTime}",
                                            color = Color.Gray,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = String.format("%.1f km", currentDistance),
                                    color = if (isCurrentUser) NeonOrangePrimary else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )

                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "Route details Shared to ${friend.name}!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share routes item",
                                        tint = NeonOrangePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // WATCH WRISTBAND SYNC OPTION PANEL
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CharcoalCard)
                    .border(1.dp, DarkBorderAccent, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⌚ SMART WATCHES & WRISTBANDS",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonOrangePrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("INTEGRATION", color = NeonOrangePrimary, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Track real-time heart rate, average BPM and fitness telemetry directly from popular smartwatch APIs.",
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (profile.connectedDevice != "None") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSlateBackground)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Watch,
                                contentDescription = "Active smart watch connected icon",
                                tint = SyncedGreen
                            )
                            Column {
                                Text(profile.connectedDevice, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("BPM Heart Rate streaming active", color = Color.Gray, fontSize = 9.sp)
                            }
                        }

                        Button(
                            onClick = { viewModel.connectWearable("None") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("DISCONNECT", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.showWatchConnectDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonOrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("connect_wearable_button")
                    ) {
                        Icon(Icons.Default.Watch, contentDescription = "Watch pair icon", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONNECT SMART WATCH / BAND", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // OFFLINE BACKUPS & CLOUD SYNCING PANEL
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CharcoalCard)
                    .border(1.dp, DarkBorderAccent, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "☁️ OFFLINE STORAGE & CLOUD BACKUPS",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "If remote signals are low, data is saved locally offline and automatically backed up to user cloud profiles later.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSlateBackground)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (viewModel.isSyncing) "BACKING UP TO CLOUD..." else "SECURED LOCAL COPY",
                            color = if (viewModel.isSyncing) NeonOrangePrimary else SyncedGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        val formattedDate = if (profile.lastSyncTimestamp > 0L) {
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(profile.lastSyncTimestamp))
                        } else {
                            "Never"
                        }
                        Text(
                            text = "Last synced: $formattedDate",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }

                    if (viewModel.isSyncing) {
                        CircularProgressIndicator(
                            color = NeonOrangePrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Button(
                            onClick = { viewModel.syncDataWithCloud() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonOrangePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("sync_cloud_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync records now icon", tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SYNC NOW", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // GOOGLE ACCOUNT REGISTRATION & SETTINGS SECTION
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CharcoalCard)
                    .border(1.dp, DarkBorderAccent, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "🛡️ ACCOUNT PORTAL SETTINGS",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap the dropdown below to instantly switch between active training profiles or link custom Google coordinates.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSlateBackground)
                            .border(1.dp, DarkBorderAccent, RoundedCornerShape(10.dp))
                            .clickable { isAccountDropdownExpanded = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(NeonOrangePrimary.copy(alpha = 0.15f))
                                    .size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (profile.isGoogleConnected && profile.userName.isNotEmpty()) profile.userName.take(1).uppercase() else "AR",
                                    color = NeonOrangePrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (profile.isGoogleConnected) profile.userName else "Offline Guest Athlete",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (profile.isGoogleConnected) profile.email else "offline-user@gmail.com",
                                    color = Color.Gray,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (profile.isGoogleConnected) "CONNECTED" else "LOCAL ONLY",
                                color = if (profile.isGoogleConnected) SyncedGreen else Color.Gray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select different Google account dropdown",
                                tint = NeonOrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isAccountDropdownExpanded,
                        onDismissRequest = { isAccountDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(CharcoalCard)
                            .border(1.dp, DarkBorderAccent, RoundedCornerShape(8.dp))
                    ) {
                        val mockAccounts = listOf(
                            Pair("John Doe", "john.doe@gmail.com"),
                            Pair("Sarah Connor", "sarah.c@gmail.com"),
                            Pair("Alex Mercer", "alex.mercer@gmail.com"),
                            Pair("Emily Stone", "emily.stone@gmail.com")
                        )

                        mockAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(account.first, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(account.second, color = Color.Gray, fontSize = 9.sp)
                                        }
                                        if (profile.isGoogleConnected && profile.email == account.second) {
                                            Icon(Icons.Default.Check, contentDescription = "Active user", tint = SyncedGreen, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.connectGoogleAccount(account.second, account.first)
                                    isAccountDropdownExpanded = false
                                }
                            )
                        }

                        Divider(color = DarkBorderAccent)

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Google Account icon", tint = NeonOrangePrimary, modifier = Modifier.size(16.dp))
                                    Text("Add new account...", color = NeonOrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                isAccountDropdownExpanded = false
                                showGoogleDialog = true
                            }
                        )

                        if (profile.isGoogleConnected) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout icon", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        Text("Disconnect accounts", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    isAccountDropdownExpanded = false
                                    viewModel.disconnectGoogleAccount()
                                }
                            )
                        }
                    }
                }
            }
        }

        // ⚙️ ATHLETE GENERAL PREFERENCES SECTION
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CharcoalCard)
                    .border(1.dp, DarkBorderAccent, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "⚙️ ATHLETE PREFERENCES",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Light / Dark Theme Mode Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (profile.darkTheme) Icons.Default.Brightness2 else Icons.Default.LightMode,
                            contentDescription = "Theme selection custom icon",
                            tint = NeonOrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("Cinematic Theme", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(if (profile.darkTheme) "Dark Active Slate" else "Light Active Slate", color = Color.Gray, fontSize = 9.sp)
                        }
                    }
                    Switch(
                        checked = profile.darkTheme,
                        onCheckedChange = { viewModel.toggleDarkTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonOrangePrimary,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("toggle_dark_theme")
                    )
                }

                Divider(color = DarkBorderAccent, modifier = Modifier.padding(vertical = 8.dp))

                // Weather Sync Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Weather sync simulation icon",
                            tint = NeonOrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("Local Weather Sync", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Simulate climate pairing for running & cycling", color = Color.Gray, fontSize = 9.sp)
                        }
                    }
                    Switch(
                        checked = profile.weatherSyncEnabled,
                        onCheckedChange = { viewModel.toggleWeatherSync() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonOrangePrimary,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("toggle_weather_sync")
                    )
                }

                Divider(color = DarkBorderAccent, modifier = Modifier.padding(vertical = 8.dp))

                // Metric System Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Display layout coordinate toggle",
                            tint = NeonOrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("Metric Coordinates (Km / m / °C)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(if (profile.metricSystem) "Celsius and Kilometers" else "Fahrenheit and Miles", color = Color.Gray, fontSize = 9.sp)
                        }
                    }
                    Switch(
                        checked = profile.metricSystem,
                        onCheckedChange = { viewModel.toggleMetricSystem() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonOrangePrimary,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("toggle_metric_system")
                    )
                }
            }
        }

        // 💖 DONATE & SUPPORT THE APP
        item {
            var inputAmount by remember { mutableStateOf("") }
            var selectedPreset by remember { mutableStateOf<Double?>(null) }
            val context = LocalContext.current

            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                border = BorderStroke(1.dp, NeonOrangePrimary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("donation_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Donate Heart Icon",
                                tint = NeonOrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "SUPPORT CO-DEVELOPERS",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonOrangePrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("EURO (€)", color = NeonOrangePrimary, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Apex Track is completely free and ad-free! Support our running development, API keys, and maintenance costs with a custom contribution.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // PRESET QUICK BUCHUNGS-BUTTONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val presets = listOf(2.0, 5.0, 10.0, 20.0, 50.0)
                        presets.forEach { amt ->
                            val isSelected = selectedPreset == amt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonOrangePrimary.copy(alpha = 0.2f) else DarkSlateBackground)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) NeonOrangePrimary else DarkBorderAccent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedPreset = amt
                                        inputAmount = String.format("%.2f", amt)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "€${amt.toInt()}",
                                    color = if (isSelected) NeonOrangePrimary else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // CUSTOM AMOUNT TEXTFIELD (WITH EURO SIGN)
                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.toDoubleOrNull() != null || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                inputAmount = newValue
                                selectedPreset = null
                            }
                        },
                        label = { Text("Custom Amount in Euro", fontSize = 11.sp) },
                        prefix = { Text("€ ", color = NeonOrangePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonOrangePrimary,
                            unfocusedBorderColor = DarkBorderAccent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = NeonOrangePrimary,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("donation_amount_input"),
                        shape = RoundedCornerShape(10.dp),
                        placeholder = { Text("0.00", color = Color.DarkGray, fontSize = 12.sp) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // SUBMIT DONATE BUTTON
                    Button(
                        onClick = {
                            val amt = inputAmount.toDoubleOrNull()
                            if (amt == null || amt <= 0.0) {
                                Toast.makeText(context, "Please enter a valid positive Euro amount!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.recordDonation(amt)
                                inputAmount = ""
                                selectedPreset = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonOrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("donate_button")
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Favorite Heart Icon", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val donateLabel = if (inputAmount.isNotEmpty() && inputAmount.toDoubleOrNull() != null) {
                            "DONATE €${inputAmount}"
                        } else {
                            "DONATE TO APEX TRACK"
                        }
                        Text(
                            text = donateLabel,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    if (profile.totalDonatedEuro > 0.0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Support success indicator icon",
                                tint = SyncedGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Your Lifetime Contribution: €${String.format("%.2f", profile.totalDonatedEuro)} (Thank you! ❤️)",
                                color = SyncedGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Google connection Simulation Dialog
    if (showGoogleDialog) {
        Dialog(onDismissRequest = { showGoogleDialog = false }) {
            Surface(
                color = CharcoalCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DarkBorderAccent)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock secure icon", tint = NeonOrangePrimary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "LINK GOOGLE ACCOUNT",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Enter account credentials to enable cloud backups & leaderboards sync:",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = googleNameInput,
                        onValueChange = { googleNameInput = it },
                        label = { Text("Your Athlete Name", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonOrangePrimary,
                            unfocusedBorderColor = DarkBorderAccent,
                            focusedContainerColor = DarkSlateBackground,
                            unfocusedContainerColor = DarkSlateBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = googleEmailInput,
                        onValueChange = { googleEmailInput = it },
                        label = { Text("Google Email address", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonOrangePrimary,
                            unfocusedBorderColor = DarkBorderAccent,
                            focusedContainerColor = DarkSlateBackground,
                            unfocusedContainerColor = DarkSlateBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (googleEmailInput.isNotBlank() && googleNameInput.isNotBlank()) {
                                    viewModel.connectGoogleAccount(googleEmailInput.trim(), googleNameInput.trim())
                                    showGoogleDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonOrangePrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CONNECT", color = Color.Black, fontWeight = FontWeight.Black)
                        }

                        Button(
                            onClick = { showGoogleDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ABORT", color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }

    // Wearable Connected simulation dialog
    if (viewModel.showWatchConnectDialog) {
        Dialog(onDismissRequest = { viewModel.showWatchConnectDialog = false }) {
            Surface(
                color = CharcoalCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DarkBorderAccent)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PAIR RECENT WEARABLES",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Bluetooth sensors nearby. Select wristband to read BPM:",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    listOf("Apple Watch Ultra 2", "Garmin Instinct Solar", "Fitbit Charge 6").forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.connectWearable(device)
                                    viewModel.showWatchConnectDialog = false
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSlateBackground)
                                .border(1.dp, DarkBorderAccent, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Watch, contentDescription = "Simulated wear", tint = NeonOrangeTertiary)
                                Text(device, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = "Select device logo", tint = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.showWatchConnectDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CANCEL", color = Color.LightGray)
                    }
                }
            }
        }
    }
}

// BATTERY SAVING IDLE WORKOUT WARNING TRIGGER
@Composable
fun IdleBatteryAlertOverlay(
    onDismiss: () -> Unit,
    onStopSession: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = CharcoalCard,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, Color.Red),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = "Battery Warning icon",
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "IDLING BATTERY WARNING",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Your tracking session has been idling for more than 30 minutes. Shut down active CPU / GPS sensors to preserve critical phone battery life?",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStopSession,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("STOP WORKOUT NOW", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("KEEP TRACKING", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

data class Friend(
    val name: String,
    val weeklyKm: Double,
    val primaryActivity: String,
    val recentTime: String
)

// DYNAMIC PROGRAMMATIC MOUNTAIN TRAIL LOGO
@Composable
fun MountainTrailLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Beautiful Amber / Sun setting behind peaks
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFEA00), Color(0xFFFF3D00)),
                center = Offset(width * 0.5f, height * 0.38f),
                radius = width * 0.28f
            ),
            center = Offset(width * 0.5f, height * 0.38f),
            radius = width * 0.28f
        )
        
        // Left Ridge Peak (Electric Cyan/Light Blue)
        val leftPeakPath = Path().apply {
            moveTo(width * 0.12f, height * 0.82f)
            lineTo(width * 0.44f, height * 0.36f)
            lineTo(width * 0.68f, height * 0.82f)
            close()
        }
        drawPath(leftPeakPath, color = Color(0xFF00E5FF))
        
        // Right Ridge Peak (Vibrant Emerald Green)
        val rightPeakPath = Path().apply {
            moveTo(width * 0.38f, height * 0.82f)
            lineTo(width * 0.68f, height * 0.42f)
            lineTo(width * 0.94f, height * 0.82f)
            close()
        }
        drawPath(rightPeakPath, color = Color(0xFF00E676))
        
        // Winding Trail Path (High-Contrast White trail flowing from bottom to mountain gap)
        val trailPath = Path().apply {
            moveTo(width * 0.54f, height * 0.82f)
            cubicTo(
                width * 0.42f, height * 0.72f,
                width * 0.66f, height * 0.64f,
                width * 0.54f, height * 0.55f
            )
            cubicTo(
                width * 0.45f, height * 0.49f,
                width * 0.56f, height * 0.44f,
                width * 0.54f, height * 0.40f
            )
        }
        drawPath(
            trailPath,
            color = Color.White,
            style = Stroke(
                width = 8f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

// REALTIME ANIMATED EQUALIZER DANCING COLUMNS
@Composable
fun PlayingSoundWaveEqualizer(isPlaying: Boolean, tintColor: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "music_eq_anim")
    
    // Animate individual bars differently for realistic sound waveform frequencies
    val heightScale0 = if (isPlaying) infiniteTransition.animateFloat(0.2f, 1.0f, infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse), "b0").value else 0.15f
    val heightScale1 = if (isPlaying) infiniteTransition.animateFloat(0.35f, 0.9f, infiniteRepeatable(tween(550, easing = LinearOutSlowInEasing), RepeatMode.Reverse), "b1").value else 0.15f
    val heightScale2 = if (isPlaying) infiniteTransition.animateFloat(0.15f, 1.0f, infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse), "b2").value else 0.15f
    val heightScale3 = if (isPlaying) infiniteTransition.animateFloat(0.4f, 0.8f, infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse), "b3").value else 0.15f
    val heightScale4 = if (isPlaying) infiniteTransition.animateFloat(0.2f, 0.95f, infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse), "b4").value else 0.15f
    val heightScale5 = if (isPlaying) infiniteTransition.animateFloat(0.3f, 1.0f, infiniteRepeatable(tween(500, easing = LinearOutSlowInEasing), RepeatMode.Reverse), "b5").value else 0.15f

    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(heightScale0, heightScale1, heightScale2, heightScale3, heightScale4, heightScale5).forEach { hVal ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(hVal)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(tintColor)
            )
        }
    }
}

// PREMIUM MULTI-PLATFORM AUDIO CONTROLLER WIDGET
@Composable
fun MusicPlayerControlPanel(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val activeService = viewModel.activeMusicService
    val isPlaying = viewModel.isMusicPlaying
    val serviceColor = when (activeService) {
        "Spotify" -> Color(0xFF1DB954)
        "YouTube Music" -> Color(0xFFFF0000)
        else -> Color(0xFFC2185B) // Deezer vibrant pink magenta
    }
    
    val bgGradient = when (activeService) {
        "Spotify" -> Brush.verticalGradient(colors = listOf(Color(0xFF0F301B), Color(0xFF161616)))
        "YouTube Music" -> Brush.verticalGradient(colors = listOf(Color(0xFF450F0F), Color(0xFF161616)))
        else -> Brush.verticalGradient(colors = listOf(Color(0xFF380B45), Color(0xFF161616)))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, serviceColor.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(bgGradient)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                
                // Active Service Selection Banner Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Music service category",
                            tint = serviceColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "AUDIO RUNNING COMPANION",
                            color = Color(0xFF94A3B8), // slate-400
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Platforms Selection Switcher
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Spotify", "YouTube Music", "Deezer").forEach { svc ->
                            val isSelected = svc == activeService
                            val chipBg = if (isSelected) serviceColor.copy(alpha = 0.22f) else Color.Transparent
                            val chipOutline = if (isSelected) serviceColor.copy(alpha = 0.6f) else Color(0xFF2E2E2E)
                            val chipText = if (isSelected) serviceColor else Color.Gray

                            Text(
                                text = if (svc == "YouTube Music") "YT Music" else svc,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = chipText,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(chipBg)
                                    .border(
                                        width = 0.8.dp,
                                        color = chipOutline,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.switchMusicService(svc) }
                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                // Track Album Art cover, Details & Equalizer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rotation angle animation for vinyl spinning
                        val infiniteAnim = rememberInfiniteTransition(label = "vinyl_rotate")
                        val angle by if (isPlaying) {
                            infiniteAnim.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(4000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "spin"
                            )
                        } else {
                            remember { mutableStateOf(0f) }
                        }

                        // Custom Spinning Vinyl Disc Artwork
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .graphicsLayer { rotationZ = angle }
                                .clip(CircleShape)
                                .background(Color(0xFF121212))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    radius = size.width * 0.45f
                                )
                                drawCircle(
                                    color = serviceColor.copy(alpha = 0.25f),
                                    radius = size.width * 0.28f,
                                    style = Stroke(width = 1.5f)
                                )
                                drawCircle(
                                    color = serviceColor,
                                    radius = size.width * 0.11f
                                )
                            }
                        }

                        // Track Metadata Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.currentTrackTitle,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = viewModel.currentTrackArtist,
                                color = Color.Gray,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Equalizer wave
                    PlayingSoundWaveEqualizer(isPlaying = isPlaying, tintColor = serviceColor)
                }

                // Streaming Playback duration slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { viewModel.musicPlaybackProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = serviceColor,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Math calculation for track durations representation
                        val totalSecs = 210
                        val currentSecs = (viewModel.musicPlaybackProgress * totalSecs).toInt()
                        val elapsedStr = String.format("%d:%02d", currentSecs / 60, currentSecs % 60)
                        val totalStr = String.format("%d:%02d", totalSecs / 60, totalSecs % 60)
                        
                        Text(elapsedStr, color = Color.Gray, fontSize = 9.sp)
                        Text(totalStr, color = Color.Gray, fontSize = 9.sp)
                    }
                }

                // AUDIO PLAYBACK PHYSICAL CONTROLS & HEART RATE SYNCHRONIZERS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cadence Heart Beat Cadence Speed Match button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (viewModel.cadenceSyncEnabled) Color(0xFFFF3D00).copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = 0.8.dp,
                                color = if (viewModel.cadenceSyncEnabled) Color(0xFFFF3D00).copy(alpha = 0.5f) else Color(0xFF2E2E2E),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.cadenceSyncEnabled = !viewModel.cadenceSyncEnabled
                                if (viewModel.cadenceSyncEnabled) {
                                    viewModel.checkAndSyncCadenceBpm(viewModel.currentBpm)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Heart pacing sync badge",
                            tint = if (viewModel.cadenceSyncEnabled) Color(0xFFFF3D00) else Color.Gray,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = if (viewModel.cadenceSyncEnabled) "BPM SYNC ACTIVE" else "SYNC CADENCE TO BPM",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = if (viewModel.cadenceSyncEnabled) Color(0xFFFF3D00) else Color.Gray,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // SKIP PREVIOUS • PLAY/PAUSE • SKIP NEXT PANEL
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.playPreviousTrack() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Prev Track button", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Circular play box
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(serviceColor)
                                .clickable { viewModel.isMusicPlaying = !viewModel.isMusicPlaying },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/pause toggle inside controller",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.playNextTrack() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next Track button", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Playlist selection toggle
                    IconButton(
                        onClick = { viewModel.selectSongDialogActive = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Select a track manually from playlist list",
                            tint = serviceColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// SONG SELECTOR OVERLAY DIALOG
@Composable
fun SelectSongDialog(
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit
) {
    val playlist = viewModel.getActivePlaylist()
    val activeService = viewModel.activeMusicService
    val brandColor = when (activeService) {
        "Spotify" -> Color(0xFF1DB954)
        "YouTube Music" -> Color(0xFFFF0000)
        else -> Color(0xFFC2185B)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF161616),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT $activeService SOUND",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close selections selector", tint = Color.Gray, modifier = Modifier.size(15.dp))
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                playlist.forEach { song ->
                    val isCurrent = song.first == viewModel.currentTrackTitle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrent) brandColor.copy(alpha = 0.12f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isCurrent) brandColor.copy(alpha = 0.35f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                viewModel.selectTrack(song.first, song.second)
                                onDismiss()
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = song.first,
                                color = if (isCurrent) brandColor else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = song.second,
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active selected play song indicator",
                                tint = brandColor,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = "Play selection sound row icon",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// DONATION SUCCESS CELEBRATION DIALOG
@Composable
fun DonationSuccessDialog(
    amount: Double,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF161616),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, NeonOrangePrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("donation_success_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Heart celebration animation container
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(NeonOrangePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Success celebration heart",
                        tint = NeonOrangePrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "THANK YOU SO MUCH! 🎉",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "A simulated donation of €${String.format("%.2f", amount)} has been successfully compiled and recorded. Your generous support goes directly towards supporting our co-developers, hosting costs, and future fitness upgrades!",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SyncedGreen.copy(alpha = 0.12f))
                        .border(1.dp, SyncedGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🏅 APEX SUPPORTER MEDAL AWARDED",
                        color = SyncedGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonOrangePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("dismiss_success_button")
                ) {
                    Text("YOU ARE WELCOME!", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
