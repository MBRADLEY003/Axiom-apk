package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.configureAppCheck
import com.example.data.AxiomRepository
import com.example.data.OttiliaAiRepository
import com.example.domain.rbac.AxiomPermission
import com.example.domain.rbac.RbacPermissions
import com.example.domain.rbac.SecurityHashUtil
import com.example.ui.components.AxiomBrandLogo
import com.example.ui.components.AxiomPillBadge
import com.example.ui.screens.AnalyticsDataScienceScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DriverDetailScreen
import com.example.ui.screens.DriversScreen
import com.example.ui.screens.FinancialIntelligenceScreen
import com.example.ui.screens.FleetScreen
import com.example.ui.screens.FuelIntelligenceScreen
import com.example.ui.screens.GpsRoutesScreen
import com.example.ui.screens.LogisticsScreen
import com.example.ui.screens.MaintenanceScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OttiliaAiScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.RiskIntelligenceScreen
import com.example.ui.screens.RiskInvestigationScreen
import com.example.ui.screens.SettingsAndAdminScreen
import com.example.ui.screens.VehicleDetailScreen
import com.example.ui.screens.signOutAxiom
import com.example.ui.theme.AxiomCharcoal
import com.example.ui.theme.AxiomCrimson
import com.example.ui.theme.AxiomGold
import com.example.ui.theme.AxiomGoldSubtleContainer
import com.example.ui.theme.AxiomHairline
import com.example.ui.theme.AxiomIvory
import com.example.ui.theme.AxiomObsidian
import com.example.ui.theme.AxiomSilver
import com.example.ui.theme.AxiomSlate
import com.example.ui.theme.AxiomTheme
import com.example.ui.viewmodel.AxiomViewModel
import com.example.ui.viewmodel.AxiomWorkspaceUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

object AxiomRoutes {
    const val OVERVIEW = "overview"
    const val FLEET = "fleet"
    const val FUEL = "fuel"
    const val GPS = "gps"
    const val DRIVERS = "drivers"
    const val MAINTENANCE = "maintenance"
    const val LOGISTICS = "logistics"
    const val FINANCE = "finance"
    const val RISK = "risk"
    const val ANALYTICS = "analytics"
    const val REPORTS = "reports"
    const val OTTILIA = "ottilia"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"

    const val VEHICLE_DETAIL = "vehicle/{vehicleId}"
    const val DRIVER_DETAIL = "driver/{driverId}"
    const val RISK_DETAIL = "risk/{alertId}"

    fun vehicleDetail(id: String) = "vehicle/$id"
    fun driverDetail(id: String) = "driver/$id"
    fun riskDetail(id: String) = "risk/$id"
}

private data class AxiomNavModule(
    val route: String,
    val title: String,
    val section: String,
    val icon: ImageVector,
)

