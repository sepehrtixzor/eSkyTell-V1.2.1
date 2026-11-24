package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BridgeCommandType
import com.example.data.model.DevicePlatform
import com.example.data.model.DiscoveredBluetoothDevice
import com.example.data.model.ExportFormat
import com.example.data.model.RelayConnectionState
import com.example.data.model.RelayDeviceRole
import com.example.data.model.TransferMode
import com.example.data.model.TransferSessionState
import com.example.ui.MainUiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintJade
import com.example.ui.theme.RoseError

@Composable
fun BridgeHubScreen(
    uiState: MainUiState,
    onRequestPermissions: () -> Unit,
    onOpenPermissionsSheet: () -> Unit,
    onRequestEnableBluetooth: () -> Unit,
    onToggleScan: () -> Unit,
    onToggleReceiveServer: () -> Unit,
    onSelectBridgeSource: (String) -> Unit,
    onSelectRelayRole: (RelayDeviceRole) -> Unit,
    onConnectDevice: (DiscoveredBluetoothDevice) -> Unit,
    onDisconnectPeer: () -> Unit,
    onPairDevice: (DiscoveredBluetoothDevice) -> Unit,
    onSendToDevice: (DiscoveredBluetoothDevice) -> Unit,
    onQuickExportShare: () -> Unit,
    onSelectMode: (TransferMode) -> Unit,
    onSelectFormat: (ExportFormat) -> Unit,
    onToggleEncryption: (Boolean) -> Unit,
    onRegeneratePin: () -> Unit,
    onToggleQuietHours: (Boolean) -> Unit,
    onToggleAutoReconnect: (Boolean) -> Unit,
    onCancelAutoReconnect: () -> Unit,
    onOpenIphoneGuide: () -> Unit,
    onOpenHistorySheet: () -> Unit,
    onDismissTransferBanner: () -> Unit,
    onDismissIncomingEvent: () -> Unit,
    onBridgeCallNumber: (String, String) -> Unit,
    onOpenSystemSms: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allDevices = uiState.allKnownDevices
    val isListening = uiState.transferState is TransferSessionState.Listening

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Obsidian Jade Hero Bridge Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MintJade.copy(alpha = 0.45f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_eskytell_obsidian),
                            contentDescription = "پل هوشمند eSkyTell",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(205.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(205.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x9906110E),
                                            Color(0xCC09110F),
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val relayStatus = uiState.relayConnectionState
                                val pillColor = when {
                                    !uiState.isBluetoothSupported -> MaterialTheme.colorScheme.errorContainer
                                    relayStatus is RelayConnectionState.Connected ||
                                        relayStatus is RelayConnectionState.Relaying -> EmeraldSuccess.copy(alpha = 0.28f)
                                    relayStatus is RelayConnectionState.Reconnecting ||
                                        relayStatus is RelayConnectionState.Connecting -> AmberWarning.copy(alpha = 0.25f)
                                    relayStatus is RelayConnectionState.Error -> RoseError.copy(alpha = 0.26f)
                                    uiState.isBluetoothEnabled -> MintJade.copy(alpha = 0.22f)
                                    else -> AmberWarning.copy(alpha = 0.22f)
                                }
                                val pillIcon = when {
                                    !uiState.isBluetoothEnabled -> Icons.Rounded.BluetoothDisabled
                                    relayStatus is RelayConnectionState.Connected ||
                                        relayStatus is RelayConnectionState.Relaying -> Icons.Rounded.BluetoothConnected
                                    relayStatus is RelayConnectionState.Reconnecting ||
                                        relayStatus is RelayConnectionState.Connecting -> Icons.Rounded.BluetoothSearching
                                    relayStatus is RelayConnectionState.Error -> Icons.Rounded.ErrorOutline
                                    else -> Icons.Rounded.Bluetooth
                                }
                                val pillTint = when {
                                    relayStatus is RelayConnectionState.Connected ||
                                        relayStatus is RelayConnectionState.Relaying -> EmeraldSuccess
                                    relayStatus is RelayConnectionState.Reconnecting ||
                                        relayStatus is RelayConnectionState.Connecting -> AmberWarning
                                    relayStatus is RelayConnectionState.Error -> RoseError
                                    uiState.isBluetoothEnabled -> MintJade
                                    else -> AmberWarning
                                }
                                val pillText = when {
                                    !uiState.isBluetoothSupported -> "بدون بلوتوث"
                                    !uiState.isBluetoothEnabled -> "بلوتوث خاموش"
                                    relayStatus is RelayConnectionState.Connected -> "متصل به ${relayStatus.deviceName}"
                                    relayStatus is RelayConnectionState.Relaying -> "در حال رله داده"
                                    relayStatus is RelayConnectionState.Reconnecting -> "اتصال مجدد (${relayStatus.attempt}/${relayStatus.maxAttempts})"
                                    relayStatus is RelayConnectionState.Connecting -> "در حال اتصال..."
                                    relayStatus is RelayConnectionState.Listening -> "آماده دریافت"
                                    relayStatus is RelayConnectionState.Error -> "خطای اتصال"
                                    else -> "بلوتوث آماده"
                                }
                                Surface(
                                    color = pillColor,
                                    shape = CircleShape,
                                    modifier = Modifier.testTag("bridge_hero_status_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = pillIcon,
                                            contentDescription = "وضعیت اتصال بلوتوث",
                                            tint = pillTint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = pillText,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        onClick = onOpenHistorySheet,
                                        color = Color(0xAA0A1613),
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f)),
                                        modifier = Modifier.testTag("bridge_history_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.History, contentDescription = "تاریخچه", tint = ChampagneGold, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("تاریخچه", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                        }
                                    }

                                    Surface(
                                        onClick = onOpenPermissionsSheet,
                                        color = Color(0xAA0A1613),
                                        shape = CircleShape,
                                        border = BorderStroke(
                                            1.dp,
                                            if (uiState.permissionSummary.allGranted) EmeraldSuccess.copy(alpha = 0.6f)
                                            else AmberWarning.copy(alpha = 0.7f)
                                        ),
                                        modifier = Modifier.testTag("bridge_permissions_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Rounded.Security,
                                                contentDescription = "مجوزها",
                                                tint = if (uiState.permissionSummary.allGranted) EmeraldSuccess else AmberWarning,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("مجوزها", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                        }
                                    }

                                    Surface(
                                        onClick = onOpenIphoneGuide,
                                        color = Color(0xAA0A1613),
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, MintJade.copy(alpha = 0.55f)),
                                        modifier = Modifier.testTag("bridge_guide_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.HelpOutline, contentDescription = "راهنمای آیفون", tint = MintJade, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("راهنما", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "پل هوشمند eSkyTell",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "مدیریت اتصال دوطرفه با آیفون و گوشی دوم • ${uiState.smsRecords.size} پیامک و ${uiState.callRecords.size} تماس آماده همگام‌سازی",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD5EBE5)
                            )

                            OutlinedButton(
                                onClick = onToggleReceiveServer,
                                border = BorderStroke(1.dp, if (isListening) EmeraldSuccess else MintJade),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isListening) EmeraldSuccess.copy(alpha = 0.2f) else Color(0x880B1815)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("bridge_receive_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WifiTethering,
                                    contentDescription = null,
                                    tint = if (isListening) EmeraldSuccess else MintJade,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isListening) "پل زنده فعال است (لمس برای توقف)" else "فعال‌سازی حالت آماده دریافت زنده (BLE + RFCOMM)",
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 2. Live Incoming Bridge Event Banner
            item {
                AnimatedVisibility(visible = uiState.latestIncomingBridgeEvent != null) {
                    val event = uiState.latestIncomingBridgeEvent
                    if (event != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.16f)),
                            border = BorderStroke(1.dp, EmeraldSuccess),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (event.type) {
                                            BridgeCommandType.CALL_TRANSFER -> "فرمان تماس دریافتی از ${event.sourceDeviceName}"
                                            BridgeCommandType.SMS_RELAY -> "پیامک دریافتی از ${event.sourceDeviceName}"
                                            BridgeCommandType.CONTACT_SYNC -> "مخاطب جدید از ${event.sourceDeviceName}"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = onDismissIncomingEvent) {
                                        Icon(Icons.Rounded.Close, contentDescription = "بستن")
                                    }
                                }
                                Text(
                                    text = "${event.contactName} (${event.phoneNumber}) ${if (event.messageBody.isNotBlank()) "— ${event.messageBody}" else ""}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (event.phoneNumber.isNotBlank()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledTonalButton(onClick = { onBridgeCallNumber(event.phoneNumber, event.contactName) }) {
                                            Text("تماس")
                                        }
                                        OutlinedButton(onClick = { onOpenSystemSms(event.phoneNumber, event.messageBody) }) {
                                            Text("پاسخ پیامکی")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Active Session Progress / Status Banner
            item {
                AnimatedVisibility(
                    visible = uiState.transferState !is TransferSessionState.Idle,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    TransferStatusBanner(
                        state = uiState.transferState,
                        onDismiss = onDismissTransferBanner,
                        onStopListening = onToggleReceiveServer,
                        onCancelReconnect = onCancelAutoReconnect
                    )
                }
            }

            // 4. Missing Bluetooth / Permissions Banner
            if (!uiState.isBluetoothSupported || !uiState.hasBluetoothPermissions || !uiState.isBluetoothEnabled || !uiState.permissionSummary.allGranted) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.55f)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = when {
                                        !uiState.isBluetoothSupported -> "بلوتوث در این دستگاه پشتیبانی نمی‌شود"
                                        !uiState.hasBluetoothPermissions -> "تکمیل دسترسی‌های بلوتوث"
                                        !uiState.isBluetoothEnabled -> "بلوتوث خاموش است"
                                        else -> "تکمیل مجوزهای مخاطبین و اعلان"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "برای اتصال پایدار به آیفون و دریافت اعلان تماس/پیامک، مجوزها و بلوتوث را فعال کنید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Button(
                                onClick = if (uiState.isBluetoothSupported && uiState.hasBluetoothPermissions && !uiState.isBluetoothEnabled) {
                                    onRequestEnableBluetooth
                                } else {
                                    onOpenPermissionsSheet
                                },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("bridge_setup_action_btn")
                            ) {
                                Text(
                                    if (uiState.isBluetoothSupported && uiState.hasBluetoothPermissions && !uiState.isBluetoothEnabled) "روشن کردن"
                                    else "تأیید مجوزها"
                                )
                            }
                        }
                    }
                }
            }

            // 4b. Primary / Secondary Role & Persistent Relay Link Card
            item {
                val connectedState = uiState.relayConnectionState as? RelayConnectionState.Connected
                val reconnectingState = uiState.relayConnectionState as? RelayConnectionState.Reconnecting
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        when {
                            connectedState != null -> EmeraldSuccess.copy(alpha = 0.7f)
                            reconnectingState != null -> AmberWarning.copy(alpha = 0.75f)
                            uiState.relayConnectionState is RelayConnectionState.Error -> RoseError.copy(alpha = 0.65f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                        }
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "نقش دستگاه در رله داده",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (val st = uiState.relayConnectionState) {
                                        is RelayConnectionState.Connected ->
                                            "متصل به ${st.deviceName} (${st.transport.labelFa})"
                                        is RelayConnectionState.Listening ->
                                            "در حال انتظار اتصال (${st.role.labelFa})"
                                        is RelayConnectionState.Connecting ->
                                            "در حال برقراری ارتباط با ${st.deviceName}..."
                                        is RelayConnectionState.Reconnecting ->
                                            "سیگنال قطع شد — تلاش مجدد ${st.attempt} از ${st.maxAttempts} برای ${st.deviceName}..."
                                        is RelayConnectionState.Relaying ->
                                            "در حال رله داده (${(st.progress * 100).toInt()}%)..."
                                        is RelayConnectionState.Error -> st.message
                                        RelayConnectionState.Disconnected ->
                                            uiState.relayDeviceRole.subtitleFa
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (uiState.relayConnectionState) {
                                        is RelayConnectionState.Connected -> EmeraldSuccess
                                        is RelayConnectionState.Reconnecting -> AmberWarning
                                        is RelayConnectionState.Error -> RoseError
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            if (connectedState != null) {
                                OutlinedButton(
                                    onClick = onDisconnectPeer,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("bridge_disconnect_peer_btn")
                                ) {
                                    Text("قطع")
                                }
                            } else if (reconnectingState != null) {
                                OutlinedButton(
                                    onClick = onCancelAutoReconnect,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("bridge_cancel_reconnect_btn")
                                ) {
                                    Text("لغو تلاش")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RelayDeviceRole.entries.forEach { role ->
                                val selected = uiState.relayDeviceRole == role
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectRelayRole(role) },
                                    label = { Text(role.labelFa) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("bridge_role_${role.name}")
                                )
                            }
                        }
                    }
                }
            }

            // 5. Security PIN + Quiet Hours Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MintJade, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("رمزنگاری بلوتوثی AES-256", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "پین جلسه: ${uiState.pairingPin}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = ChampagneGold
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onRegeneratePin, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "تغییر پین")
                                }
                                Switch(
                                    checked = uiState.encryptionEnabled,
                                    onCheckedChange = onToggleEncryption,
                                    modifier = Modifier.testTag("bridge_encryption_switch")
                                )
                            }
                        }

                        if (uiState.appConfig.enableQuietHours) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("حالت سکوت پل (Quiet Hours)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "عدم نمایش پاپ‌آپ اعلان‌های ورودی در زمان استراحت",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = uiState.isQuietHoursEnabled,
                                    onCheckedChange = onToggleQuietHours,
                                    modifier = Modifier.testTag("bridge_quiet_hours_switch")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, tint = MintJade, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("اتصال مجدد خودکار بلوتوث", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "تلاش خودکار برای برقراری مجدد ارتباط هنگام قطع ناگهانی سیگنال",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.autoReconnectEnabled,
                                onCheckedChange = onToggleAutoReconnect,
                                modifier = Modifier.testTag("bridge_auto_reconnect_switch")
                            )
                        }
                    }
                }
            }

            // 6. Transfer Modes & Multi-Format Export
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "پروتکل انتقال و خروجی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalButton(
                            onClick = onQuickExportShare,
                            modifier = Modifier.testTag("bridge_quick_export_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("اشتراک‌گذاری فوری")
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(TransferMode.entries) { mode ->
                            val isSelected = uiState.transferMode == mode
                            Surface(
                                onClick = { onSelectMode(mode) },
                                shape = MaterialTheme.shapes.medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MintJade else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                                ),
                                modifier = Modifier.width(210.dp).testTag("bridge_mode_${mode.name}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = mode.titleFa,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MintJade else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = mode.subtitleFa,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.transferMode == TransferMode.CROSS_PLATFORM_OPP) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ExportFormat.entries) { format ->
                                FilterChip(
                                    selected = uiState.exportFormat == format,
                                    onClick = { onSelectFormat(format) },
                                    label = { Text(format.titleFa) },
                                    modifier = Modifier.testTag("bridge_format_${format.name}")
                                )
                            }
                        }
                    }
                }
            }

            // 7. Devices Header & Scan Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "آیفون‌ها و دستگاه‌های بلوتوثی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isScanning) "در حال اسکن بلوتوث..." else "${allDevices.size} دستگاه در دسترس",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            when {
                                !uiState.hasBluetoothPermissions -> onRequestPermissions()
                                !uiState.isBluetoothEnabled -> onRequestEnableBluetooth()
                                else -> onToggleScan()
                            }
                        },
                        enabled = uiState.isBluetoothSupported,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("bridge_scan_btn")
                    ) {
                        Icon(
                            imageVector = if (uiState.isScanning) Icons.Rounded.Close else Icons.Rounded.BluetoothSearching,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (uiState.isScanning) "توقف اسکن" else "جستجو")
                    }
                }
            }

            // 8. Device List or Radar Empty State
            if (allDevices.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_devices),
                                contentDescription = "رادار اسکن بلوتوث",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                            Icon(
                                imageVector = if (uiState.isScanning) Icons.Rounded.Radar else Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = MintJade
                            )
                            Text(
                                text = if (uiState.isScanning) "در حال جستجوی آیفون و گوشی‌های نزدیک..." else "دستگاهی شناسایی نشده است",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "بلوتوث آیفون یا گوشی دوم خود را روشن کنید و دکمه «جستجو» را بزنید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(allDevices, key = { it.address }) { device ->
                    val isActiveSource = uiState.activeBridgeDevice?.address == device.address
                    val connectedAddress = (uiState.relayConnectionState as? RelayConnectionState.Connected)?.address
                    val isRelayConnected = connectedAddress == device.address || device.isConnected
                    BridgeDeviceCard(
                        device = device,
                        isActiveSource = isActiveSource,
                        isRelayConnected = isRelayConnected,
                        transferMode = uiState.transferMode,
                        onSetActive = { onSelectBridgeSource(device.address) },
                        onConnectOrDisconnect = {
                            if (isRelayConnected) onDisconnectPeer() else onConnectDevice(device)
                        },
                        onPair = { onPairDevice(device) },
                        onSend = { onSendToDevice(device) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun BridgeDeviceCard(
    device: DiscoveredBluetoothDevice,
    isActiveSource: Boolean,
    isRelayConnected: Boolean,
    transferMode: TransferMode,
    onSetActive: () -> Unit,
    onConnectOrDisconnect: () -> Unit,
    onPair: () -> Unit,
    onSend: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isActiveSource || isRelayConnected) 1.5.dp else 1.dp,
            color = when {
                isRelayConnected -> EmeraldSuccess
                isActiveSource -> MintJade
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
            }
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSetActive() }
            .testTag("device_card_${device.address}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        isRelayConnected -> EmeraldSuccess.copy(alpha = 0.2f)
                        isActiveSource -> MintJade.copy(alpha = 0.2f)
                        device.platform == DevicePlatform.IPHONE -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (device.platform) {
                                DevicePlatform.IPHONE -> Icons.Rounded.PhoneIphone
                                DevicePlatform.ANDROID -> Icons.Rounded.Android
                                DevicePlatform.COMPUTER -> Icons.Rounded.Computer
                                DevicePlatform.OTHER -> Icons.Rounded.DevicesOther
                            },
                            contentDescription = device.platform.labelFa,
                            tint = when {
                                isRelayConnected -> EmeraldSuccess
                                isActiveSource -> MintJade
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isActiveSource) {
                            Surface(color = MintJade.copy(alpha = 0.18f), shape = CircleShape) {
                                Text(
                                    text = "فعال",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MintJade,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(device.platform.labelFa, style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                        if (isRelayConnected) {
                            Text("• متصل", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                        } else if (device.isPaired) {
                            Text("• جفت‌شده", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                        }
                        if (device.rssi != null) {
                            Text(
                                text = "• ${device.rssi} dBm",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!device.isPaired) {
                    IconButton(onClick = onPair, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Link, contentDescription = "جفت‌سازی", tint = ChampagneGold)
                    }
                }
                OutlinedButton(
                    onClick = onConnectOrDisconnect,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("connect_to_${device.address}")
                ) {
                    Text(if (isRelayConnected) "قطع" else "اتصال")
                }
                FilledTonalButton(
                    onClick = onSend,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("send_to_${device.address}")
                ) {
                    Text(
                        when (transferMode) {
                            TransferMode.BLE_GATT_BRIDGE -> "ارسال BLE"
                            TransferMode.DIRECT_RFCOMM -> "ارسال مستقیم"
                            TransferMode.CROSS_PLATFORM_OPP -> "ارسال فایل"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferStatusBanner(
    state: TransferSessionState,
    onDismiss: () -> Unit,
    onStopListening: () -> Unit,
    onCancelReconnect: () -> Unit
) {
    val containerColor = when (state) {
        is TransferSessionState.Success -> EmeraldSuccess.copy(alpha = 0.16f)
        is TransferSessionState.Error -> MaterialTheme.colorScheme.errorContainer
        is TransferSessionState.Reconnecting -> AmberWarning.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = when (state) {
                            is TransferSessionState.Success -> Icons.Rounded.CheckCircle
                            is TransferSessionState.Error -> Icons.Rounded.ErrorOutline
                            is TransferSessionState.Reconnecting -> Icons.Rounded.BluetoothSearching
                            else -> Icons.Rounded.BluetoothConnected
                        },
                        contentDescription = null,
                        tint = when (state) {
                            is TransferSessionState.Success -> EmeraldSuccess
                            is TransferSessionState.Error -> MaterialTheme.colorScheme.error
                            is TransferSessionState.Reconnecting -> AmberWarning
                            else -> MintJade
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (state) {
                            is TransferSessionState.Listening -> "پل زنده روی «${state.deviceName}» آماده دریافت..."
                            is TransferSessionState.Connecting -> "در حال اتصال به ${state.targetName}..."
                            is TransferSessionState.Reconnecting -> "تلاش مجدد برای اتصال به «${state.targetName}» (${state.attempt}/${state.maxAttempts})..."
                            is TransferSessionState.Transferring -> "انتقال به ${state.targetName} (${(state.progress * 100).toInt()}%)"
                            is TransferSessionState.Success -> state.message
                            is TransferSessionState.Error -> state.message
                            TransferSessionState.Idle -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (state is TransferSessionState.Listening) {
                    TextButton(onClick = onStopListening) { Text("توقف") }
                } else if (state is TransferSessionState.Reconnecting) {
                    TextButton(onClick = onCancelReconnect) { Text("لغو") }
                } else if (state is TransferSessionState.Success || state is TransferSessionState.Error) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "بستن")
                    }
                }
            }
            if (state is TransferSessionState.Transferring) {
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth().clip(CircleShape))
            } else if (state is TransferSessionState.Listening ||
                state is TransferSessionState.Connecting ||
                state is TransferSessionState.Reconnecting
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(CircleShape))
            }
        }
    }
}
