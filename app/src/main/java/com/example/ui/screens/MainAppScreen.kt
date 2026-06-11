package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Lead
import com.example.data.model.SyncLog
import com.example.data.model.Territory
import com.example.ui.viewmodel.LeadViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.hypot

sealed class AppScreen(val route: String, val title: String) {
    object Map : AppScreen("map_route", "Grid Map")
    object Territories : AppScreen("territories_route", "Territories")
    object Sync : AppScreen("sync_route", "Cloud Sync")
    object User : AppScreen("user_route", "User Menu")
}

@Composable
fun MainAppScreen(
    viewModel: LeadViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Map) }

    val leads by viewModel.filteredLeads.collectAsStateWithLifecycle()
    val allLeadsRaw by viewModel.allLeads.collectAsStateWithLifecycle()
    val territories by viewModel.territories.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val selectedTerritoryId by viewModel.selectedTerritoryId.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Icon block inspired by clean minimal rounded logo
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Logo Icon",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "TerritoryPro",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "PHASE 1 • FIELD MVP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Online/offline synclink pill
                    val unsyncedCount = allLeadsRaw.count { !it.isSynced }
                    val isOffline = unsyncedCount > 0
                    val lastSyncLog = syncLogs.firstOrNull { it.status == "SUCCESS" }
                    val lastSyncTimeStr = lastSyncLog?.timestamp?.let { 
                        java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
                    } ?: "Never"

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            color = if (!isOffline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (!isOffline) Color(0xFFC8E6C9) else Color(0xFFFFCDD2))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (!isOffline) Color(0xFF2E7D32) else Color(0xFFC62828), CircleShape)
                                )
                                Text(
                                    text = if (!isOffline) "Online" else "Offline ($unsyncedCount)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (!isOffline) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                        Text(
                            text = "Last sync: $lastSyncTimeStr",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, end = 4.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Map,
                    onClick = { currentScreen = AppScreen.Map },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Map") },
                    label = { Text("Grid Map") },
                    modifier = Modifier.testTag("nav_item_map")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Territories,
                    onClick = { currentScreen = AppScreen.Territories },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Territories") },
                    label = { Text("Territories") },
                    modifier = Modifier.testTag("nav_item_territories")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Sync,
                    onClick = { currentScreen = AppScreen.Sync },
                    icon = {
                        val unsyncedCount = allLeadsRaw.count { !it.isSynced }
                        BadgedBox(
                            badge = {
                                if (unsyncedCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text(unsyncedCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync")
                        }
                    },
                    label = { Text("Cloud Sync") },
                    modifier = Modifier.testTag("nav_item_sync")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.User,
                    onClick = { currentScreen = AppScreen.User },
                    icon = { Icon(Icons.Default.Person, contentDescription = "User Menu") },
                    label = { Text("User") },
                    modifier = Modifier.testTag("nav_item_user")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                AppScreen.Map -> {
                    MapDashboard(
                        rawLeads = leads,
                        allLeadsCount = allLeadsRaw.size,
                        territories = territories,
                        selectedTerritoryId = selectedTerritoryId,
                        onTerritorySelected = { viewModel.selectedTerritoryId.value = it },
                        onAddLead = { name, address, status, lat, lng, notes, phone, tId ->
                            viewModel.addLead(name, address, status, lat, lng, notes, phone, tId)
                        },
                        onUpdateLead = { viewModel.updateLead(it) },
                        onDeleteLead = { viewModel.deleteLead(it) },
                        viewModel = viewModel
                    )
                }
                AppScreen.Territories -> {
                    TerritoryManagerScreen(
                        territories = territories,
                        leadsCountMap = allLeadsRaw.groupBy { it.territoryId }
                            .mapValues { it.value.size },
                        onAddTerritory = { name, desc, color ->
                            viewModel.addTerritory(name, desc, color)
                        },
                        onDeleteTerritory = { viewModel.deleteTerritory(it) }
                    )
                }
                AppScreen.Sync -> {
                    SyncLedgerScreen(
                        leads = allLeadsRaw,
                        syncLogs = syncLogs,
                        isSyncing = isSyncing,
                        onSyncClick = { viewModel.syncWithBackend() },
                        onClearLogs = { viewModel.clearLogs() }
                    )
                }
                AppScreen.User -> {
                    UserMenuScreen(
                        allLeads = allLeadsRaw,
                        onImportLeads = { imported ->
                            imported.forEach { lead ->
                                viewModel.addLead(lead.name, lead.address, lead.status, lead.latitude, lead.longitude, lead.notes, lead.phone, lead.territoryId)
                            }
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. MAP DASHBOARD SCREEN & INTERACTIVE CANVAS MAP
// ==========================================

// Florida Map Projections Constants
const val CENTER_LAT = 27.6648
const val CENTER_LNG = -81.5158
const val SCALE_FACTOR_LAT = 180.0
const val SCALE_FACTOR_LNG = 140.0

fun getCanvasX(longitude: Double): Float {
    return ((longitude - CENTER_LNG) * SCALE_FACTOR_LNG * 1.5).toFloat()
}

fun getCanvasY(latitude: Double): Float {
    return (-((latitude - CENTER_LAT) * SCALE_FACTOR_LAT * 2.0)).toFloat()
}

fun getLngFromCanvasX(canvasX: Float): Double {
    return CENTER_LNG + (canvasX / (SCALE_FACTOR_LNG * 1.5))
}

fun getLatFromCanvasY(canvasY: Float): Double {
    return CENTER_LAT - (canvasY / (SCALE_FACTOR_LAT * 2.0))
}

data class PersonalPermit(
    val permitNum: String,
    val date: String,
    val permitType: String,
    val status: String,
    val contractor: String
)

data class HousePlot(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val width: Float = 68f,
    val height: Float = 54f,
    val ownerName: String = "Norma Holmes",
    val primaryLanguage: String = "English",
    val tenureStatus: String = "Homeowner",
    val incomeBracket: String = "$110k-$120k",
    val gender: String = "Female",
    val maritalStatus: String = "Unmarried",
    val yearBuilt: String = "1985-1989",
    val estimatedValue: String = "$300k-$350k",
    val squareFeet: String = "1,000-1,499 sf",
    val occupants: Int = 2,
    val creditScore: String = "740-799",
    val initialRoofAge: Int = 18,
    val permits: List<PersonalPermit> = emptyList()
) {
    val phone: String
        get() = when (ownerName) {
            "Norma Holmes" -> "305-555-0143"
            "Frank Miller" -> "813-555-0182"
            "Sarah Connor" -> "407-555-0111"
            "Alejandro Ruiz" -> "305-555-0199"
            "Arthur Pendelton" -> "954-555-0176"
            "Chloe Sterling" -> "305-555-0155"
            "Captain Jack G." -> "305-555-0100"
            "Dr. Laura Vance" -> "813-555-0130"
            "Marcus Aurelius" -> "407-555-0150"
            "Sophia Loren" -> "954-555-0121"
            "Donald Vanderbilt" -> "561-555-0105"
            "Major Nelson" -> "321-555-0164"
            else -> "305-555-0122"
        }

    val email: String
        get() = when (ownerName) {
            "Norma Holmes" -> "norma.holmes@miamimail.com"
            "Frank Miller" -> "frank.miller@tampabay.rr.com"
            "Sarah Connor" -> "sconnor@orlandogis.net"
            "Alejandro Ruiz" -> "alejandro.ruiz@keyslife.com"
            "Arthur Pendelton" -> "arthur.p@browardbiz.com"
            "Chloe Sterling" -> "chloe.sterling@miamibeach.com"
            "Captain Jack G." -> "jack.sparrow@keywestcharter.com"
            "Dr. Laura Vance" -> "laura.vance@vancemd.org"
            "Marcus Aurelius" -> "marcus.aurelius@meditation.org"
            "Sophia Loren" -> "sophia.loren@hollywoodstar.com"
            "Donald Vanderbilt" -> "donald@vanderbiltproperties.com"
            "Major Nelson" -> "major.nelson@cocoabeach.gov"
            else -> "${ownerName.lowercase().replace(" ", ".")}@floridaowner.com"
        }
}

fun calculateDistanceMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusMiles = 3958.8
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadiusMiles * c
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDashboard(
    rawLeads: List<Lead>,
    allLeadsCount: Int,
    territories: List<Territory>,
    selectedTerritoryId: Long?,
    onTerritorySelected: (Long?) -> Unit,
    onAddLead: (String, String, String, Double, Double, String, String, Long?) -> Unit,
    onUpdateLead: (Lead) -> Unit,
    onDeleteLead: (Lead) -> Unit,
    viewModel: LeadViewModel
) {
    val selectedStatusFilters = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    val leads = if (selectedStatusFilters.isEmpty()) {
        rawLeads
    } else {
        rawLeads.filter { it.status in selectedStatusFilters }
    }

    var mapOffsetX by remember { mutableStateOf(0f) }
    var mapOffsetY by remember { mutableStateOf(0f) }
    var mapScale by remember { mutableStateOf(1f) }

    var useLeafletMap by remember { mutableStateOf(true) }
    var selectedMapLayer by remember { mutableStateOf("satellite") }
    var pendingLatitude by remember { mutableStateOf<Double?>(null) }
    var pendingLongitude by remember { mutableStateOf<Double?>(null) }
    var userLatitude by remember { mutableStateOf(28.3200) }
    var userLongitude by remember { mutableStateOf(-80.6080) }
    var userLocationCenterTrigger by remember { mutableStateOf(0) }

    var showAddDialog by remember { mutableStateOf(false) }
    var pendingAddCoordinates by remember { mutableStateOf<Offset?>(null) }
    var pendingAddressSuggestion by remember { mutableStateOf("") }
    var pendingCounty by remember { mutableStateOf<String?>(null) }

    var selectedLeadDetail by remember { mutableStateOf<Lead?>(null) }
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()

    // Slide-up housing details state
    var selectedHousePlot by remember { mutableStateOf<HousePlot?>(null) }

    // Pulsing GPS user location marker animation
    val infiniteTransition = rememberInfiniteTransition(label = "gps_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    // Permit tracking states
    val permitsPulled = remember { mutableStateMapOf<String, Boolean>() }
    var isPullingAddress by remember { mutableStateOf<String?>(null) }
    var pullProgressText by remember { mutableStateOf("") }

    // Pre-calculated static overlay layout streets/homes in Florida peninsula representing Miami, Tampa, Orlando etc.
    val staticHousePlots = remember {
        listOf(
            HousePlot(
                address = "342 Ocean Drive, Miami FL",
                latitude = 25.7711,
                longitude = -80.1303,
                ownerName = "Norma Holmes",
                primaryLanguage = "English",
                tenureStatus = "Homeowner",
                incomeBracket = "$110k-$120k",
                gender = "Female",
                maritalStatus = "Unmarried",
                yearBuilt = "1985-1989",
                estimatedValue = "$300k-$350k",
                squareFeet = "1,000-1,499 sf",
                occupants = 2,
                creditScore = "740-799",
                initialRoofAge = 18,
                permits = listOf(
                    PersonalPermit("PMT-2008-08412", "06/14/2008", "Roofing Replacement", "Completed", "Shoreline Roofing Co."),
                    PersonalPermit("ELE-2015-11029", "02/10/2015", "Solar PV System Install", "Completed", "Freedom Solar Specialists")
                )
            ),
            HousePlot(
                address = "1412 Sun & Beach Ave, Tampa FL",
                latitude = 27.9506,
                longitude = -82.4572,
                ownerName = "Frank Miller",
                primaryLanguage = "English",
                tenureStatus = "Homeowner",
                incomeBracket = "$85k-$95k",
                gender = "Male",
                maritalStatus = "Married",
                yearBuilt = "2004-2008",
                estimatedValue = "$450k-$500k",
                squareFeet = "1,800-2,200 sf",
                occupants = 4,
                creditScore = "680-719",
                initialRoofAge = 22,
                permits = listOf(
                    PersonalPermit("PMT-2004-03912", "08/12/2004", "New Construction Permit", "Completed", "Tampa Premier Builders")
                )
            ),
            HousePlot(
                address = "705 Citrus Grove Way, Orlando FL",
                latitude = 28.5383,
                longitude = -81.3792,
                ownerName = "Sarah Connor",
                primaryLanguage = "English",
                tenureStatus = "Homeowner",
                incomeBracket = "$135k-$150k",
                gender = "Female",
                maritalStatus = "Married",
                yearBuilt = "2010-2014",
                estimatedValue = "$350k-$400k",
                squareFeet = "1,500-1,799 sf",
                occupants = 3,
                creditScore = "720-739",
                initialRoofAge = 8,
                permits = listOf(
                    PersonalPermit("PMT-2018-09121", "11/04/2018", "Hurricane Shingle Upgrade", "Completed", "Orlando Roof Guard Inc."),
                    PersonalPermit("PLB-2021-00234", "04/05/2021", "Water Heater Replacement", "Completed", "PlumbLine Plumbing")
                )
            ),
            HousePlot(
                address = "890 Palm Blvd, Key West FL",
                latitude = 24.5551,
                longitude = -81.7800,
                ownerName = "Alejandro Ruiz",
                primaryLanguage = "Spanish",
                tenureStatus = "Homeowner",
                incomeBracket = "$70k-$80k",
                gender = "Male",
                maritalStatus = "Married",
                yearBuilt = "1990-1994",
                estimatedValue = "$550k-$600k",
                squareFeet = "1,200-1,499 sf",
                occupants = 5,
                creditScore = "700-719",
                initialRoofAge = 16,
                permits = listOf(
                    PersonalPermit("PMT-2010-00445", "10/18/2010", "Tile Roofing System Replacement", "Completed", "Key West Roofers Ltd.")
                )
            ),
            HousePlot(
                address = "1024 Broward Blvd, Fort Lauderdale FL",
                latitude = 26.1224,
                longitude = -80.1373,
                ownerName = "Arthur Pendelton",
                primaryLanguage = "English",
                tenureStatus = "Homeowner",
                incomeBracket = "$180k-$200k",
                gender = "Male",
                maritalStatus = "Married",
                yearBuilt = "2000-2003",
                estimatedValue = "$650k-$700k",
                squareFeet = "2,500-3,000 sf",
                occupants = 3,
                creditScore = "810-850",
                initialRoofAge = 5,
                permits = listOf(
                    PersonalPermit("PMT-2021-12049", "09/16/2021", "Metal Roofing Retrofit", "Completed", "Everlast Roofing Co."),
                    PersonalPermit("SOL-2022-00511", "01/14/2022", "Premium Solar PV Grid Ties", "Completed", "Freedom Solar Specialists")
                )
            ),
            HousePlot(
                address = "150 Collins Ave, Miami Beach FL",
                latitude = 25.7750,
                longitude = -80.1310,
                ownerName = "Chloe Sterling",
                primaryLanguage = "English",
                tenureStatus = "Renter",
                incomeBracket = "$90k-$100k",
                gender = "Female",
                maritalStatus = "Unmarried",
                yearBuilt = "2015-2019",
                estimatedValue = "$500k-$550k",
                squareFeet = "900-1,199 sf",
                occupants = 1,
                creditScore = "730-749",
                initialRoofAge = 7,
                permits = emptyList()
            ),
            HousePlot(
                address = "210 Duval St, Key West FL",
                latitude = 24.5570,
                longitude = -81.7950,
                ownerName = "Captain Jack G.",
                primaryLanguage = "English",
                tenureStatus = "Homeowner",
                incomeBracket = "$50k-$60k",
                gender = "Male",
                maritalStatus = "Unmarried",
                yearBuilt = "1890-1920",
                estimatedValue = "$800k-$900k",
                squareFeet = "1,400-1,699 sf",
                occupants = 2,
                creditScore = "690-715",
                initialRoofAge = 14,
                permits = listOf(
                    PersonalPermit("PMT-2012-04910", "11/12/2012", "Metal Seam Roof Repair", "Completed", "Conch Republic Contractors")
                )
            ),
            HousePlot(
                address = "505 Bayshore Blvd, Tampa FL",
                latitude = 27.9350,
                longitude = -82.4630,
                ownerName = "Dr. Laura Vance",
                primaryLanguage = "English",
                tenureStatus = "Homeowner",
                incomeBracket = "$250k+",
                gender = "Female",
                maritalStatus = "Married",
                yearBuilt = "2018-2022",
                estimatedValue = "$1.2M+",
                squareFeet = "3,500-4,000 sf",
                occupants = 4,
                creditScore = "820-850",
                initialRoofAge = 4,
                permits = listOf(
                    PersonalPermit("PMT-2022-00109", "04/01/2022", "Roof & Canopy Installation", "Completed", "Tampa Elite Roofing")
                )
            ),
            HousePlot(
                address = "400 W Church St, Orlando FL",
                latitude = 28.5390,
                longitude = -81.3830,
                ownerName = "Marcus Aurelius",
                primaryLanguage = "English",
                tenureStatus = "Homeowner",
                incomeBracket = "$120k-$130k",
                gender = "Male",
                maritalStatus = "Married",
                yearBuilt = "2005-2009",
                estimatedValue = "$320k-$360k",
                squareFeet = "1,400-1,600 sf",
                occupants = 2,
                creditScore = "760-799",
                initialRoofAge = 21,
                permits = emptyList()
            ),
            HousePlot(
                address = "120 Las Olas Blvd, Fort Lauderdale FL",
                latitude = 26.1190,
                longitude = -80.1400,
                ownerName = "Sophia Loren",
                primaryLanguage = "Italian / English",
                tenureStatus = "Homeowner",
                incomeBracket = "$150k-$180k",
                gender = "Female",
                maritalStatus = "Married",
                yearBuilt = "1998-2002",
                estimatedValue = "$750k-$850k",
                squareFeet = "2,200-2,600 sf",
                occupants = 3,
                creditScore = "780-820",
                initialRoofAge = 3,
                permits = listOf(
                    PersonalPermit("PMT-2023-01004", "08/04/2023", "Complete Metal Tile Overcoat", "Completed", "Fort Lauderdale Roofing Specialists")
                )
            ),
            HousePlot(
                address = "1200 S Ocean Blvd, Palm Beach FL",
                latitude = 26.7050,
                longitude = -80.0360,
                ownerName = "Donald Vanderbilt",
                primaryLanguage = "English",
                tenureStatus = "Homeowner",
                incomeBracket = "$500k+",
                gender = "Male",
                maritalStatus = "Married",
                yearBuilt = "1980-1984",
                estimatedValue = "$3M+",
                squareFeet = "5,000-6,500 sf",
                occupants = 5,
                creditScore = "840-850",
                initialRoofAge = 11,
                permits = listOf(
                    PersonalPermit("PMT-2015-08492", "06/05/2015", "Spanish Tile Roof Replacement", "Completed", "Vanderbilt Premium Roofing")
                )
            ),
            HousePlot(
                address = "150 Cocoa Beach Blvd, Cocoa Beach FL",
                latitude = 28.3200,
                longitude = -80.6080,
                ownerName = "Major Nelson",
                primaryLanguage = "English",
                tenureStatus = "Homeowner",
                incomeBracket = "$95k-$110k",
                gender = "Male",
                maritalStatus = "Married",
                yearBuilt = "1960-1970",
                estimatedValue = "$420k-$460k",
                squareFeet = "1,300-1,500 sf",
                occupants = 3,
                creditScore = "710-739",
                initialRoofAge = 25,
                permits = emptyList()
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // High-contrast clean stats grid row matching tailwind grid-cols-3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "TOTAL MARKED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "$allLeadsCount",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "FILTER SIZE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${leads.size}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val pendingCount = leads.count { !it.isSynced }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "SYNC LAG",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (pendingCount == 0) Color(0xFF2E7D32) else Color(0xFFC62828), CircleShape)
                        )
                        Text(
                            text = if (pendingCount == 0) "Synced" else "$pendingCount lag",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Drop-down quick territory filtering choices
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedTerritoryId == null,
                onClick = { onTerritorySelected(null) },
                label = { Text("All Sectors", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(14.dp)) }
            )

            territories.forEach { terr ->
                val isSelected = selectedTerritoryId == terr.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onTerritorySelected(terr.id) },
                    label = { Text(terr.name, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(android.graphics.Color.parseColor(terr.colorHex)), CircleShape)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (useLeafletMap) {
                        userLocationCenterTrigger = userLocationCenterTrigger + 1
                    } else {
                        mapOffsetX = 0f
                        mapOffsetY = 0f
                        mapScale = 1.0f
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Center Map",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Find My Loc",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Map View Selection Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Engine:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

            ElevatedFilterChip(
                selected = !useLeafletMap,
                onClick = { useLeafletMap = false },
                label = { Text("Grid Canvas", fontSize = 10.sp) },
                modifier = Modifier.height(28.dp).testTag("grid_canvas_chip")
            )

            ElevatedFilterChip(
                selected = useLeafletMap && selectedMapLayer == "street",
                onClick = {
                    useLeafletMap = true
                    selectedMapLayer = "street"
                },
                label = { Text("Street Map", fontSize = 10.sp) },
                modifier = Modifier.height(28.dp).testTag("street_map_chip")
            )

            ElevatedFilterChip(
                selected = useLeafletMap && selectedMapLayer == "satellite",
                onClick = {
                    useLeafletMap = true
                    selectedMapLayer = "satellite"
                },
                label = { Text("Satellite", fontSize = 10.sp) },
                modifier = Modifier.height(28.dp).testTag("satellite_map_chip")
            )
        }

        // Status Filter Row
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Text("Filters:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            
            val statusDisplayNames = mapOf(
                "NOT_HOME" to "Not Home",
                "WARM_LEAD" to "Warm Lead",
                "CUSTOMER" to "Closed Sale",
                "REFUSED" to "Not Int.",
                "GO_BACK" to "Follow Up"
            )
            
            items(statusDisplayNames.size) { index ->
                val status = statusDisplayNames.keys.elementAt(index)
                val isSelected = selectedStatusFilters.contains(status)
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            selectedStatusFilters.remove(status)
                        } else {
                            selectedStatusFilters.add(status)
                        }
                    },
                    label = { Text(statusDisplayNames[status] ?: status, fontSize = 10.sp) },
                    modifier = Modifier.height(28.dp).testTag("filter_chip_$status")
                )
            }
        }

        // Help Indicator Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (useLeafletMap)
                        "Leaflet: Double-tap or long-press map to drop pin. Tap markers for detail drawers."
                    else
                        "Interactive Mesh: Swipe to pan. Double-tap parcels to drop lead pins.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // INTERACTIVE MAP CONTAINER
        val mapBoxModifier = if (useLeafletMap) {
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
        } else {
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        mapScale = (mapScale * zoom).coerceIn(0.3f, 5.0f)
                        mapOffsetX += pan.x
                        mapOffsetY += pan.y
                    }
                }
                .pointerInput(Unit) {
                    this.detectTapGestures(
                        onDoubleTap = { offset ->
                            val localX = (offset.x - size.width / 2 - mapOffsetX) / mapScale
                            val localY = (offset.y - size.height / 2 - mapOffsetY) / mapScale
                            val lat = getLatFromCanvasY(localY)
                            val lng = getLngFromCanvasX(localX)
                            pendingAddCoordinates = Offset(localX, localY)
                            pendingLatitude = null
                            pendingLongitude = null
                            pendingAddressSuggestion = "Florida GPS Parcel (Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)})"
                            showAddDialog = true
                        },
                        onTap = { offset ->
                            val localX = (offset.x - size.width / 2 - mapOffsetX) / mapScale
                            val localY = (offset.y - size.height / 2 - mapOffsetY) / mapScale

                            // 1. Check if tapped an existing Lead Pin
                            val tappedLead = leads.find { lead ->
                                val ldX = getCanvasX(lead.longitude)
                                val ldY = getCanvasY(lead.latitude)
                                hypot(localX - ldX, localY - ldY) < 25f
                            }

                            if (tappedLead != null) {
                                selectedLeadDetail = tappedLead
                                selectedHousePlot = staticHousePlots.find { it.address.equals(tappedLead.address, ignoreCase = true) }
                                return@detectTapGestures
                            }

                            // 2. Check if tapped a static parcel/house plot
                            val tappedHouse = staticHousePlots.find { house ->
                                val houseX = getCanvasX(house.longitude)
                                val houseY = getCanvasY(house.latitude)
                                val left = houseX - house.width / 2
                                val right = houseX + house.width / 2
                                val top = houseY - house.height / 2
                                val bottom = houseY + house.height / 2
                                localX in left..right && localY in top..bottom
                            }

                            if (tappedHouse != null) {
                                selectedHousePlot = tappedHouse
                                permitsPulled[tappedHouse.address] = true
                                val leadAtHouse = leads.find { it.address.equals(tappedHouse.address, ignoreCase = true) }
                                if (leadAtHouse != null) {
                                    selectedLeadDetail = leadAtHouse
                                } else {
                                    val tempNotes = "Dropped pin via instant home mapping. Roof age verified: ${tappedHouse.initialRoofAge} years old based on Florida county permit records."
                                    onAddLead(
                                        tappedHouse.ownerName,
                                        tappedHouse.address,
                                        "WARM_LEAD",
                                        tappedHouse.latitude,
                                        tappedHouse.longitude,
                                        tempNotes,
                                        tappedHouse.phone,
                                        selectedTerritoryId
                                    )
                                    selectedLeadDetail = Lead(
                                        name = tappedHouse.ownerName,
                                        address = tappedHouse.address,
                                        status = "WARM_LEAD",
                                        latitude = tappedHouse.latitude,
                                        longitude = tappedHouse.longitude,
                                        notes = tempNotes,
                                        phone = tappedHouse.phone,
                                        territoryId = selectedTerritoryId
                                    )
                                }
                            } else {
                                selectedHousePlot = null
                                selectedLeadDetail = null
                            }
                        }
                    )
                }
        }

        Box(modifier = mapBoxModifier) {
            if (useLeafletMap) {
                LeafletMapView(
                    leads = leads,
                    staticHousePlots = staticHousePlots,
                    centerLat = userLatitude,
                    centerLng = userLongitude,
                    isDarkTheme = isSystemDark,
                    mapStyle = selectedMapLayer,
                    onMapLongClick = { lat, lng ->
                        pendingAddCoordinates = null
                        pendingLatitude = lat
                        pendingLongitude = lng
                        pendingAddressSuggestion = "Florida GPS Pin (Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)})"
                        showAddDialog = true
                    },
                    onLeadSelect = { leadId ->
                        val matchingLead = leads.find { it.id == leadId }
                        if (matchingLead != null) {
                            selectedLeadDetail = matchingLead
                            selectedHousePlot = staticHousePlots.find { it.address.equals(matchingLead.address, ignoreCase = true) }
                        }
                    },
                    onHomeSelect = { address ->
                        val foundHouse = staticHousePlots.find { it.address.equals(address, ignoreCase = true) }
                        if (foundHouse != null) {
                            selectedHousePlot = foundHouse
                            permitsPulled[foundHouse.address] = true
                            val leadAtHouse = leads.find { it.address.equals(foundHouse.address, ignoreCase = true) }
                            if (leadAtHouse != null) {
                                selectedLeadDetail = leadAtHouse
                            } else {
                                val tempNotes = "Dropped pin via instant home mapping. Roof age verified: ${foundHouse.initialRoofAge} years old based on Florida county permit records."
                                onAddLead(
                                    foundHouse.ownerName,
                                    foundHouse.address,
                                    "WARM_LEAD",
                                    foundHouse.latitude,
                                    foundHouse.longitude,
                                    tempNotes,
                                    foundHouse.phone,
                                    selectedTerritoryId
                                )
                                selectedLeadDetail = Lead(
                                    name = foundHouse.ownerName,
                                    address = foundHouse.address,
                                    status = "WARM_LEAD",
                                    latitude = foundHouse.latitude,
                                    longitude = foundHouse.longitude,
                                    notes = tempNotes,
                                    phone = foundHouse.phone,
                                    territoryId = selectedTerritoryId
                                )
                            }
                        }
                    },
                    onAddOfflineLeads = { offlineLeads ->
                        offlineLeads.forEach { lead ->
                            viewModel.addLead(
                                name = lead.name,
                                address = lead.address,
                                status = lead.status,
                                latitude = lead.latitude,
                                longitude = lead.longitude,
                                notes = lead.notes,
                                phone = lead.phone,
                                territoryId = selectedTerritoryId
                            )
                        }
                        viewModel.syncWithBackend()
                    },
                    modifier = Modifier.fillMaxSize().testTag("leaflet_web_view_map")
                )
            } else {
                val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                val textPaintColor = MaterialTheme.colorScheme.onSurface

                Canvas(modifier = Modifier.fillMaxSize().testTag("interactive_canvas_map")) {
                    val cx = size.width / 2
                    val cy = size.height / 2

                    // Coordinate origin translation
                    translate(cx + mapOffsetX, cy + mapOffsetY) {
                        scale(mapScale, mapScale, pivot = Offset.Zero) {
                    
                    // A. Draw Florida Peninsula Contour Path
                    val floridaPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(getCanvasX(-87.5), getCanvasY(31.0))
                        lineTo(getCanvasX(-81.5), getCanvasY(31.0))
                        lineTo(getCanvasX(-81.4), getCanvasY(30.7)) // Jacksonville
                        lineTo(getCanvasX(-80.5), getCanvasY(28.5)) // Cocoa Beach area
                        lineTo(getCanvasX(-80.0), getCanvasY(26.8)) // Palm Beach
                        lineTo(getCanvasX(-80.1), getCanvasY(25.8)) // Miami
                        lineTo(getCanvasX(-80.4), getCanvasY(25.0)) // Key Largo
                        lineTo(getCanvasX(-81.8), getCanvasY(24.5)) // Key West
                        lineTo(getCanvasX(-81.1), getCanvasY(25.1)) // Cape Sable
                        lineTo(getCanvasX(-81.7), getCanvasY(26.0)) // Naples
                        lineTo(getCanvasX(-82.6), getCanvasY(27.8)) // Tampa Bay
                        quadraticBezierTo(
                            getCanvasX(-83.5), getCanvasY(29.8),
                            getCanvasX(-84.0), getCanvasY(30.1) // Apalachee Bay
                        )
                        lineTo(getCanvasX(-87.5), getCanvasY(30.5)) // Pensacola
                        close()
                    }

                    // Background Water Body (Azure Ocean)
                    drawRect(
                        color = if (isSystemDark) Color(0xFF0F1B29) else Color(0xFFECF5FE),
                        topLeft = Offset(-2000f, -2000f),
                        size = Size(4000f, 4000f)
                    )

                    // Landmass filled (Sands/Forest dynamic theme tints)
                    drawPath(
                        path = floridaPath,
                        color = if (isSystemDark) Color(0xFF202C39) else Color(0xFFE6EFE4),
                    )
                    // Landmass coastal boundaries
                    drawPath(
                        path = floridaPath,
                        color = if (isSystemDark) Color(0xFF32475C) else Color(0xFFC2D59B),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                        )
                    )

                    // 1. Draw Grid lines (Earth Maps Projection Mesh)
                    val dotPaintColor = textPaintColor.copy(alpha = 0.08f)
                    for (x in -1500..1500 step 120) {
                        drawLine(
                            color = dotPaintColor,
                            start = Offset(x.toFloat(), -1500f),
                            end = Offset(x.toFloat(), 1500f),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 15f))
                        )
                    }
                    for (y in -1500..1500 step 120) {
                        drawLine(
                            color = dotPaintColor,
                            start = Offset(-1500f, y.toFloat()),
                            end = Offset(1500f, y.toFloat()),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 15f))
                        )
                    }

                    // 2. DRAW ROADS (Interstate Route Grids - Florida High-Speed Corridors)
                    val expressPaths = listOf(
                        // US-1 Coastal Scenic Highway down the Florida Keys
                        listOf(
                            Offset(getCanvasX(-80.13), getCanvasY(25.77)),
                            Offset(getCanvasX(-80.44), getCanvasY(25.08)),
                            Offset(getCanvasX(-81.78), getCanvasY(24.55))
                        ),
                        // I-95 East Coast Highway
                        listOf(
                            Offset(getCanvasX(-80.13), getCanvasY(25.77)),
                            Offset(getCanvasX(-80.14), getCanvasY(26.12)),
                            Offset(getCanvasX(-80.04), getCanvasY(26.70)),
                            Offset(getCanvasX(-80.60), getCanvasY(28.32)),
                            Offset(getCanvasX(-81.40), getCanvasY(30.70))
                        ),
                        // I-4 Tampa-Orlando Pathway
                        listOf(
                            Offset(getCanvasX(-82.45), getCanvasY(27.95)),
                            Offset(getCanvasX(-81.37), getCanvasY(28.53)),
                            Offset(getCanvasX(-80.60), getCanvasY(28.32))
                        )
                    )

                    val expresswayColor = if (isSystemDark) Color(0xFFF9A825).copy(alpha = 0.35f) else Color(0xFFFFCC80)
                    expressPaths.forEach { points ->
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = expresswayColor,
                                start = points[i],
                                end = points[i+1],
                                strokeWidth = 5f
                            )
                        }
                    }

                    // Draw geographic site bubbles
                    val landmarkColor = textPaintColor.copy(alpha = 0.25f)
                    val landmarks = listOf(
                        "Miami Coast" to Offset(getCanvasX(-80.13), getCanvasY(25.77)),
                        "Tampa Beach" to Offset(getCanvasX(-82.45), getCanvasY(27.95)),
                        "Orlando Lakes" to Offset(getCanvasX(-81.37), getCanvasY(28.53)),
                        "Florida Keys" to Offset(getCanvasX(-81.78), getCanvasY(24.55)),
                        "Jacksonville" to Offset(getCanvasX(-81.40), getCanvasY(30.70))
                    )
                    landmarks.forEach { (_, coord) ->
                        drawCircle(
                            color = landmarkColor,
                            radius = 6f,
                            center = coord
                        )
                        drawLine(
                            color = landmarkColor,
                            start = coord,
                            end = Offset(coord.x, coord.y + 10f),
                            strokeWidth = 2f
                        )
                    }

                    // 3. DRAW HOUSE PLOTS (Earth parcels)
                    staticHousePlots.forEach { house ->
                        val matchingLead = leads.find { it.address.equals(house.address, ignoreCase = true) }
                        
                        val fillFillColor = when (matchingLead?.status) {
                            "WARM_LEAD" -> Color(0xFF1565C0).copy(alpha = 0.35f)
                            "GO_BACK" -> Color(0xFFF57C00).copy(alpha = 0.35f)
                            "NOT_HOME" -> Color(0xFF757575).copy(alpha = 0.35f)
                            "CUSTOMER" -> Color(0xFF2E7D32).copy(alpha = 0.35f)
                            "REFUSED" -> Color(0xFFC62828).copy(alpha = 0.35f)
                            else -> Color.Transparent
                        }

                        val strokeStrokeColor = when (matchingLead?.status) {
                            "WARM_LEAD" -> Color(0xFF1565C0)
                            "GO_BACK" -> Color(0xFFF57C00)
                            "NOT_HOME" -> Color(0xFF757575)
                            "CUSTOMER" -> Color(0xFF2E7D32)
                            "REFUSED" -> Color(0xFFC62828)
                            else -> outlineColor
                        }

                        val hX = getCanvasX(house.longitude)
                        val hY = getCanvasY(house.latitude)

                        // Draw Parcel Outer boundary representing open API GIS coordinates lines
                        drawRect(
                            color = strokeStrokeColor.copy(alpha = 0.08f),
                            topLeft = Offset(hX - house.width / 2 - 8, hY - house.height / 2 - 8),
                            size = Size(house.width + 16, house.height + 16)
                        )

                        // Draw House parcel structure
                        drawRect(
                            color = fillFillColor,
                            topLeft = Offset(hX - house.width / 2, hY - house.height / 2),
                            size = Size(house.width, house.height)
                        )
                        drawRect(
                            color = strokeStrokeColor,
                            topLeft = Offset(hX - house.width / 2, hY - house.height / 2),
                            size = Size(house.width, house.height),
                            style = Stroke(width = if (matchingLead != null) 3.dp.toPx() else 1.dp.toPx())
                        )

                        // Chimney Grouping
                        drawCircle(
                            color = strokeStrokeColor,
                            radius = 6f,
                            center = Offset(hX - house.width / 3, hY - house.height / 3),
                            style = Stroke(2f)
                        )
                    }

                    // 4. DRAW LEAD PINS
                    leads.forEach { lead ->
                        val ldX = getCanvasX(lead.longitude)
                        val ldY = getCanvasY(lead.latitude)

                        val pinColor = when (lead.status) {
                            "WARM_LEAD" -> Color(0xFF1565C0)
                            "GO_BACK" -> Color(0xFFF57C00)
                            "NOT_HOME" -> Color(0xFF757575)
                            "CUSTOMER" -> Color(0xFF2E7D32)
                            "REFUSED" -> Color(0xFFC62828)
                            else -> Color.DarkGray
                        }

                        // Aura
                        drawCircle(
                            color = pinColor.copy(alpha = 0.25f),
                            radius = 24f,
                            center = Offset(ldX, ldY)
                        )

                        // Inner solid pin
                        drawCircle(
                            color = pinColor,
                            radius = 11f,
                            center = Offset(ldX, ldY)
                        )

                        drawCircle(
                            color = Color.White,
                            radius = 5f,
                            center = Offset(ldX, ldY)
                        )

                        // Stem to point accurately over target coastline
                        drawLine(
                            color = pinColor,
                            start = Offset(ldX, ldY),
                            end = Offset(ldX, ldY + 16f),
                            strokeWidth = 3f
                        )
                    }

                    // 5. DRAW USER PULSING PIN / ACCURACY DOT
                    val uLat = 28.3200
                    val uLng = -80.6080
                    val uX = getCanvasX(uLng)
                    val uY = getCanvasY(uLat)

                    // Draw accuracy pulse ring
                    drawCircle(
                        color = Color(0xFF1976D2).copy(alpha = pulseAlpha),
                        radius = pulseRadius,
                        center = Offset(uX, uY)
                    )

                    // Draw inner white border
                    drawCircle(
                        color = Color.White,
                        radius = 9f,
                        center = Offset(uX, uY)
                    )

                    // Draw inner primary blue core
                    drawCircle(
                        color = Color(0xFF1976D2),
                        radius = 6f,
                        center = Offset(uX, uY)
                    )
                }
                }
            }
            }

            // Nearby Leads Overlay
            val nearbyLeads = leads.filter { calculateDistanceMiles(userLatitude, userLongitude, it.latitude, it.longitude) <= 0.5 }
            if (nearbyLeads.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Nearby Leads (< 0.5 mi)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        nearbyLeads.take(5).forEach { lead ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(0.35f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dotColor = when (lead.status) {
                                    "WARM_LEAD" -> Color(0xFF1565C0)
                                    "GO_BACK" -> Color(0xFFF57C00)
                                    "NOT_HOME" -> Color(0xFF757575)
                                    "CUSTOMER" -> Color(0xFF2E7D32)
                                    "REFUSED" -> Color(0xFFC62828)
                                    else -> Color.DarkGray
                                }
                                Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = lead.address.take(18) + (if (lead.address.length > 18) "..." else ""),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Legend indicators
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Rep Legend", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF2E7D32), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Customer / Closed", fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF1565C0), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Warm Lead", fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFF57C00), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Go Back / Callback", fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF757575), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Not Home", fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFC62828), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refused / Out", fontSize = 10.sp)
                }
            }

            // FLOATING ACTION CORES (Zoom & GPS Position Tracking)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Find Me button (crosshair style)
                val scope = rememberCoroutineScope()
                FloatingActionButton(
                    onClick = {
                        // Smoothly center the map on the user's active GPS coordinate and zoom in!
                        scope.launch {
                            mapOffsetX = 0f
                            mapOffsetY = 0f
                            mapScale = 2.5f
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp).testTag("gps_find_me_button")
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Find Where You Are",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Zoom In Zoom Out Cores
                FloatingActionButton(
                    onClick = { mapScale = (mapScale + 0.3f).coerceAtMost(5.0f) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(44.dp).testTag("zoom_in_button")
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                }

                FloatingActionButton(
                    onClick = { mapScale = (mapScale - 0.3f).coerceAtLeast(0.3f) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(44.dp).testTag("zoom_out_button")
                ) {
                    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                        Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // SLIDE-UP D2D HOUSING & PERMITS DRAWER
            androidx.compose.animation.AnimatedVisibility(
                visible = selectedHousePlot != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                selectedHousePlot?.let { house ->
                    val scope = rememberCoroutineScope()
                    val hasPulled = permitsPulled[house.address] ?: false
                    val isPreparing = isPullingAddress == house.address

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clickable(enabled = false) {}, // Intercept map touches
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Top Drag Handle & Close Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Address rating star / diamond handle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Yellow / Orange Badge representation of custom roof age code
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color = if (hasPulled) {
                                                    if (house.initialRoofAge > 15) Color(0xFFFFB300) else Color(0xFF2E7D32)
                                                } else {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (hasPulled) "${house.initialRoofAge}" else "?",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = if (hasPulled) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Column(modifier = Modifier.fillMaxWidth(0.65f)) {
                                        Text(
                                            text = house.address,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "GPS Coordinates: ${String.format("%.4f", house.latitude)}, ${String.format("%.4f", house.longitude)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        selectedHousePlot = null
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close Panel",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Grid of property metrics (Year Built, Estimated Value, SQ Foot, Active status)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Metric 1: Year Built
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.DateRange, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("BUILT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text(house.yearBuilt, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }

                                // Metric 2: Estimated Value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Home, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("EST VALUE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text(house.estimatedValue, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }

                                // Metric 3: Square Feet
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("SQ FT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Text(house.squareFeet, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Metric 4: Roof Age
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (hasPulled) {
                                                if (house.initialRoofAge > 15) Color(0xFFFFF9C4) else Color(0xFFE8F5E9)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            },
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Build, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ROOF AGE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Text(
                                            text = if (hasPulled) "${house.initialRoofAge} Years Old" else "Unverified (?)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (hasPulled) {
                                                if (house.initialRoofAge > 15) Color(0xFFF57C00) else Color(0xFF2E7D32)
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }

                                // Metric 5: Occupants
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("OCCUPANTS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Text("${house.occupants} Residents", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }

                                // Metric 6: Credit Score
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Favorite, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("CREDIT SCORE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Text(house.creditScore, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Homeowner Details Header & Badge Chips
                            Text(
                                text = "Homeowner: ${house.ownerName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Phone icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = house.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = house.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Badges flow row representation
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    house.primaryLanguage to MaterialTheme.colorScheme.secondaryContainer,
                                    house.tenureStatus to MaterialTheme.colorScheme.tertiaryContainer,
                                    house.incomeBracket to MaterialTheme.colorScheme.primaryContainer,
                                    house.gender to MaterialTheme.colorScheme.surfaceVariant,
                                    house.maritalStatus to MaterialTheme.colorScheme.surfaceVariant
                                ).forEach { (label, containerColor) ->
                                    Surface(
                                        color = containerColor,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive Local Permit & Roofing History Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (!hasPulled && !isPreparing) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Local Florida Permit History",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Pull public tax & building records to reveal verified roof permit dates & solar upgrades.",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        isPullingAddress = house.address
                                                        pullProgressText = "Requesting Florida GIS coordinates..."
                                                        delay(700)
                                                        pullProgressText = "Opening county building inspection index..."
                                                        delay(700)
                                                        pullProgressText = "Querying residential structural permits..."
                                                        delay(700)
                                                        pullProgressText = "Finished! Compiling structural roof logs..."
                                                        delay(500)
                                                        permitsPulled[house.address] = true
                                                        isPullingAddress = null
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Pull Permits", fontSize = 11.sp)
                                            }
                                        }
                                    } else if (isPreparing) {
                                        // Loading Progress Indicators
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.5.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Column {
                                                Text(
                                                    text = "Contacting Local Municipality...",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = pullProgressText,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    } else {
                                        // Display verified permits!
                                        Text(
                                            text = "Verified Municipal Records Florida",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (house.permits.isEmpty()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(14.dp))
                                                    Text(
                                                        text = "No roofing replacement permits found on record since construction. Roof age is original (${house.initialRoofAge} years). Potential lead opportunity!",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        } else {
                                            house.permits.forEach { permit ->
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "• ${permit.permitType}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = permit.date,
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                    Text(
                                                        text = "Permit: ${permit.permitNum} | Status: ${permit.status} | Contractor: ${permit.contractor}",
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.outline,
                                                        modifier = Modifier.padding(start = 10.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive Status Setter D2D Grid Buttons mapping directly to Lead Dropping!
                            Text(
                                text = "Drop / Update Lead Status Check",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val leadAtHouse = leads.find { it.address.equals(house.address, ignoreCase = true) }
                                val activeStatus = leadAtHouse?.status

                                listOf(
                                    Triple("LEAD", "WARM_LEAD", Color(0xFF1565C0)),
                                    Triple("DK1", "NOT_HOME", Color(0xFFFFA000)),
                                    Triple("DK2", "NOT_HOME", Color(0xFFFF8F00)),
                                    Triple("DK3", "NOT_HOME", Color(0xFFFF6F00)),
                                    Triple("GB", "GO_BACK", Color(0xFFE65100)),
                                    Triple("INS", "CUSTOMER", Color(0xFF2E7D32)),
                                    Triple("NI", "REFUSED", Color(0xFFC62828))
                                ).forEach { (label, statusValue, buttonColor) ->
                                    val isSelected = activeStatus == statusValue && (
                                        if (label.startsWith("DK")) {
                                            leadAtHouse.notes.contains(label) || (label == "DK1" && (!leadAtHouse.notes.contains("DK")))
                                        } else true
                                    )

                                    ElevatedButton(
                                        onClick = {
                                            val notesText = when {
                                                label.startsWith("DK") -> "Door Knock attempt ($label). Primary details retrieved."
                                                else -> "Status logged as $label via D2D quick mapping."
                                            }
                                            if (leadAtHouse != null) {
                                                onUpdateLead(
                                                    leadAtHouse.copy(
                                                        status = statusValue,
                                                        notes = notesText,
                                                        updatedAt = System.currentTimeMillis()
                                                    )
                                                )
                                            } else {
                                                onAddLead(
                                                    house.ownerName,
                                                    house.address,
                                                    statusValue,
                                                    house.latitude,
                                                    house.longitude,
                                                    notesText,
                                                    "",
                                                    selectedTerritoryId
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.elevatedButtonColors(
                                            containerColor = if (isSelected) buttonColor else buttonColor.copy(alpha = 0.08f),
                                            contentColor = if (isSelected) Color.White else buttonColor
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG: ADD/DROP LEAD PIN
    // ==========================================
    if (showAddDialog && (pendingAddCoordinates != null || (pendingLatitude != null && pendingLongitude != null))) {
        val dialogContext = LocalContext.current
        LaunchedEffect(pendingLatitude, pendingLongitude, pendingAddCoordinates) {
            val resolvedLat = pendingLatitude ?: getLatFromCanvasY(pendingAddCoordinates!!.y)
            val resolvedLng = pendingLongitude ?: getLngFromCanvasX(pendingAddCoordinates!!.x)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val geocoder = android.location.Geocoder(dialogContext, java.util.Locale.getDefault())
                    val addresses = geocoder.getFromLocation(resolvedLat, resolvedLng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val county = addresses[0].subAdminArea ?: addresses[0].adminArea // subAdminArea is usually county
                        if (!county.isNullOrBlank()) {
                            pendingCounty = county
                        } else {
                            pendingCounty = "Unknown County"
                        }
                    } else {
                         pendingCounty = "Unknown County"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    pendingCounty = "County lookup failed"
                }
            }
        }

        var ownerName by remember { mutableStateOf("") }
        var contactPhone by remember { mutableStateOf("") }
        var inputAddress by remember { mutableStateOf(pendingAddressSuggestion) }
        var selectedStatus by remember { mutableStateOf("NOT_HOME") }
        var leadNotes by remember { mutableStateOf("") }
        var assignedTerritoryId by remember { mutableStateOf<Long?>(selectedTerritoryId) }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("add_lead_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Drop New Lead Pin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (pendingCounty != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Suggested County: $pendingCounty", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Homeowner Name(s)") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        placeholder = { Text("e.g. Smith Family") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_lead_owner_name")
                    )

                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Contact Phone") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        placeholder = { Text("e.g. 555-0199") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("add_lead_phone")
                    )

                    OutlinedTextField(
                        value = inputAddress,
                        onValueChange = { inputAddress = it },
                        label = { Text("Street Address") },
                        leadingIcon = { Icon(Icons.Default.Home, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_lead_address")
                    )

                    // Visit Status selection row
                    Text("Current Visit Status", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "NOT_HOME" to ("NH" to Color(0xFF757575)),
                            "WARM_LEAD" to ("Warm" to Color(0xFF1565C0)),
                            "GO_BACK" to ("Return" to Color(0xFFF57C00)),
                            "CUSTOMER" to ("Sold" to Color(0xFF2E7D32)),
                            "REFUSED" to ("Out" to Color(0xFFC62828))
                        ).forEach { (statusKey, displayTuple) ->
                            val (displayText, highlightColor) = displayTuple
                            val isSelected = selectedStatus == statusKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) highlightColor else highlightColor.copy(alpha = 0.12f)
                                    )
                                    .clickable { selectedStatus = statusKey }
                                    .padding(vertical = 10.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayText,
                                    color = if (isSelected) Color.White else highlightColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = leadNotes,
                        onValueChange = { leadNotes = it },
                        label = { Text("Rep Notes") },
                        placeholder = { Text("solar details, check qualifications, etc.") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("add_lead_notes")
                    )

                    Text("Associate Territory Line", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        territories.forEach { terr ->
                            val isSelected = assignedTerritoryId == terr.id
                            Surface(
                                selected = isSelected,
                                onClick = { assignedTerritoryId = terr.id },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = terr.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val resolvedLat = pendingLatitude ?: getLatFromCanvasY(pendingAddCoordinates!!.y)
                                val resolvedLng = pendingLongitude ?: getLngFromCanvasX(pendingAddCoordinates!!.x)
                                onAddLead(
                                    ownerName,
                                    inputAddress,
                                    selectedStatus,
                                    resolvedLat,
                                    resolvedLng,
                                    leadNotes,
                                    contactPhone,
                                    assignedTerritoryId
                                )
                                showAddDialog = false
                            },
                            modifier = Modifier.testTag("save_lead_button")
                        ) {
                            Text("Save Pin")
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // SHEET/DIALOG: UPDATE DETAILS & REAL AI DOOR PITCH (GEMINI)
    // ==========================================
    if (selectedLeadDetail != null) {
        val lead = selectedLeadDetail!!
        var editName by remember(lead) { mutableStateOf(lead.name) }
        var editPhone by remember(lead) { mutableStateOf(lead.phone) }
        var editAddress by remember(lead) { mutableStateOf(lead.address) }
        var editStatus by remember(lead) { mutableStateOf(lead.status) }
        var editNotes by remember(lead) { mutableStateOf(lead.notes) }
        var editTerritoryId by remember(lead) { mutableStateOf(lead.territoryId) }

        val aiPitch by viewModel.generatedPitch.collectAsStateWithLifecycle()
        val isGeneratingPitch by viewModel.isGeneratingPitch.collectAsStateWithLifecycle()

        Dialog(onDismissRequest = {
            viewModel.clearGeneratedPitch()
            selectedLeadDetail = null
        }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("lead_details_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScrollEnabled()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Household Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row {
                            if (!lead.isSynced) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "Offline Edit", 
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            IconButton(onClick = {
                                onDeleteLead(lead)
                                viewModel.clearGeneratedPitch()
                                selectedLeadDetail = null
                            }) {
                                Icon(Icons.Default.Delete, "Delete House Pin", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Homeowner Name(s)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Contact Phone") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("edit_lead_phone")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Street Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Visit Status Highlight", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "NOT_HOME" to ("NH" to Color(0xFF757575)),
                            "WARM_LEAD" to ("Warm" to Color(0xFF1565C0)),
                            "GO_BACK" to ("Return" to Color(0xFFF57C00)),
                            "CUSTOMER" to ("Sold" to Color(0xFF2E7D32)),
                            "REFUSED" to ("Out" to Color(0xFFC62828))
                        ).forEach { (statusKey, displayTuple) ->
                            val (displayText, highlightColor) = displayTuple
                            val isSelected = editStatus == statusKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) highlightColor else highlightColor.copy(alpha = 0.12f)
                                    )
                                    .clickable { editStatus = statusKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayText,
                                    color = if (isSelected) Color.White else highlightColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Visit Notes") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // GEMINI AI INTEGRATION CARD
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star, // Changed AutoAwesome to Star
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AI Opener Pitch Generator",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Button(
                                    onClick = {
                                        viewModel.generatePitch(
                                            address = editAddress,
                                            name = editName,
                                            status = editStatus,
                                            notes = editNotes
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Ask", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isGeneratingPitch) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Gemini is composing custom pitch...", fontSize = 11.sp, style = MaterialTheme.typography.bodySmall)
                                }
                            } else if (aiPitch != null) {
                                Text(
                                    text = aiPitch!!,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                Text(
                                    text = "Ready to generate a personalized door-opener script tailored to this resident's notes and sector status.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            viewModel.clearGeneratedPitch()
                            selectedLeadDetail = null
                        }) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                val updatedLead = lead.copy(
                                    name = editName,
                                    phone = editPhone,
                                    address = editAddress,
                                    status = editStatus,
                                    notes = editNotes,
                                    territoryId = editTerritoryId
                                )
                                onUpdateLead(updatedLead)
                                viewModel.clearGeneratedPitch()
                                selectedLeadDetail = null
                            },
                            modifier = Modifier.testTag("apply_edit_lead_button")
                        ) {
                            Text("Apply Changes")
                        }
                    }
                }
            }
        }
    }
}

// Extension to color-fill conditional
fun Color.ifElse(condition: Color, alternate: Color): Color =
    if (this == alternate) Color.Transparent else this

// Simplified scrolling helper modifier for Dialog column scrolling
@Composable
fun Modifier.verticalScrollEnabled(): Modifier =
    this.then(
        @Suppress("DEPRECATION")
        androidx.compose.foundation.rememberScrollState().let { scrollState ->
            this.verticalScroll(scrollState)
        }
    )

// ==========================================
// 2. TERRITORIES MANAGER SCREEN
// ==========================================

@Composable
fun TerritoryManagerScreen(
    territories: List<Territory>,
    leadsCountMap: Map<Long?, Int>,
    onAddTerritory: (String, String, String) -> Unit,
    onDeleteTerritory: (Territory) -> Unit
) {
    var showCreateTerritory by remember { mutableStateOf(false) }

    var newTName by remember { mutableStateOf("") }
    var newTDesc by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#1565C0") }

    val availableColors = listOf(
        "#1565C0",
        "#2E7D32",
        "#D84315",
        "#AD1457",
        "#6A1B9A",
        "#FF8F00"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Manual Territories",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage custom sector lines and map groupings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { showCreateTerritory = true },
                modifier = Modifier.testTag("add_territory_button")
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Draw")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (territories.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Menu, // Changed LayersClear to Menu
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Sectors Custom Defined", fontWeight = FontWeight.Bold)
                    Text("Draw dynamic sector groups to bucket different leads.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("territories_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(territories) { terr ->
                    val color = Color(android.graphics.Color.parseColor(terr.colorHex))
                    val pinsInside = leadsCountMap[terr.id] ?: 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = terr.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = terr.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn, // Changed PinDrop to LocationOn
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$pinsInside households active",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteTerritory(terr) }) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE TERRITORY DIALOG
    if (showCreateTerritory) {
        Dialog(onDismissRequest = { showCreateTerritory = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("create_territory_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Create Territory Boundary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = newTName,
                        onValueChange = { newTName = it },
                        label = { Text("Territory Sector Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("territory_name_input")
                    )

                    OutlinedTextField(
                        value = newTDesc,
                        onValueChange = { newTDesc = it },
                        label = { Text("Area / Target Product Focus") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("territory_desc_input")
                    )

                    Text("Marker Group Color", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableColors.forEach { hex ->
                            val colorValue = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(colorValue, CircleShape)
                                    .clickable { selectedColorHex = hex }
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreateTerritory = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onAddTerritory(newTName, newTDesc, selectedColorHex)
                                newTName = ""
                                newTDesc = ""
                                showCreateTerritory = false
                            },
                            modifier = Modifier.testTag("save_territory_button")
                        ) {
                            Text("Save Sector")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. OFFLINE SYNC LEDGER SCREEN
// ==========================================

@Composable
fun SyncLedgerScreen(
    leads: List<Lead>,
    syncLogs: List<SyncLog>,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onClearLogs: () -> Unit
) {
    val unsyncedLeads = leads.filter { !it.isSynced }
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Offline Sync Center",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sales Rabbit offline database tracking and backup",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large status card with Sync Now Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (unsyncedLeads.isEmpty()) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
                else 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (unsyncedLeads.isEmpty()) "Database Protected" else "Database Out Of Sync",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (unsyncedLeads.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (unsyncedLeads.isEmpty()) "All drop pins are uploaded." else "${unsyncedLeads.size} pins waiting to be synced.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                if (unsyncedLeads.isEmpty()) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) 
                                else 
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (unsyncedLeads.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning, // Changed CloudDone / CloudQueue to CheckCircle / Warning
                            contentDescription = null,
                            tint = if (unsyncedLeads.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    if (isSyncing) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Piping database payloads over active internet...", fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = onSyncClick,
                            modifier = Modifier.fillMaxWidth().testTag("trigger_sync_button")
                        ) {
                            Icon(Icons.Default.Refresh, null) // Changed CloudUpload to Refresh
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Handshake & Upload Sync (${unsyncedLeads.size} pending)")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sync Handshake History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (syncLogs.isNotEmpty()) {
                TextButton(onClick = onClearLogs) {
                    Text("Clear Ledger")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (syncLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No sync operations completed yet.", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("sync_logs_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(syncLogs) { log ->
                    val isSuccess = log.status == "SUCCESS"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning, // Changed Cancel to Warning
                                        contentDescription = null,
                                        tint = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSuccess) "Sync Cleared" else "Sync Interrupted",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                                Text(
                                    text = formatter.format(Date(log.timestamp)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = log.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isSuccess && log.recordsSynced > 0) {
                                Text(
                                    text = "Packets: ${log.recordsSynced} records pushed successfully.",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// LEAFLET MAP VIEW & HELPERS INTEGRATION
// ==========================================

fun leadsListToJson(leads: List<Lead>): String {
    val builder = StringBuilder()
    builder.append("[")
    leads.forEachIndexed { index, lead ->
        val escapedName = lead.name.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedAddress = lead.address.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedStatus = lead.status.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedNotes = lead.notes.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedPhone = lead.phone.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")

        builder.append("{")
        builder.append("\"id\":${lead.id},")
        builder.append("\"name\":\"$escapedName\",")
        builder.append("\"address\":\"$escapedAddress\",")
        builder.append("\"status\":\"$escapedStatus\",")
        builder.append("\"latitude\":${lead.latitude},")
        builder.append("\"longitude\":${lead.longitude},")
        builder.append("\"notes\":\"$escapedNotes\",")
        builder.append("\"phone\":\"$escapedPhone\",")
        builder.append("\"territoryId\":${lead.territoryId ?: "null"}")
        builder.append("}")
        if (index < leads.size - 1) {
            builder.append(",")
        }
    }
    builder.append("]")
    return builder.toString()
}

fun housePlotsToJson(plots: List<HousePlot>): String {
    val builder = StringBuilder()
    builder.append("[")
    plots.forEachIndexed { index, plot ->
        val escapedAddress = plot.address.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedOwnerName = plot.ownerName.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedPrimaryLanguage = plot.primaryLanguage.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedTenureStatus = plot.tenureStatus.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedIncomeBracket = plot.incomeBracket.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedGender = plot.gender.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedMaritalStatus = plot.maritalStatus.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedYearBuilt = plot.yearBuilt.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedEstimatedValue = plot.estimatedValue.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedSquareFeet = plot.squareFeet.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedCreditScore = plot.creditScore.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedPhone = plot.phone.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
        val escapedEmail = plot.email.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")

        builder.append("{")
        builder.append("\"address\":\"$escapedAddress\",")
        builder.append("\"latitude\":${plot.latitude},")
        builder.append("\"longitude\":${plot.longitude},")
        builder.append("\"ownerName\":\"$escapedOwnerName\",")
        builder.append("\"phone\":\"$escapedPhone\",")
        builder.append("\"email\":\"$escapedEmail\",")
        builder.append("\"primaryLanguage\":\"$escapedPrimaryLanguage\",")
        builder.append("\"tenureStatus\":\"$escapedTenureStatus\",")
        builder.append("\"incomeBracket\":\"$escapedIncomeBracket\",")
        builder.append("\"gender\":\"$escapedGender\",")
        builder.append("\"maritalStatus\":\"$escapedMaritalStatus\",")
        builder.append("\"yearBuilt\":\"$escapedYearBuilt\",")
        builder.append("\"estimatedValue\":\"$escapedEstimatedValue\",")
        builder.append("\"squareFeet\":\"$escapedSquareFeet\",")
        builder.append("\"occupants\":${plot.occupants},")
        builder.append("\"creditScore\":\"$escapedCreditScore\",")
        builder.append("\"initialRoofAge\":${plot.initialRoofAge}")
        builder.append("}")
        if (index < plots.size - 1) {
            builder.append(",")
        }
    }
    builder.append("]")
    return builder.toString()
}

fun getLeafletHtml(leadsJson: String, staticHousePlotsJson: String, centerLat: Double, centerLng: Double, isDarkTheme: Boolean): String {
    val safeLeadsJson = leadsJson.replace("'", "\\'")
    val safeHousePlotsJson = staticHousePlotsJson.replace("'", "\\'")
    val textColor = if (isDarkTheme) "#E0E0E0" else "#212121"
    val bgColor = if (isDarkTheme) "#1E1E1E" else "#FFFFFF"
    val inputColor = if (isDarkTheme) "#2C2C2C" else "#F5F5F5"
    val borderColor = if (isDarkTheme) "#444444" else "#CCCCCC"

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.css" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.5.3/dist/MarkerCluster.Default.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <script src="https://unpkg.com/leaflet.markercluster@1.5.3/dist/leaflet.markercluster.js"></script>
            <style>
                html, body, #map {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                    background: ${if (isDarkTheme) "#121212" else "#f0f0f0"};
                }
                .leaflet-bar {
                    border: none !important;
                    box-shadow: 0 2px 6px rgba(0,0,0,0.2) !important;
                }
                .leaflet-bar a {
                    background: $bgColor !important;
                    color: $textColor !important;
                    border-bottom: 1px solid $borderColor !important;
                }
                .leaflet-popup-content-wrapper {
                    background: ${if (isDarkTheme) "#191919" else "#FFFFFF"} !important;
                    color: $textColor !important;
                    border-radius: 12px;
                    font-family: sans-serif;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.3) !important;
                }
                .leaflet-popup-tip {
                    background: ${if (isDarkTheme) "#191919" else "#FFFFFF"} !important;
                }
                .leaflet-control-attribution {
                    display: none !important;
                }
                
                /* Offline Modal & UI elements styling */
                #offlineModal {
                    display: none;
                    position: fixed;
                    z-index: 99999;
                    left: 0;
                    top: 0;
                    width: 100%;
                    height: 100%;
                    background: rgba(0,0,0,0.6);
                    align-items: center;
                    justify-content: center;
                    animation: fadeIn 0.3s;
                }
                @keyframes fadeIn {
                    from { opacity: 0; }
                    to { opacity: 1; }
                }
                .modal-card {
                    background: $bgColor;
                    color: $textColor;
                    padding: 20px;
                    border-radius: 16px;
                    width: 85%;
                    max-width: 320px;
                    box-shadow: 0 10px 25px rgba(0,0,0,0.4);
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                }
                .modal-title {
                    margin-top: 0;
                    font-size: 16px;
                    font-weight: bold;
                    color: #FF9800;
                    border-bottom: 1px solid $borderColor;
                    padding-bottom: 8px;
                    display: flex;
                    align-items: center;
                    gap: 8px;
                }
                .modal-label {
                    font-size: 11px;
                    font-weight: bold;
                    display: block;
                    margin-bottom: 4px;
                    color: ${if (isDarkTheme) "#B0B0B0" else "#555555"};
                }
                .modal-input {
                    width: 100%;
                    box-sizing: border-box;
                    padding: 10px;
                    margin-bottom: 12px;
                    border: 1px solid $borderColor;
                    border-radius: 8px;
                    background: $inputColor;
                    color: $textColor;
                    font-size: 13px;
                }
                .modal-input:focus {
                    outline: none;
                    border-color: #FF9800;
                }
                .modal-buttons {
                    display: flex;
                    justify-content: space-between;
                    gap: 10px;
                    margin-top: 8px;
                }
                .btn-cancel {
                    flex: 1;
                    border: none;
                    background: #757575;
                    color: white;
                    padding: 10px;
                    border-radius: 8px;
                    font-weight: bold;
                    cursor: pointer;
                    font-size: 12px;
                }
                .btn-save {
                    flex: 1;
                    border: none;
                    background: #FF9800;
                    color: white;
                    padding: 10px;
                    border-radius: 8px;
                    font-weight: bold;
                    cursor: pointer;
                    font-size: 12px;
                }
                
                #offlineToast {
                    display: none;
                    position: fixed;
                    bottom: 30px;
                    left: 50%;
                    transform: translateX(-50%);
                    z-index: 100000;
                    background: #333333;
                    color: #ffffff;
                    padding: 12px 20px;
                    border-radius: 30px;
                    font-size: 12px;
                    font-family: sans-serif;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.4);
                    text-align: center;
                    min-width: 240px;
                    max-width: 80%;
                    line-height: 1.4;
                    animation: slideUp 0.3s;
                }
                @keyframes slideUp {
                    from { transform: translate(-50%, 50px); opacity: 0; }
                    to { transform: translate(-50%, 0); opacity: 1; }
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            
            <!-- Offline Modal Dialog Layer -->
            <div id="offlineModal">
                <div class="modal-card">
                    <h3 class="modal-title">
                        <span>⚠️ Offline Pin Drop (IndexedDB)</span>
                    </h3>
                    <p style="font-size:12px; margin-bottom:12px; line-height: 1.4;">
                        You are currently offline. This pin will be stored locally in <b>IndexedDB</b> and uploaded to the server once your connection is restored.
                    </p>
                    
                    <label class="modal-label">Owner/Homeowner Name</label>
                    <input type="text" id="offName" placeholder="e.g. Jean Dupont" class="modal-input" />
                    
                    <label class="modal-label">Contact Phone</label>
                    <input type="text" id="offPhone" placeholder="e.g. 555-0100" class="modal-input" />
                    
                    <label class="modal-label">Notes & Observations</label>
                    <textarea id="offNotes" placeholder="e.g. Solar consultation preferred, roof is south-facing." rows="3" class="modal-input" style="font-family:sans-serif; resize:none;"></textarea>
                    
                    <div class="modal-buttons">
                        <button onclick="closeOfflineModal()" class="btn-cancel">Cancel</button>
                        <button onclick="saveOfflineAndClose()" class="btn-save">Save Pin Offline</button>
                    </div>
                </div>
            </div>
            
            <!-- Global Toast notification banner -->
            <div id="offlineToast"></div>

            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    zoomAnimation: true,
                    fadeAnimation: true,
                    markerZoomAnimation: true
                });

                var osmTiles = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 20
                });

                var darkTiles = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                    maxZoom: 20
                });

                var satelliteTiles = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                    maxZoom: 20
                });

                var currentBase = ${if (isDarkTheme) "darkTiles" else "osmTiles"};
                currentBase.addTo(map);

                map.setView([$centerLat, $centerLng], 13);

                var gpsRing = L.circle([$centerLat, $centerLng], {
                    color: '#2196F3',
                    fillColor: '#2196F3',
                    fillOpacity: 0.15,
                    radius: 200,
                    weight: 1
                }).addTo(map);

                var gpsMarker = L.circleMarker([$centerLat, $centerLng], {
                    color: '#ffffff',
                    fillColor: '#1976D2',
                    fillOpacity: 0.95,
                    radius: 8,
                    weight: 3
                }).addTo(map);

                var markerGroup = L.markerClusterGroup({
                    spiderfyOnMaxZoom: true,
                    showCoverageOnHover: false,
                    zoomToBoundsOnClick: true,
                    maxClusterRadius: 50
                }).addTo(map);

                var offlineMarkerGroup = L.markerClusterGroup({
                    spiderfyOnMaxZoom: true,
                    showCoverageOnHover: false,
                    zoomToBoundsOnClick: true,
                    maxClusterRadius: 50
                }).addTo(map);

                // ==========================================
                // INDEXEDDB DATABASE INTEGRATION
                // ==========================================
                var db;
                var dbRequest = indexedDB.open("LeafletOfflineDB", 1);
                
                dbRequest.onupgradeneeded = function(e) {
                    var database = e.target.result;
                    if (!database.objectStoreNames.contains("offline_pins")) {
                        database.createObjectStore("offline_pins", { keyPath: "id", autoIncrement: true });
                    }
                };

                dbRequest.onsuccess = function(e) {
                    db = e.target.result;
                    console.log("IndexedDB database established.");
                    loadExistingOfflinePins();
                    
                    if (navigator.onLine) {
                        syncOfflinePins();
                    }
                };

                dbRequest.onerror = function(e) {
                    console.error("IndexedDB expansion failure: " + e);
                };

                function loadExistingOfflinePins() {
                    if (!db) return;
                    try {
                        var transaction = db.transaction(["offline_pins"], "readonly");
                        var store = transaction.objectStore("offline_pins");
                        var request = store.getAll();
                        request.onsuccess = function(e) {
                            var pins = e.target.result;
                            if (pins) {
                                pins.forEach(function(pin) {
                                    drawOfflineMarker(pin);
                                });
                            }
                        };
                    } catch (err) {
                        console.error("Error loading offline pins: " + err);
                    }
                }

                function drawOfflineMarker(pin) {
                    var marker = L.circleMarker([pin.latitude, pin.longitude], {
                        color: '#FF9800',
                        fillColor: '#FFC107',
                        fillOpacity: 0.90,
                        radius: 11,
                        weight: 2.5,
                        dashArray: '3, 4'
                    }).addTo(offlineMarkerGroup);

                    var popupHtml = "<div style='font-size:12px; line-height:1.4; padding:2px; min-width:140px;'>" +
                        "<div style='background:#FF9800; color:white; padding:2px 6px; border-radius:4px; font-weight:bold; margin-bottom:6px; text-align:center;'>OFFLINE CACHED</div>" +
                        "<b>" + (pin.name || 'Offline Owner') + "</b><br/>" +
                        (pin.phone ? "📞 " + pin.phone + "<br/>" : "") +
                        "Notes: " + (pin.notes || 'No description') + "<br/>" +
                        "<span style='font-size:10px; color:#FF9800; font-weight:bold;'>Syncing automatically when online</span>" +
                        "</div>";

                    marker.bindPopup(popupHtml);
                }

                function clearOfflineMarkers() {
                    offlineMarkerGroup.clearLayers();
                }

                function syncOfflinePins() {
                    if (!db) return;
                    try {
                        var transaction = db.transaction(["offline_pins"], "readonly");
                        var store = transaction.objectStore("offline_pins");
                        var request = store.getAll();
                        
                        request.onsuccess = function(e) {
                            var pins = e.target.result;
                            if (pins && pins.length > 0) {
                                console.log("Broadcasting " + pins.length + " IndexedDB pins back online.");
                                if (window.AndroidInterface && window.AndroidInterface.syncOfflineWebviewPins) {
                                    window.AndroidInterface.syncOfflineWebviewPins(JSON.stringify(pins));
                                    
                                    // Clear IndexedDB store
                                    var clearTx = db.transaction(["offline_pins"], "readwrite");
                                    var clearStore = clearTx.objectStore("offline_pins");
                                    clearStore.clear().onsuccess = function() {
                                        console.log("IndexedDB logs successfully flushed.");
                                        clearOfflineMarkers();
                                        showToastNotification("💡 Connection Restored! Successfully synchronized " + pins.length + " offline cached pins.");
                                    };
                                }
                            }
                        };
                    } catch (err) {
                        console.error("IndexedDB Sync engine error: " + err);
                    }
                }

                function showToastNotification(text) {
                    var t = document.getElementById("offlineToast");
                    t.innerText = text;
                    t.style.display = "block";
                    setTimeout(function() {
                        t.style.display = "none";
                    }, 4000);
                }

                // Global network state change listeners
                window.addEventListener('online', function() {
                    console.log("Network back online. Re-initiating synchronization pipeline.");
                    syncOfflinePins();
                });

                window.addEventListener('offline', function() {
                    console.log("Network service disconnected. Falling back onto local IndexedDB layer.");
                    showToastNotification("⚠️ Disconnected! App running in Offline fallback mode.");
                });

                // Offline form management
                var currentOffLat = 0.0;
                var currentOffLng = 0.0;

                function openOfflineModal(lat, lng) {
                    currentOffLat = lat;
                    currentOffLng = lng;
                    document.getElementById("offName").value = "";
                    document.getElementById("offPhone").value = "";
                    document.getElementById("offNotes").value = "";
                    document.getElementById("offlineModal").style.display = "flex";
                }

                function closeOfflineModal() {
                    document.getElementById("offlineModal").style.display = "none";
                }

                function saveOfflineAndClose() {
                    var name = document.getElementById("offName").value.trim();
                    var phone = document.getElementById("offPhone").value.trim();
                    var notes = document.getElementById("offNotes").value.trim();
                    
                    if (!db) {
                        alert("Database offline layer initializing. Please retry in a split second.");
                        return;
                    }

                    var transaction = db.transaction(["offline_pins"], "readwrite");
                    var store = transaction.objectStore("offline_pins");
                    var pinRecord = {
                        latitude: currentOffLat,
                        longitude: currentOffLng,
                        name: name || "Offline Owner",
                        phone: phone || "",
                        notes: notes || "Dropped pin while offline",
                        status: "NOT_HOME",
                        timestamp: Date.now()
                    };

                    var request = store.add(pinRecord);
                    request.onsuccess = function() {
                        console.log("Pin stored in Webview IndexedDB.");
                        drawOfflineMarker(pinRecord);
                        closeOfflineModal();
                        showToastNotification("📌 Pin dropped and queued in IndexedDB locally!");
                    };
                    request.onerror = function() {
                        alert("Failed to drop pin in local storage.");
                    };
                }

                // ==========================================
                // PIN RENDERING & STANDARD INTERACTION
                // ==========================================
                function updateMarkers(leadsJsonString) {
                    markerGroup.clearLayers();
                    try {
                        var leads = JSON.parse(leadsJsonString);
                        leads.forEach(function(lead) {
                            var pinColor = "#757575";
                            if (lead.status === "WARM_LEAD") pinColor = "#1565C0";
                            else if (lead.status === "GO_BACK") pinColor = "#F57C00";
                            else if (lead.status === "CUSTOMER") pinColor = "#2E7D32";
                            else if (lead.status === "REFUSED") pinColor = "#C62828";
                            else if (lead.status === "NOT_HOME") pinColor = "#757575";

                            var marker = L.circleMarker([lead.latitude, lead.longitude], {
                                color: '#ffffff',
                                fillColor: pinColor,
                                fillOpacity: 0.95,
                                radius: 10,
                                weight: 2
                            }).addTo(markerGroup);

                            var popupHtml = "<div style='font-size:12px; line-height:1.4; padding:2px; min-width:140px;'>" +
                                "<b>" + (lead.name || 'Owner Unidentified') + "</b><br/>" +
                                (lead.phone ? "📞 " + lead.phone + "<br/>" : "") +
                                (lead.address ? "📍 " + lead.address + "<br/>" : "") +
                                "Status: <span style='font-weight:bold; color: " + pinColor + "'>" + lead.status + "</span><br/>" +
                                "Notes: " + (lead.notes || 'No notes') + "<br/>" +
                                "<button onclick='AndroidInterface.openLeadDetails(" + lead.id + ")' style='margin-top:8px; width:100%; border:none; background:#1976D2; color:white; padding:5px 8px; border-radius:4px; font-weight:bold; cursor:pointer;'>View Details & AI Pitch</button>" +
                                "</div>";

                            marker.bindPopup(popupHtml);
                        });
                    } catch(err) {
                        console.error("JSON parse err: " + err);
                    }
                }

                var houseGroup = L.layerGroup().addTo(map);

                function updateHouses(housesJsonString) {
                    houseGroup.clearLayers();
                    try {
                        var houses = JSON.parse(housesJsonString);
                        houses.forEach(function(house) {
                            var marker = L.circleMarker([house.latitude, house.longitude], {
                                color: '#E65100',
                                fillColor: '#FFD54F',
                                fillOpacity: 0.85,
                                radius: 10,
                                weight: 2,
                                dashArray: '2, 3'
                            }).addTo(houseGroup);

                            var popupHtml = "<div style='font-size:12px; line-height:1.4; padding:2px; min-width:150px;'>" +
                                "<div style='background:#E65100; color:white; padding:3px 6px; border-radius:4px; font-weight:bold; margin-bottom:6px; text-align:center;'>🏠 FLORIDA HOUSE</div>" +
                                "<b>Owner:</b> " + house.ownerName + "<br/>" +
                                "<b>Address:</b> " + house.address + "<br/>" +
                                "<b>Built:</b> " + house.yearBuilt + "<br/>" +
                                "<button onclick='AndroidInterface.onHomeClick(\"" + house.address.replace(/'/g, "\\'") + "\")' style='margin-top:8px; width:100%; border:none; background:#E65100; color:white; padding:5px 8px; border-radius:4px; font-weight:bold; cursor:pointer;'>Select & Drop Pin</button>" +
                                "</div>";

                            marker.bindPopup(popupHtml);

                            marker.on('click', function() {
                                AndroidInterface.onHomeClick(house.address);
                            });
                        });
                    } catch(err) {
                        console.error("Houses parse err: " + err);
                    }
                }

                // Initial load
                updateMarkers('$safeLeadsJson');
                updateHouses('$safeHousePlotsJson');

                function setMapStyle(style) {
                    map.removeLayer(osmTiles);
                    map.removeLayer(darkTiles);
                    map.removeLayer(satelliteTiles);

                    if (style === 'satellite') {
                        satelliteTiles.addTo(map);
                    } else if (style === 'street') {
                        osmTiles.addTo(map);
                    } else if (style === 'dark') {
                        darkTiles.addTo(map);
                    }
                }

                function centerOnLocation(lat, lng) {
                    map.setView([lat, lng], 14);
                    gpsMarker.setLatLng([lat, lng]);
                    gpsRing.setLatLng([lat, lng]);
                }

                function handleInteraction(lat, lng) {
                    if (navigator.onLine) {
                        AndroidInterface.onMapLongPress(lat, lng);
                    } else {
                        openOfflineModal(lat, lng);
                    }
                }

                map.on('dblclick', function(e) {
                    handleInteraction(e.latlng.lat, e.latlng.lng);
                });

                map.on('contextmenu', function(e) {
                    handleInteraction(e.latlng.lat, e.latlng.lng);
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
fun LeafletMapView(
    leads: List<Lead>,
    staticHousePlots: List<HousePlot>,
    centerLat: Double,
    centerLng: Double,
    isDarkTheme: Boolean,
    mapStyle: String,
    onMapLongClick: (Double, Double) -> Unit,
    onLeadSelect: (Long) -> Unit,
    onHomeSelect: (String) -> Unit,
    onAddOfflineLeads: (List<Lead>) -> Unit,
    modifier: Modifier = Modifier
) {
    val leadsJson = remember(leads) { leadsListToJson(leads) }
    val staticHousePlotsJson = remember(staticHousePlots) { housePlotsToJson(staticHousePlots) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(leadsJson) {
        webViewRef?.evaluateJavascript("updateMarkers('$leadsJson')", null)
    }

    LaunchedEffect(staticHousePlotsJson) {
        webViewRef?.evaluateJavascript("updateHouses('$staticHousePlotsJson')", null)
    }

    LaunchedEffect(mapStyle) {
        webViewRef?.evaluateJavascript("setMapStyle('$mapStyle')", null)
    }

    LaunchedEffect(centerLat, centerLng) {
        webViewRef?.evaluateJavascript("centerOnLocation($centerLat, $centerLng)", null)
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        evaluateJavascript("updateMarkers('$leadsJson')", null)
                        evaluateJavascript("updateHouses('$staticHousePlotsJson')", null)
                        evaluateJavascript("setMapStyle('$mapStyle')", null)
                    }
                }
                addJavascriptInterface(object : Any() {
                    @android.webkit.JavascriptInterface
                    fun onMapDoubleTap(lat: Double, lng: Double) {
                        onMapLongClick(lat, lng)
                    }

                    @android.webkit.JavascriptInterface
                    fun onMapLongPress(lat: Double, lng: Double) {
                        onMapLongClick(lat, lng)
                    }

                    @android.webkit.JavascriptInterface
                    fun openLeadDetails(leadId: Long) {
                        onLeadSelect(leadId)
                    }

                    @android.webkit.JavascriptInterface
                    fun onHomeClick(address: String) {
                        onHomeSelect(address)
                    }

                    @android.webkit.JavascriptInterface
                    fun syncOfflineWebviewPins(pinsJson: String) {
                        try {
                            val jsonArray = org.json.JSONArray(pinsJson)
                            val list = mutableListOf<Lead>()
                            for (i in 0 until jsonArray.length()) {
                                  val obj = jsonArray.getJSONObject(i)
                                  list.add(
                                      Lead(
                                          name = obj.optString("name", "Offline Owner"),
                                          address = obj.optString("address", "Offline Location"),
                                          status = obj.optString("status", "NOT_HOME"),
                                          latitude = obj.optDouble("latitude", 0.0),
                                          longitude = obj.optDouble("longitude", 0.0),
                                          notes = obj.optString("notes", ""),
                                          phone = obj.optString("phone", "")
                                      )
                                  )
                            }
                            onAddOfflineLeads(list)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }, "AndroidInterface")

                val htmlContent = getLeafletHtml(
                    leadsJson = leadsJson,
                    staticHousePlotsJson = staticHousePlotsJson,
                    centerLat = centerLat,
                    centerLng = centerLng,
                    isDarkTheme = isDarkTheme
                )
                loadDataWithBaseURL("https://leaflet-map", htmlContent, "text/html", "UTF-8", null)
                webViewRef = this
            }
        },
        update = { webView ->
            // Update
        },
        modifier = modifier
    )
}

@Composable
fun UserMenuScreen(
    allLeads: List<Lead>,
    onImportLeads: (List<Lead>) -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("demo@example.com") }
    var name by remember { mutableStateOf("John Doe") }
    var companyName by remember { mutableStateOf("Acme Roofing") }

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = outputStream.bufferedWriter()
                writer.write("Name,Address,Status,Latitude,Longitude,Phone,Notes\n")
                allLeads.forEach { lead ->
                    val safeName = lead.name.replace(",", " ")
                    val safeAddress = lead.address.replace(",", " ")
                    val safeStatus = lead.status.replace(",", " ")
                    val safeNotes = lead.notes.replace("\n", " ").replace(",", " ")
                    val safePhone = lead.phone.replace(",", " ")
                    writer.write("${safeName},${safeAddress},${safeStatus},${lead.latitude},${lead.longitude},${safePhone},${safeNotes}\n")
                }
                writer.flush()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = inputStream.bufferedReader()
                // skip header
                reader.readLine()
                val imported = mutableListOf<Lead>()
                reader.forEachLine { line ->
                    val parts = line.split(",")
                    if(parts.size >= 7) {
                        imported.add(
                            Lead(
                                name = parts[0],
                                address = parts[1],
                                status = parts[2],
                                latitude = parts[3].toDoubleOrNull() ?: 0.0,
                                longitude = parts[4].toDoubleOrNull() ?: 0.0,
                                phone = parts[5],
                                notes = parts[6]
                            )
                        )
                    }
                }
                onImportLeads(imported)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "User Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        OutlinedTextField(
            value = companyName,
            onValueChange = { companyName = it },
            label = { Text("Company Name") },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "Data Management",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Button(
            onClick = {
                exportLauncher.launch("leads_export.csv")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Share, contentDescription = "Export")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Leads to CSV")
        }

        OutlinedButton(
            onClick = {
                importLauncher.launch(arrayOf("text/csv", "*/*"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Import")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Leads from CSV")
        }
    }
}