private val axiomModules = listOf(
    AxiomNavModule(AxiomRoutes.OVERVIEW, "Overview", "COMMAND", Icons.Outlined.Dashboard),
    AxiomNavModule(AxiomRoutes.OTTILIA, "Ottilia AI", "COMMAND", Icons.Filled.AutoAwesome),
    AxiomNavModule(AxiomRoutes.FLEET, "Fleet Assets", "OPERATIONS", Icons.Outlined.DirectionsCar),
    AxiomNavModule(AxiomRoutes.FUEL, "Fuel Intelligence", "OPERATIONS", Icons.Outlined.LocalGasStation),
    AxiomNavModule(AxiomRoutes.GPS, "GPS & Routes", "OPERATIONS", Icons.Outlined.Map),
    AxiomNavModule(AxiomRoutes.DRIVERS, "Drivers", "OPERATIONS", Icons.Outlined.Person),
    AxiomNavModule(AxiomRoutes.MAINTENANCE, "Maintenance", "OPERATIONS", Icons.Outlined.Build),
    AxiomNavModule(AxiomRoutes.LOGISTICS, "Logistics", "OPERATIONS", Icons.Outlined.LocalShipping),
    AxiomNavModule(AxiomRoutes.FINANCE, "Financial Intel", "INTELLIGENCE", Icons.Outlined.AccountBalanceWallet),
    AxiomNavModule(AxiomRoutes.RISK, "Risk Intel", "INTELLIGENCE", Icons.Outlined.Shield),
    AxiomNavModule(AxiomRoutes.ANALYTICS, "Analytics & OLS", "INTELLIGENCE", Icons.Outlined.Analytics),
    AxiomNavModule(AxiomRoutes.REPORTS, "Reports", "GOVERNANCE", Icons.AutoMirrored.Outlined.Assignment),
    AxiomNavModule(AxiomRoutes.NOTIFICATIONS, "Notifications", "GOVERNANCE", Icons.Outlined.Notifications),
    AxiomNavModule(AxiomRoutes.SETTINGS, "Settings & Admin", "GOVERNANCE", Icons.Outlined.Settings),
)

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must configure App Check immediately after super.onCreate and before UI/AI initialization
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            AxiomTheme {
                val authFlow = remember { Firebase.auth.authStateFlow() }
                val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

                if (currentUser == null) {
                    AuthScreen(onAuthSuccess = { /* authStateFlow reacts automatically */ })
                } else {
                    val user = currentUser!!
                    key(user.uid) {
                        AxiomAuthenticatedWorkspace(user = user)
                    }
                }
            }
        }
    }
}

