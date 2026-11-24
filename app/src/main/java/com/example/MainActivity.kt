package com.example

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.RelayConnectionState
import com.example.data.remote.CloudAdminRepository
import com.example.notifications.SubscriptionNotificationManager
import com.example.ui.MainViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutGoogle
import com.example.ui.components.ActiveBridgeCallOverlay
import com.example.ui.components.HistoryBottomSheet
import com.example.ui.components.IphoneGuideSheet
import com.example.ui.components.PasswordRecoveryDialog
import com.example.ui.components.PaywallBottomSheet
import com.example.ui.components.PermissionsManagerSheet
import com.example.ui.screens.AdminStudioScreen
import com.example.ui.screens.BridgeHubScreen
import com.example.ui.screens.LegalDocumentType
import com.example.ui.screens.LegalPagesScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.MoreAndAccountScreen
import com.example.ui.screens.PhoneScreen
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ESkyTellTheme
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MintJade
import com.example.ui.theme.RoseError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

const val ROUTE_PHONE = "phone"
const val ROUTE_MESSAGES = "messages"
const val ROUTE_BRIDGE = "bridge"
const val ROUTE_MORE = "more"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private fun FirebaseAuth.authStateFlow() = callbackFlow<FirebaseUser?> {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {

    private val pendingSharedText = MutableStateFlow<String?>(null)
    private val pendingOpenPaywall = MutableStateFlow(false)
    private val resumeTick = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureSharedIntent(intent)

        setContent {
            val currentUser by remember { Firebase.auth.authStateFlow() }
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
            val sharedText by pendingSharedText.collectAsStateWithLifecycle()
            val openPaywallTrigger by pendingOpenPaywall.collectAsStateWithLifecycle()
            val resumeCount by resumeTick.collectAsStateWithLifecycle()
            var isDarkTheme by remember { mutableStateOf(true) }

            ESkyTellTheme(darkTheme = isDarkTheme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val user = currentUser
                    if (user == null) {
                        AuthScreen(onAuthSuccess = {})
                    } else {
                        key(user.uid) {
                            val isAdmin = remember(user.email) {
                                CloudAdminRepository.BOOTSTRAPPED_ADMIN_EMAIL.equals(
                                    user.email?.trim(),
                                    ignoreCase = true
                                )
                            }
                            val vm: MainViewModel = viewModel(
                                key = user.uid,
                                factory = MainViewModel.factory(
                                    context = applicationContext,
                                    userId = user.uid,
                                    userEmail = user.email.orEmpty(),
                                    isAdmin = isAdmin
                                )
                            )
                            val uiState by vm.uiState.collectAsStateWithLifecycle()

                            LaunchedEffect(uiState.isDarkTheme) {
                                isDarkTheme = uiState.isDarkTheme
                            }
                            LaunchedEffect(resumeCount) {
                                vm.refreshBluetoothStatus()
                            }
                            LaunchedEffect(sharedText) {
                                val text = sharedText
                                if (!text.isNullOrBlank()) {
                                    vm.importPastedOrSharedText(text, sourceLabel = "اشتراک‌گذاری")
                                    pendingSharedText.value = null
                                }
                            }
                            LaunchedEffect(openPaywallTrigger) {
                                if (openPaywallTrigger) {
                                    vm.openPaywallSheet()
                                    pendingOpenPaywall.value = false
                                }
                            }

                            ESkyTellApp(viewModel = vm)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        captureSharedIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        resumeTick.value += 1
    }

    private fun captureSharedIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(SubscriptionNotificationManager.EXTRA_OPEN_PAYWALL, false) == true) {
            pendingOpenPaywall.value = true
        }
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!shared.isNullOrBlank()) {
                pendingSharedText.value = shared
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ESkyTellApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_PHONE

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showIphoneGuide by remember { mutableStateOf(false) }
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showAdminStudio by remember { mutableStateOf(false) }
    var showPasswordRecovery by remember { mutableStateOf(false) }
    var legalDocToShow by remember { mutableStateOf<LegalDocumentType?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshBluetoothStatus()
        if (granted) {
            viewModel.setRenewalNotificationsEnabled(true)
            viewModel.sendTestRenewalNotification()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("مجوز اعلان‌ها برای نمایش یادآوری تمدید لازم است.")
            }
        }
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshBluetoothStatus()
        if (viewModel.bluetoothManager.checkRequiredPermissions() && uiState.isBluetoothEnabled) {
            viewModel.toggleScan()
        }
    }

    val allPermittedLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshBluetoothStatus()
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshBluetoothStatus()
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupUri(uri)
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.shareIntentEvent.collect { intent ->
            runCatching { context.startActivity(intent) }
        }
    }

    legalDocToShow?.let { docType ->
        LegalPagesScreen(
            initialTab = docType,
            onClose = { legalDocToShow = null }
        )
        return
    }

    if (showAdminStudio && uiState.isAdmin) {
        AdminStudioScreen(
            uiState = uiState,
            onSaveGlobalConfig = viewModel::saveAdminGlobalConfig,
            onSaveAnnouncement = viewModel::saveAdminAnnouncement,
            onToggleAnnouncementPublished = viewModel::toggleAdminAnnouncementPublished,
            onDeleteAnnouncement = viewModel::deleteAdminAnnouncement,
            onSetUserSubscription = viewModel::setAdminUserSubscription,
            onReviewPaymentRequest = viewModel::reviewPaymentRequest,
            onSendRenewalNotification = viewModel::sendAdminRenewalReminder,
            onClose = { showAdminStudio = false }
        )
        return
    }

    val activeCall = uiState.activeCall
    if (activeCall != null) {
        ActiveBridgeCallOverlay(
            activeCall = activeCall,
            simLabel = uiState.activeSimLabel,
            onToggleMute = viewModel::toggleActiveCallMute,
            onToggleSpeaker = viewModel::toggleActiveCallSpeaker,
            onToggleBluetoothAudio = viewModel::toggleActiveCallBluetoothAudio,
            onLaunchSystemDialer = { viewModel.placeSystemDialerCall(activeCall.phoneNumber) },
            onEndCall = viewModel::endActiveBridgeCall
        )
        return
    }

    val destinations = remember {
        listOf(
            NavDestinationItem(
                route = ROUTE_PHONE,
                label = "تلفن",
                selectedIcon = Icons.Rounded.Call,
                unselectedIcon = Icons.Outlined.Call,
                testTag = "nav_phone"
            ),
            NavDestinationItem(
                route = ROUTE_MESSAGES,
                label = "پیامک",
                selectedIcon = Icons.Rounded.Sms,
                unselectedIcon = Icons.Outlined.Sms,
                testTag = "nav_messages"
            ),
            NavDestinationItem(
                route = ROUTE_BRIDGE,
                label = "پل بلوتوث",
                selectedIcon = Icons.Rounded.BluetoothConnected,
                unselectedIcon = Icons.Outlined.BluetoothConnected,
                testTag = "nav_bridge"
            ),
            NavDestinationItem(
                route = ROUTE_MORE,
                label = "تنظیمات",
                selectedIcon = Icons.Rounded.Tune,
                unselectedIcon = Icons.Outlined.Tune,
                testTag = "nav_more"
            )
        )
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        BluetoothConnectionStatusIndicator(
                            isBluetoothEnabled = uiState.isBluetoothEnabled,
                            relayState = uiState.relayConnectionState,
                            onClick = { navigateTo(ROUTE_BRIDGE) }
                        )
                        Spacer(Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        destinations.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    if (item.route == ROUTE_MESSAGES && uiState.unreadSmsTotal > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge { Text(uiState.unreadSmsTotal.toString()) }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail {
                        destinations.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    if (item.route == ROUTE_MESSAGES && uiState.unreadSmsTotal > 0) {
                                        BadgedBox(badge = { Badge { Text(uiState.unreadSmsTotal.toString()) } }) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_PHONE
                    ) {
                        composable(ROUTE_PHONE) {
                            PhoneScreen(
                                uiState = uiState,
                                onSelectSimSlot = viewModel::selectSimSlot,
                                onAppendDigit = viewModel::appendDialerDigit,
                                onBackspaceDigit = viewModel::backspaceDialer,
                                onClearDialer = viewModel::clearDialer,
                                onStartBridgeCall = viewModel::startBridgeCall,
                                onPlaceSystemDialerCall = viewModel::placeSystemDialerCall,
                                onOpenSmsWithNumber = { number ->
                                    if (number.isNotBlank()) {
                                        viewModel.openSmsThread(number)
                                        navigateTo(ROUTE_MESSAGES)
                                    }
                                },
                                onAddManualCall = viewModel::addManualCall,
                                onDeleteCall = viewModel::deleteCall,
                                onSaveContact = viewModel::saveContact,
                                onToggleFavorite = viewModel::toggleContactFavorite,
                                onDeleteContact = viewModel::deleteContact,
                                onImportPhoneContacts = viewModel::importContactsFromPhone,
                                onRequestContactsPermission = {
                                    allPermittedLauncher.launch(viewModel.bluetoothManager.allCompliantPermissions())
                                },
                                onSaveContactToPhone = viewModel::exportContactToPhone,
                                onShareContactsBluetooth = viewModel::shareContactsViaBluetooth
                            )
                        }

                        composable(ROUTE_MESSAGES) {
                            MessagesScreen(
                                uiState = uiState,
                                onOpenThread = viewModel::openSmsThread,
                                onCloseThread = viewModel::closeSmsThread,
                                onDeleteThread = viewModel::deleteSmsThread,
                                onSelectSimSlot = viewModel::selectSimSlot,
                                onSendSms = { phone, name, body ->
                                    viewModel.sendBridgeSms(phone, name, body)
                                },
                                onOpenSystemSms = viewModel::openSystemSmsComposer,
                                onBridgeCall = viewModel::startBridgeCall,
                                onImportFileClick = {
                                    openDocumentLauncher.launch(
                                        arrayOf(
                                            "text/xml",
                                            "application/xml",
                                            "application/json",
                                            "text/csv",
                                            "text/comma-separated-values",
                                            "text/vcard",
                                            "text/x-vcard",
                                            "text/plain",
                                            "*/*"
                                        )
                                    )
                                },
                                onAddManualSms = viewModel::addManualSms,
                                onImportPastedText = { text -> viewModel.importPastedOrSharedText(text) }
                            )
                        }

                        composable(ROUTE_BRIDGE) {
                            BridgeHubScreen(
                                uiState = uiState,
                                onRequestPermissions = {
                                    btPermissionLauncher.launch(viewModel.bluetoothManager.requiredPermissions())
                                },
                                onOpenPermissionsSheet = { showPermissionsSheet = true },
                                onRequestEnableBluetooth = {
                                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                                },
                                onToggleScan = viewModel::toggleScan,
                                onToggleReceiveServer = {
                                    if (!uiState.hasBluetoothPermissions) {
                                        btPermissionLauncher.launch(viewModel.bluetoothManager.requiredPermissions())
                                    } else if (!uiState.isBluetoothEnabled) {
                                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                                    } else {
                                        viewModel.toggleReceiveServer()
                                    }
                                },
                                onSelectBridgeSource = viewModel::selectBridgeSource,
                                onSelectRelayRole = viewModel::setRelayDeviceRole,
                                onConnectDevice = viewModel::connectToDevice,
                                onDisconnectPeer = viewModel::disconnectBridgePeer,
                                onPairDevice = viewModel::pairDevice,
                                onSendToDevice = viewModel::sendToDevice,
                                onQuickExportShare = { viewModel.sendViaStandardBluetoothExport() },
                                onSelectMode = viewModel::setTransferMode,
                                onSelectFormat = viewModel::setExportFormat,
                                onToggleEncryption = viewModel::setEncryptionEnabled,
                                onRegeneratePin = viewModel::regeneratePairingPin,
                                onToggleQuietHours = viewModel::toggleQuietHours,
                                onToggleAutoReconnect = viewModel::setAutoReconnectEnabled,
                                onCancelAutoReconnect = viewModel::cancelAutoReconnect,
                                onOpenIphoneGuide = { showIphoneGuide = true },
                                onOpenHistorySheet = { showHistorySheet = true },
                                onDismissTransferBanner = viewModel::clearTransferBanner,
                                onDismissIncomingEvent = viewModel::dismissIncomingBridgeEvent,
                                onBridgeCallNumber = viewModel::startBridgeCall,
                                onOpenSystemSms = viewModel::openSystemSmsComposer
                            )
                        }

                        composable(ROUTE_MORE) {
                            MoreAndAccountScreen(
                                uiState = uiState,
                                onOpenAdminStudio = { showAdminStudio = true },
                                onOpenPaywall = viewModel::openPaywallSheet,
                                onOpenPermissionsSheet = { showPermissionsSheet = true },
                                onOpenHistorySheet = { showHistorySheet = true },
                                onToggleTheme = viewModel::toggleTheme,
                                onToggleAutoReconnect = viewModel::setAutoReconnectEnabled,
                                onToggleRenewalNotifications = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.hasNotificationPermission()) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setRenewalNotificationsEnabled(enabled)
                                    }
                                },
                                onSendTestRenewalNotification = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !viewModel.hasNotificationPermission()) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.sendTestRenewalNotification()
                                    }
                                },
                                onOpenPasswordRecovery = { showPasswordRecovery = true },
                                onOpenLegalDocument = { doc -> legalDocToShow = doc },
                                onSaveSimLabels = viewModel::updateUserSimLabels,
                                onOpenExternalAction = viewModel::openExternalAction,
                                onSignOut = {
                                    signOutGoogle(credentialManager = credentialManager, scope = scope)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showIphoneGuide) {
        IphoneGuideSheet(onDismiss = { showIphoneGuide = false })
    }

    if (showPermissionsSheet) {
        PermissionsManagerSheet(
            summary = uiState.permissionSummary,
            onRequestAllPermitted = {
                allPermittedLauncher.launch(viewModel.bluetoothManager.allCompliantPermissions())
            },
            onDismiss = { showPermissionsSheet = false }
        )
    }

    if (showHistorySheet) {
        HistoryBottomSheet(
            history = uiState.transferHistory,
            onDeleteEntry = viewModel::deleteHistory,
            onClearHistory = viewModel::clearHistory,
            onDismiss = { showHistorySheet = false }
        )
    }

    if (showPasswordRecovery) {
        PasswordRecoveryDialog(
            initialEmail = uiState.currentUserEmail,
            currentPairingPin = uiState.pairingPin,
            onRegeneratePairingPin = viewModel::regeneratePairingPin,
            onDismiss = { showPasswordRecovery = false }
        )
    }

    if (uiState.showPaywallSheet) {
        PaywallBottomSheet(
            appConfig = uiState.appConfig,
            userPaymentRequests = uiState.userPaymentRequests,
            isCloudSaving = uiState.isCloudSaving,
            onOpenPaymentUrl = { url -> viewModel.openExternalAction(url) },
            onSubmitPaymentRequest = viewModel::submitPaymentRequest,
            onNotifyCopied = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
            onDismiss = viewModel::dismissPaywallSheet
        )
    }
}

@Composable
private fun BluetoothConnectionStatusIndicator(
    isBluetoothEnabled: Boolean,
    relayState: RelayConnectionState,
    onClick: () -> Unit
) {
    val isPulsing = relayState is RelayConnectionState.Connecting ||
        relayState is RelayConnectionState.Reconnecting ||
        relayState is RelayConnectionState.Relaying ||
        relayState is RelayConnectionState.Listening

    val pulseTransition = rememberInfiniteTransition(label = "bt_status_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = if (isPulsing) 0.35f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bt_pulse_alpha"
    )

    val statusColor: Color
    val statusText: String
    val statusIcon: ImageVector

    when {
        !isBluetoothEnabled -> {
            statusColor = RoseError
            statusText = "خاموش"
            statusIcon = Icons.Rounded.BluetoothDisabled
        }
        relayState is RelayConnectionState.Connected -> {
            statusColor = EmeraldSuccess
            statusText = "متصل"
            statusIcon = Icons.Rounded.BluetoothConnected
        }
        relayState is RelayConnectionState.Relaying -> {
            statusColor = MintJade
            statusText = "رله زنده"
            statusIcon = Icons.Rounded.BluetoothConnected
        }
        relayState is RelayConnectionState.Reconnecting -> {
            statusColor = AmberWarning
            statusText = "تلاش ${relayState.attempt}/${relayState.maxAttempts}"
            statusIcon = Icons.Rounded.BluetoothSearching
        }
        relayState is RelayConnectionState.Connecting -> {
            statusColor = AmberWarning
            statusText = "در حال اتصال"
            statusIcon = Icons.Rounded.BluetoothSearching
        }
        relayState is RelayConnectionState.Listening -> {
            statusColor = MintJade
            statusText = "آماده"
            statusIcon = Icons.Rounded.BluetoothSearching
        }
        relayState is RelayConnectionState.Error -> {
            statusColor = RoseError
            statusText = "خطا"
            statusIcon = Icons.Rounded.BluetoothDisabled
        }
        else -> {
            statusColor = MaterialTheme.colorScheme.onSurfaceVariant
            statusText = "قطع"
            statusIcon = Icons.Rounded.Bluetooth
        }
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = statusColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
        modifier = Modifier.testTag("global_bluetooth_status_indicator")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(if (isPulsing) pulseAlpha else 1f)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Icon(
                imageVector = statusIcon,
                contentDescription = "وضعیت اتصال بلوتوث: $statusText",
                tint = statusColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
