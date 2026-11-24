package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PhoneEnabled
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CallRecordEntity
import com.example.data.local.ContactEntity
import com.example.data.model.CallDirection
import com.example.ui.MainUiState
import com.example.ui.components.AddCallDialog
import com.example.ui.components.AddEditContactDialog
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintJade
import com.example.ui.theme.RoseError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class KeypadButtonSpec(
    val digit: String,
    val subText: String
)

@Composable
fun PhoneScreen(
    uiState: MainUiState,
    onSelectSimSlot: (Int) -> Unit,
    onAppendDigit: (String) -> Unit,
    onBackspaceDigit: () -> Unit,
    onClearDialer: () -> Unit,
    onStartBridgeCall: (phone: String, name: String) -> Unit,
    onPlaceSystemDialerCall: (phone: String) -> Unit,
    onOpenSmsWithNumber: (phone: String) -> Unit,
    onAddManualCall: (contact: String, phone: String, duration: Int, direction: CallDirection) -> Unit,
    onDeleteCall: (Long) -> Unit,
    onSaveContact: (name: String, phone: String, email: String, notes: String, isFavorite: Boolean, existingId: Long?) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onDeleteContact: (Long) -> Unit,
    onImportPhoneContacts: () -> Unit,
    onRequestContactsPermission: () -> Unit,
    onSaveContactToPhone: (ContactEntity) -> Unit,
    onShareContactsBluetooth: (List<ContactEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneSubTab by remember { mutableIntStateOf(0) } // 0=Keypad, 1=Recents, 2=Contacts
    var showAddCallDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var prefillPhoneForContact by remember { mutableStateOf("") }
    var editingContact by remember { mutableStateOf<ContactEntity?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top SIM & Bridge Status Pill Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.appConfig.enableDualSim) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SimPillChip(
                            selected = uiState.activeSimSlot == 1,
                            label = uiState.sim1DisplayLabel,
                            onClick = { onSelectSimSlot(1) },
                            testTag = "sim_1_chip"
                        )
                        SimPillChip(
                            selected = uiState.activeSimSlot == 2,
                            label = uiState.sim2DisplayLabel,
                            onClick = { onSelectSimSlot(2) },
                            testTag = "sim_2_chip"
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Surface(
                    color = if (uiState.activeBridgeDevice != null) {
                        EmeraldSuccess.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BluetoothConnected,
                            contentDescription = null,
                            tint = if (uiState.activeBridgeDevice != null) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = uiState.activeBridgeDevice?.name ?: "بدون پل متصل",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Segmented Control (Keypad | Recents | Contacts) - familiar iOS simplicity
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        Triple(0, "شماره‌گیر", "phone_subtab_keypad"),
                        Triple(1, "تماس‌های اخیر", "phone_subtab_recents"),
                        Triple(2, "مخاطبین", "phone_subtab_contacts")
                    )
                    tabs.forEach { (idx, title, tag) ->
                        val active = phoneSubTab == idx
                        Surface(
                            onClick = { phoneSubTab = idx },
                            shape = CircleShape,
                            color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag(tag)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            when (phoneSubTab) {
                0 -> KeypadPane(
                    dialerNumber = uiState.dialerNumber,
                    onAppendDigit = onAppendDigit,
                    onBackspaceDigit = onBackspaceDigit,
                    onClearDialer = onClearDialer,
                    onAddToContacts = {
                        prefillPhoneForContact = uiState.dialerNumber
                        showAddContactDialog = true
                    },
                    onStartBridgeCall = { onStartBridgeCall(uiState.dialerNumber, "") },
                    onPlaceSystemDialerCall = { onPlaceSystemDialerCall(uiState.dialerNumber) },
                    onQuickSms = { onOpenSmsWithNumber(uiState.dialerNumber) },
                    modifier = Modifier.weight(1f)
                )

                1 -> RecentsPane(
                    callRecords = uiState.callRecords,
                    onAddCallClick = { showAddCallDialog = true },
                    onBridgeCallback = { call -> onStartBridgeCall(call.phoneNumber, call.contactName) },
                    onSystemCallback = { call -> onPlaceSystemDialerCall(call.phoneNumber) },
                    onSmsCallback = { call -> onOpenSmsWithNumber(call.phoneNumber) },
                    onSaveContact = { call ->
                        prefillPhoneForContact = call.phoneNumber
                        showAddContactDialog = true
                    },
                    onDeleteCall = onDeleteCall,
                    modifier = Modifier.weight(1f)
                )

                else -> ContactsPane(
                    contacts = uiState.contacts,
                    contactsPermissionGranted = uiState.permissionSummary.contactsGranted,
                    onAddContactClick = {
                        prefillPhoneForContact = ""
                        showAddContactDialog = true
                    },
                    onImportPhoneContacts = onImportPhoneContacts,
                    onRequestContactsPermission = onRequestContactsPermission,
                    onShareContactsBluetooth = onShareContactsBluetooth,
                    onBridgeCallContact = { c -> onStartBridgeCall(c.phoneNumber, c.displayName) },
                    onSmsContact = { c -> onOpenSmsWithNumber(c.phoneNumber) },
                    onToggleFavorite = onToggleFavorite,
                    onSaveContactToPhone = onSaveContactToPhone,
                    onEditContact = { editingContact = it },
                    onDeleteContact = onDeleteContact,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showAddCallDialog) {
        AddCallDialog(
            onDismiss = { showAddCallDialog = false },
            onConfirm = onAddManualCall
        )
    }

    if (showAddContactDialog) {
        AddEditContactDialog(
            initialPhone = prefillPhoneForContact,
            onDismiss = { showAddContactDialog = false },
            onConfirm = onSaveContact
        )
    }

    editingContact?.let { contact ->
        AddEditContactDialog(
            initialContact = contact,
            onDismiss = { editingContact = null },
            onConfirm = onSaveContact
        )
    }
}

@Composable
private fun SimPillChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) ChampagneGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) ChampagneGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SimCard,
                contentDescription = null,
                tint = if (selected) ChampagneGold else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) ChampagneGold else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadPane(
    dialerNumber: String,
    onAppendDigit: (String) -> Unit,
    onBackspaceDigit: () -> Unit,
    onClearDialer: () -> Unit,
    onAddToContacts: () -> Unit,
    onStartBridgeCall: () -> Unit,
    onPlaceSystemDialerCall: () -> Unit,
    onQuickSms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = remember {
        listOf(
            listOf(
                KeypadButtonSpec("1", " "),
                KeypadButtonSpec("2", "ABC"),
                KeypadButtonSpec("3", "DEF")
            ),
            listOf(
                KeypadButtonSpec("4", "GHI"),
                KeypadButtonSpec("5", "JKL"),
                KeypadButtonSpec("6", "MNO")
            ),
            listOf(
                KeypadButtonSpec("7", "PQRS"),
                KeypadButtonSpec("8", "TUV"),
                KeypadButtonSpec("9", "WXYZ")
            ),
            listOf(
                KeypadButtonSpec("*", ""),
                KeypadButtonSpec("0", "+"),
                KeypadButtonSpec("#", "")
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Number Display Area
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = dialerNumber.ifEmpty { "شماره را وارد کنید" },
                    style = if (dialerNumber.isEmpty()) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = if (dialerNumber.length > 13) 24.sp else 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    color = if (dialerNumber.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (dialerNumber.isNotBlank()) {
                TextButton(
                    onClick = onAddToContacts,
                    modifier = Modifier.testTag("dialer_add_contact_btn")
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("افزودن به مخاطبین")
                }
            } else {
                Spacer(Modifier.height(36.dp))
            }
        }

        // 3x4 Circular Keypad (Forced LTR so 1-2-3 is natural iOS layout)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                rows.forEach { rowSpecs ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowSpecs.forEach { spec ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .size(74.dp)
                                    .combinedClickable(
                                        onClick = { onAppendDigit(spec.digit) },
                                        onLongClick = {
                                            if (spec.digit == "0") onAppendDigit("+")
                                        }
                                    )
                                    .testTag("keypad_${spec.digit}")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = spec.digit,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 26.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (spec.subText.isNotBlank()) {
                                        Text(
                                            text = spec.subText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFamily,
                                                fontSize = 10.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Call / SMS / Dialer / Backspace Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            // Open native dialer / SMS quick action
            IconButton(
                onClick = onQuickSms,
                enabled = dialerNumber.isNotBlank(),
                modifier = Modifier
                    .size(54.dp)
                    .testTag("dialer_sms_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sms,
                    contentDescription = "ارسال پیامک به این شماره",
                    tint = if (dialerNumber.isNotBlank()) ChampagneGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }

            // Green iOS-like Circular Call Button (triggers Bridge Call HUD)
            Button(
                onClick = onStartBridgeCall,
                enabled = dialerNumber.isNotBlank(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldSuccess,
                    contentColor = Color(0xFF02241A)
                ),
                modifier = Modifier
                    .size(72.dp)
                    .testTag("dialer_bridge_call_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "برقراری تماس با پل eSkyTell",
                    modifier = Modifier.size(30.dp)
                )
            }

            // Backspace (long press clears)
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .size(54.dp)
                    .combinedClickable(
                        enabled = dialerNumber.isNotBlank(),
                        onClick = onBackspaceDigit,
                        onLongClick = onClearDialer
                    )
                    .testTag("dialer_backspace_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                        contentDescription = "پاک کردن رقم",
                        tint = if (dialerNumber.isNotBlank()) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }

        if (dialerNumber.isNotBlank()) {
            OutlinedButton(
                onClick = onPlaceSystemDialerCall,
                modifier = Modifier.testTag("dialer_native_call_btn")
            ) {
                Icon(Icons.Rounded.Dialpad, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("شماره‌گیری مستقیم از سیم‌کارت گوشی")
            }
        }
    }
}

@Composable
private fun RecentsPane(
    callRecords: List<CallRecordEntity>,
    onAddCallClick: () -> Unit,
    onBridgeCallback: (CallRecordEntity) -> Unit,
    onSystemCallback: (CallRecordEntity) -> Unit,
    onSmsCallback: (CallRecordEntity) -> Unit,
    onSaveContact: (CallRecordEntity) -> Unit,
    onDeleteCall: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var missedOnly by remember { mutableStateOf(false) }
    val filtered = remember(callRecords, missedOnly) {
        if (missedOnly) callRecords.filter { it.direction == CallDirection.MISSED.name }
        else callRecords
    }
    val dateFmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.US) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !missedOnly,
                        onClick = { missedOnly = false },
                        label = { Text("همه تماس‌ها") },
                        modifier = Modifier.testTag("recents_filter_all")
                    )
                    FilterChip(
                        selected = missedOnly,
                        onClick = { missedOnly = true },
                        label = { Text("بی‌پاسخ") },
                        modifier = Modifier.testTag("recents_filter_missed")
                    )
                }
                FilledTonalButton(
                    onClick = onAddCallClick,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("recents_add_call_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ثبت تماس")
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.PhoneEnabled,
                            contentDescription = null,
                            tint = MintJade,
                            modifier = Modifier.size(40.dp)
                        )
                        Text("تماسی ثبت نشده است", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "تماس‌های گرفته‌شده از شماره‌گیر، ورودی‌های بلوتوثی یا فایل‌های پشتیبان اینجا نمایش داده می‌شوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { call ->
                val dir = CallDirection.entries.find { it.name == call.direction } ?: CallDirection.INCOMING
                val dirColor = when (dir) {
                    CallDirection.INCOMING -> EmeraldSuccess
                    CallDirection.OUTGOING -> MintJade
                    CallDirection.MISSED -> RoseError
                    CallDirection.REJECTED -> AmberWarning
                }
                val dirIcon = when (dir) {
                    CallDirection.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived
                    CallDirection.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade
                    CallDirection.MISSED, CallDirection.REJECTED -> Icons.AutoMirrored.Rounded.CallMissed
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBridgeCallback(call) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = dirIcon,
                            contentDescription = dir.labelFa,
                            tint = dirColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = call.contactName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dir == CallDirection.MISSED) RoseError else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = ChampagneGold.copy(alpha = 0.16f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "SIM ${call.simSlot}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ChampagneGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${call.phoneNumber} • ${dir.labelFa} • ${dateFmt.format(Date(call.timestampMillis))}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onSmsCallback(call) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Rounded.Sms, contentDescription = "پیامک", tint = ChampagneGold)
                        }
                        IconButton(onClick = { onSystemCallback(call) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Rounded.PhoneEnabled, contentDescription = "شماره‌گیر گوشی", tint = MintJade)
                        }
                        IconButton(onClick = { onSaveContact(call) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = "ذخیره مخاطب")
                        }
                        IconButton(onClick = { onDeleteCall(call.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactsPane(
    contacts: List<ContactEntity>,
    contactsPermissionGranted: Boolean,
    onAddContactClick: () -> Unit,
    onImportPhoneContacts: () -> Unit,
    onRequestContactsPermission: () -> Unit,
    onShareContactsBluetooth: (List<ContactEntity>) -> Unit,
    onBridgeCallContact: (ContactEntity) -> Unit,
    onSmsContact: (ContactEntity) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onSaveContactToPhone: (ContactEntity) -> Unit,
    onEditContact: (ContactEntity) -> Unit,
    onDeleteContact: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var search by remember { mutableStateOf("") }
    val query = search.trim().lowercase(Locale.ROOT)
    val filtered = remember(contacts, query) {
        contacts.filter {
            query.isEmpty() ||
                it.displayName.lowercase(Locale.ROOT).contains(query) ||
                it.phoneNumber.contains(query)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onAddContactClick,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("contacts_add_btn")
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("مخاطب جدید")
                }
                OutlinedButton(
                    onClick = {
                        if (contactsPermissionGranted) onImportPhoneContacts()
                        else onRequestContactsPermission()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("contacts_import_phone_btn")
                ) {
                    Icon(Icons.Rounded.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (contactsPermissionGranted) "همگام‌سازی دفترچه" else "تأیید دسترسی مخاطبین")
                }
                if (contacts.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onShareContactsBluetooth(filtered) },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("contacts_share_vcard_btn")
                    ) {
                        Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ارسال vCard")
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("جستجو در نام یا شماره...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("contacts_search_field")
            )
        }

        item {
            val syncedCount = contacts.count { it.syncedFromBridge }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MintJade.copy(alpha = 0.35f)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contacts_offline_cache_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = "ذخیره‌سازی آفلاین مخاطبین",
                        tint = MintJade,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "حافظه آفلاین مخاطبین (Room) • در دسترس هنگام قطع اتصال",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${contacts.size} مخاطب ذخیره‌شده ($syncedCount مورد همگام‌سازی‌شده از پل)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("مخاطبی یافت نشد", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "مخاطبین گوشی خود را همگام‌سازی کنید یا مخاطب جدید بسازید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { contact ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = contact.displayName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = contact.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = contact.phoneNumber,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (contact.syncedFromBridge) {
                                        Spacer(Modifier.height(3.dp))
                                        Surface(
                                            color = MintJade.copy(alpha = 0.14f),
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = if (contact.syncedDeviceName.isNotBlank()) {
                                                    "همگام‌سازی‌شده • ${contact.syncedDeviceName}"
                                                } else {
                                                    "همگام‌سازی‌شده"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MintJade,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            IconButton(
                                onClick = { onToggleFavorite(contact.id, !contact.isFavorite) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (contact.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = "برگزیده",
                                    tint = if (contact.isFavorite) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                IconButton(onClick = { onBridgeCallContact(contact) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Rounded.Call, contentDescription = "تماس", tint = EmeraldSuccess)
                                }
                                IconButton(onClick = { onSmsContact(contact) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Rounded.Sms, contentDescription = "پیامک", tint = MintJade)
                                }
                                IconButton(onClick = { onSaveContactToPhone(contact) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Rounded.SaveAlt, contentDescription = "ذخیره در گوشی", tint = ChampagneGold)
                                }
                            }
                            Row {
                                IconButton(onClick = { onEditContact(contact) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "ویرایش")
                                }
                                IconButton(onClick = { onDeleteContact(contact.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