@Composable
private fun AxiomAuthenticatedWorkspace(user: FirebaseUser) {
    val viewModel: AxiomViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                AxiomViewModel(
                    repository = AxiomRepository(app),
                    ottiliaRepo = OttiliaAiRepository(),
                    currentUserId = user.uid,
                    userEmail = user.email.orEmpty(),
                    userDisplayName = user.displayName.orEmpty(),
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val ottiliaMessages by viewModel.ottiliaMessages.collectAsStateWithLifecycle()
    val ottiliaStreaming by viewModel.ottiliaStreaming.collectAsStateWithLifecycle()
    val ottiliaBusy by viewModel.ottiliaBusy.collectAsStateWithLifecycle()
    val ottiliaError by viewModel.ottiliaError.collectAsStateWithLifecycle()
    val briefingState by viewModel.briefingState.collectAsStateWithLifecycle()

    var pinUnlocked by rememberSaveable(user.uid) { mutableStateOf(false) }

    when (val state = uiState) {
        is AxiomWorkspaceUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AxiomObsidian),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AxiomBrandLogo(sizeDp = 52, showWordmark = true, subtitle = "Initializing Command Centre")
                    Spacer(Modifier.height(18.dp))
                    CircularProgressIndicator(color = AxiomGold)
                }
            }
        }

        is AxiomWorkspaceUiState.Error -> {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val credentialManager = remember(context) { CredentialManager.create(context) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AxiomObsidian)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AxiomCharcoal),
                    border = BorderStroke(1.dp, AxiomCrimson.copy(alpha = 0.5f)),
                    modifier = Modifier.widthIn(max = 460.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Workspace Sync Error", style = MaterialTheme.typography.titleLarge, color = AxiomCrimson)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = AxiomIvory)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { signOutAxiom(credentialManager, scope) },
                            border = BorderStroke(1.dp, AxiomGold),
                        ) {
                            Text("Sign Out & Re-Authenticate", color = AxiomGold)
                        }
                    }
                }
            }
        }

        is AxiomWorkspaceUiState.Ready -> {
            if (!pinUnlocked && !state.profile.pinHash.isNullOrBlank()) {
                AxiomPinLockGate(
                    userId = user.uid,
                    email = state.profile.email,
                    expectedHash = state.profile.pinHash,
                    onUnlocked = { pinUnlocked = true },
                )
            } else {
                AxiomShell(
                    state = state,
                    viewModel = viewModel,
                    snackbarMessage = snackbarMessage,
                    ottiliaMessages = ottiliaMessages,
                    ottiliaStreaming = ottiliaStreaming,
                    ottiliaBusy = ottiliaBusy,
                    ottiliaError = ottiliaError,
                    briefingState = briefingState,
                    onLockWorkspace = {
                        if (!state.profile.pinHash.isNullOrBlank()) {
                            pinUnlocked = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AxiomPinLockGate(
    userId: String,
    email: String,
    expectedHash: String,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AxiomObsidian)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AxiomCharcoal),
            border = BorderStroke(1.dp, AxiomHairline),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AxiomBrandLogo(sizeDp = 44, showWordmark = true, subtitle = "Session PIN Required")
                Spacer(Modifier.height(16.dp))
                Text("Operator: $email", style = MaterialTheme.typography.bodySmall, color = AxiomSilver)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it; error = null },
                    label = { Text("Enter Security PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_unlock_input"),
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, style = MaterialTheme.typography.bodySmall, color = AxiomCrimson)
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (SecurityHashUtil.verifyPin(pin, userId, expectedHash)) {
                            onUnlocked()
                        } else {
                            error = "Incorrect security PIN."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AxiomGold, contentColor = AxiomObsidian),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pin_unlock_button"),
                ) {
                    Text("Unlock AXIOM Workspace")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { signOutAxiom(credentialManager, scope) },
                    border = BorderStroke(1.dp, AxiomHairline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sign Out Instead", color = AxiomSilver)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AxiomShell(
    state: AxiomWorkspaceUiState.Ready,
    viewModel: AxiomViewModel,
    snackbarMessage: String?,
    ottiliaMessages: List<com.example.ui.viewmodel.OttiliaChatMessage>,
    ottiliaStreaming: String,
    ottiliaBusy: Boolean,
    ottiliaError: String?,
    briefingState: com.example.ui.viewmodel.BriefingUiState,
    onLockWorkspace: () -> Unit,
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        if (!snackbarMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(snackbarMessage)
            viewModel.clearSnackbar()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AxiomRoutes.OVERVIEW
    val unreadNotifications = state.notifications.count { !it.readStatus }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val currentTitle = remember(currentRoute) {
        when {
            currentRoute.startsWith("vehicle/") -> "Vehicle Telemetry Profile"
            currentRoute.startsWith("driver/") -> "Driver Operational Profile"
            currentRoute.startsWith("risk/") -> "Risk Investigation"
            else -> axiomModules.firstOrNull { it.route == currentRoute }?.title ?: "AXIOM"
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        if (isWideScreen) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        drawerContainerColor = AxiomCharcoal,
                        drawerContentColor = AxiomIvory,
                        modifier = Modifier.width(270.dp),
                    ) {
                        AxiomDrawerContent(
                            state = state,
                            currentRoute = currentRoute,
                            unreadNotifications = unreadNotifications,
                            onSelectRoute = { route -> navigateTopLevel(route) },
                        )
                    }
                }
            ) {
                AxiomScaffoldHost(
                    title = currentTitle,
                    state = state,
                    unreadNotifications = unreadNotifications,
                    showDrawerToggle = false,
                    showBottomBar = false,
                    currentRoute = currentRoute,
                    snackbarHostState = snackbarHostState,
                    onOpenDrawer = {},
                    onNavigateRoute = { navigateTopLevel(it) },
                    onLockWorkspace = onLockWorkspace,
                    navController = navController,
                    viewModel = viewModel,
                    ottiliaMessages = ottiliaMessages,
                    ottiliaStreaming = ottiliaStreaming,
                    ottiliaBusy = ottiliaBusy,
                    ottiliaError = ottiliaError,
                    briefingState = briefingState,
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = AxiomCharcoal,
                        drawerContentColor = AxiomIvory,
                        modifier = Modifier.width(285.dp),
                    ) {
                        AxiomDrawerContent(
                            state = state,
                            currentRoute = currentRoute,
                            unreadNotifications = unreadNotifications,
                            onSelectRoute = { route ->
                                scope.launch { drawerState.close() }
                                navigateTopLevel(route)
                            },
                        )
                    }
                }
            ) {
                AxiomScaffoldHost(
                    title = currentTitle,
                    state = state,
                    unreadNotifications = unreadNotifications,
                    showDrawerToggle = true,
                    showBottomBar = true,
                    currentRoute = currentRoute,
                    snackbarHostState = snackbarHostState,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateRoute = { navigateTopLevel(it) },
                    onLockWorkspace = onLockWorkspace,
                    navController = navController,
                    viewModel = viewModel,
                    ottiliaMessages = ottiliaMessages,
                    ottiliaStreaming = ottiliaStreaming,
                    ottiliaBusy = ottiliaBusy,
                    ottiliaError = ottiliaError,
                    briefingState = briefingState,
                )
            }
        }
    }
}

@Composable
private fun AxiomDrawerContent(
    state: AxiomWorkspaceUiState.Ready,
    currentRoute: String,
    unreadNotifications: Int,
    onSelectRoute: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            AxiomBrandLogo(
                sizeDp = 38,
                showWordmark = true,
                subtitle = state.activeCompany.name,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AxiomPillBadge(
                text = state.profile.roleEnum().displayName.uppercase(),
                fgColor = AxiomGold,
                bgColor = AxiomGoldSubtleContainer,
            )
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = AxiomHairline)
        Spacer(Modifier.height(8.dp))

        val sections = axiomModules.groupBy { it.section }
        sections.forEach { (section, items) ->
            Text(
                text = section,
                style = MaterialTheme.typography.labelSmall,
                color = AxiomSilver,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavigationDrawerItem(
                    selected = selected,
                    onClick = { onSelectRoute(item.route) },
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    badge = if (item.route == AxiomRoutes.NOTIFICATIONS && unreadNotifications > 0) {
                        {
                            Badge(containerColor = AxiomCrimson, contentColor = AxiomIvory) {
                                Text("$unreadNotifications")
                            }
                        }
                    } else null,
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = AxiomGoldSubtleContainer,
                        selectedIconColor = AxiomGold,
                        selectedTextColor = AxiomGold,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = AxiomSilver,
                        unselectedTextColor = AxiomIvory,
                    ),
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .testTag("drawer_nav_${item.route}"),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AxiomScaffoldHost(
    title: String,
    state: AxiomWorkspaceUiState.Ready,
    unreadNotifications: Int,
    showDrawerToggle: Boolean,
    showBottomBar: Boolean,
    currentRoute: String,
    snackbarHostState: SnackbarHostState,
    onOpenDrawer: () -> Unit,
    onNavigateRoute: (String) -> Unit,
    onLockWorkspace: () -> Unit,
    navController: NavHostController,
    viewModel: AxiomViewModel,
    ottiliaMessages: List<com.example.ui.viewmodel.OttiliaChatMessage>,
    ottiliaStreaming: String,
    ottiliaBusy: Boolean,
    ottiliaError: String?,
    briefingState: com.example.ui.viewmodel.BriefingUiState,
) {
    Scaffold(
        containerColor = AxiomObsidian,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AxiomCharcoal,
                    titleContentColor = AxiomIvory,
                ),
                navigationIcon = {
                    if (showDrawerToggle) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("open_nav_drawer_btn"),
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open Navigation Menu", tint = AxiomGold)
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = AxiomIvory,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${state.activeCompany.name} • ${state.profile.roleEnum().displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AxiomGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    if (!state.profile.pinHash.isNullOrBlank()) {
                        IconButton(
                            onClick = onLockWorkspace,
                            modifier = Modifier.testTag("lock_workspace_btn"),
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = "Lock Session", tint = AxiomSilver)
                        }
                    }
                    IconButton(
                        onClick = { onNavigateRoute(AxiomRoutes.NOTIFICATIONS) },
                        modifier = Modifier.testTag("top_notifications_btn"),
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge(containerColor = AxiomCrimson, contentColor = AxiomIvory) {
                                        Text("$unreadNotifications")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = AxiomGold,
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = AxiomCharcoal,
                    contentColor = AxiomIvory,
                ) {
                    val primaryTabs = listOf(
                        Triple(AxiomRoutes.OVERVIEW, "Overview", Icons.Filled.Dashboard),
                        Triple(AxiomRoutes.FLEET, "Fleet", Icons.Filled.DirectionsCar),
                        Triple(AxiomRoutes.FUEL, "Fuel", Icons.Filled.LocalGasStation),
                        Triple(AxiomRoutes.OTTILIA, "Ottilia AI", Icons.Filled.AutoAwesome),
                    )
                    primaryTabs.forEach { (route, label, icon) ->
                        val selected = currentRoute == route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onNavigateRoute(route) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AxiomObsidian,
                                selectedTextColor = AxiomGold,
                                indicatorColor = AxiomGold,
                                unselectedIconColor = AxiomSilver,
                                unselectedTextColor = AxiomSilver,
                            ),
                            modifier = Modifier.testTag("bottom_nav_$route"),
                        )
                    }
                    NavigationBarItem(
                        selected = currentRoute !in primaryTabs.map { it.first },
                        onClick = onOpenDrawer,
                        icon = { Icon(Icons.Filled.Menu, contentDescription = "All Modules") },
                        label = { Text("Modules", maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AxiomObsidian,
                            selectedTextColor = AxiomGold,
                            indicatorColor = AxiomGold,
                            unselectedIconColor = AxiomSilver,
                            unselectedTextColor = AxiomSilver,
                        ),
                        modifier = Modifier.testTag("bottom_nav_modules"),
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            NavHost(
                navController = navController,
                startDestination = AxiomRoutes.OVERVIEW,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(AxiomRoutes.OVERVIEW) {
                    OverviewScreen(
                        state = state,
                        onSetDateFilter = viewModel::setDateFilter,
                        onSetVehicleFilter = viewModel::setVehicleFilter,
                        onLoadDemoData = viewModel::loadSampleDemoFleet,
                        onNavigateToOttilia = { onNavigateRoute(AxiomRoutes.OTTILIA) },
                        onNavigateToReports = { onNavigateRoute(AxiomRoutes.REPORTS) },
                        onNavigateToRisk = { onNavigateRoute(AxiomRoutes.RISK) },
                    )
                }

                composable(AxiomRoutes.FLEET) {
                    FleetScreen(
                        state = state,
                        onAddVehicle = viewModel::addVehicle,
                        onUpdateVehicle = viewModel::updateVehicle,
                        onDeleteVehicle = viewModel::deleteVehicle,
                        onInspectVehicle = { id ->
                            navController.navigate(AxiomRoutes.vehicleDetail(id))
                        },
                    )
                }

                composable(
                    route = AxiomRoutes.VEHICLE_DETAIL,
                    arguments = listOf(navArgument("vehicleId") { type = NavType.StringType }),
                ) { entry ->
                    val vid = entry.arguments?.getString("vehicleId").orEmpty()
                    val vehicle = state.vehicles.firstOrNull { it.id == vid }
                    VehicleDetailScreen(
                        vehicle = vehicle,
                        state = state,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(AxiomRoutes.FUEL) {
                    FuelIntelligenceScreen(
                        state = state,
                        onLogFuel = viewModel::logFuelTransaction,
                    )
                }

                composable(AxiomRoutes.GPS) {
                    GpsRoutesScreen(
                        state = state,
                        onToggleSimMode = viewModel::toggleSimulatedMode,
                        onAddTrip = viewModel::addTrip,
                    )
                }

                composable(AxiomRoutes.DRIVERS) {
                    DriversScreen(
                        state = state,
                        onAddDriver = viewModel::addDriver,
                        onInspectDriver = { id ->
                            navController.navigate(AxiomRoutes.driverDetail(id))
                        },
                    )
                }

                composable(
                    route = AxiomRoutes.DRIVER_DETAIL,
                    arguments = listOf(navArgument("driverId") { type = NavType.StringType }),
                ) { entry ->
                    val did = entry.arguments?.getString("driverId").orEmpty()
                    val driver = state.drivers.firstOrNull { it.id == did }
                    DriverDetailScreen(
                        driver = driver,
                        state = state,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(AxiomRoutes.MAINTENANCE) {
                    MaintenanceScreen(
                        state = state,
                        onAddMaintenance = viewModel::addMaintenance,
                        onUpdateStatus = viewModel::updateMaintenanceStatus,
                    )
                }

                composable(AxiomRoutes.LOGISTICS) {
                    LogisticsScreen(
                        state = state,
                        onAddShipment = viewModel::addShipment,
                        onUpdateStatus = viewModel::updateShipmentStatus,
                    )
                }

                composable(AxiomRoutes.FINANCE) {
                    FinancialIntelligenceScreen(
                        state = state,
                        onAddExpense = viewModel::addExpense,
                    )
                }

                composable(AxiomRoutes.RISK) {
                    RiskIntelligenceScreen(
                        state = state,
                        onInspectRisk = { id ->
                            navController.navigate(AxiomRoutes.riskDetail(id))
                        },
                    )
                }

                composable(
                    route = AxiomRoutes.RISK_DETAIL,
                    arguments = listOf(navArgument("alertId") { type = NavType.StringType }),
                ) { entry ->
                    val aid = entry.arguments?.getString("alertId").orEmpty()
                    val alert = state.riskAlerts.firstOrNull { it.id == aid }
                    val canResolve = RbacPermissions.hasPermission(
                        state.profile.roleEnum(),
                        AxiomPermission.RESOLVE_RISK_ALERTS
                    )
                    RiskInvestigationScreen(
                        alert = alert,
                        onBack = { navController.popBackStack() },
                        onResolve = viewModel::resolveRiskAlert,
                        canResolve = canResolve,
                    )
                }

                composable(AxiomRoutes.ANALYTICS) {
                    AnalyticsDataScienceScreen(
                        state = state,
                        onAskOttilia = { onNavigateRoute(AxiomRoutes.OTTILIA) },
                    )
                }

                composable(AxiomRoutes.REPORTS) {
                    ReportsScreen(
                        state = state,
                        onExportFeedback = viewModel::showMessage,
                    )
                }

                composable(AxiomRoutes.OTTILIA) {
                    OttiliaAiScreen(
                        messages = ottiliaMessages,
                        streamingText = ottiliaStreaming,
                        isBusy = ottiliaBusy,
                        errorText = ottiliaError,
                        briefingState = briefingState,
                        onSendQuestion = viewModel::askOttilia,
                        onGenerateBriefing = viewModel::generateExecutiveBriefing,
                    )
                }

                composable(AxiomRoutes.NOTIFICATIONS) {
                    NotificationsScreen(
                        state = state,
                        onMarkRead = viewModel::markNotificationRead,
                        onUpdateChannels = viewModel::updateAlertChannels,
                    )
                }

                composable(AxiomRoutes.SETTINGS) {
                    SettingsAndAdminScreen(
                        state = state,
                        onSwitchCompany = viewModel::switchCompany,
                        onCreateCompany = viewModel::createCompany,
                        onSwitchRole = viewModel::switchActiveRole,
                        onAddTeamMember = viewModel::addTeamMember,
                        onSetPin = viewModel::setAppSecurityPin,
                        onLoadDemo = viewModel::loadSampleDemoFleet,
                        onCommitImport = viewModel::commitParsedImport,
                        onFeedback = viewModel::showMessage,
                    )
                }
            }
        }
    }
}
