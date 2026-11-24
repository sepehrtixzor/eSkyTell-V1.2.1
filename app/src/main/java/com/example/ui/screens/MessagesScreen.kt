package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.SmsRecordEntity
import com.example.data.model.RelayConnectionState
import com.example.data.model.SmsDirection
import com.example.data.model.SmsThreadSummary
import com.example.ui.MainUiState
import com.example.ui.components.AddSmsDialog
import com.example.ui.components.ComposeSmsDialog
import com.example.ui.components.PasteTextDialog
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintJade
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessagesScreen(
    uiState: MainUiState,
    onOpenThread: (String) -> Unit,
    onCloseThread: () -> Unit,
    onDeleteThread: (String) -> Unit,
    onSelectSimSlot: (Int) -> Unit,
    onSendSms: (phone: String, name: String, body: String) -> Unit,
    onOpenSystemSms: (phone: String, body: String) -> Unit,
    onBridgeCall: (phone: String, name: String) -> Unit,
    onImportFileClick: () -> Unit,
    onAddManualSms: (sender: String, phone: String, body: String, direction: SmsDirection) -> Unit,
    onImportPastedText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPhone = uiState.selectedSmsThreadPhone
    if (selectedPhone != null) {
        BackHandler { onCloseThread() }
        val summary = uiState.smsThreads.firstOrNull { it.phoneNumber == selectedPhone }
        ConversationDetailPane(
            phoneNumber = selectedPhone,
            displayName = summary?.displayName ?: selectedPhone,
            messages = uiState.selectedThreadMessages,
            activeSimSlot = uiState.activeSimSlot,
            activeSimLabel = uiState.activeSimLabel,
            showDualSim = uiState.appConfig.enableDualSim,
            showQuickTemplates = uiState.appConfig.enableQuickTemplates,
            onToggleSimSlot = { onSelectSimSlot(if (uiState.activeSimSlot == 1) 2 else 1) },
            onBack = onCloseThread,
            onCall = { onBridgeCall(selectedPhone, summary?.displayName ?: selectedPhone) },
            onOpenSystemSms = { body -> onOpenSystemSms(selectedPhone, body) },
            onSend = { body -> onSendSms(selectedPhone, summary?.displayName ?: selectedPhone, body) },
            modifier = modifier
        )
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var showComposeDialog by remember { mutableStateOf(false) }
    var showAddManualDialog by remember { mutableStateOf(false) }
    var showPasteDialog by remember { mutableStateOf(false) }

    val query = searchQuery.trim().lowercase(Locale.ROOT)
    val filteredThreads = remember(uiState.smsThreads, query) {
        uiState.smsThreads.filter {
            query.isEmpty() ||
                it.displayName.lowercase(Locale.ROOT).contains(query) ||
                it.phoneNumber.contains(query) ||
                it.lastMessage.lowercase(Locale.ROOT).contains(query)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "پیامک‌ها",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.smsThreads.size} گفتگو • ${uiState.activeSimLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showComposeDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("messages_compose_btn")
                    ) {
                        Icon(Icons.Rounded.AddComment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("پیام جدید")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("جستجو در گفتگوها و متن پیامک...") },
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("messages_search_input")
                )
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onImportFileClick,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("messages_import_file_btn")
                    ) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ورود فایل (XML/CSV/JSON)")
                    }
                    OutlinedButton(
                        onClick = { showPasteDialog = true },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("messages_paste_btn")
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("چسباندن متن")
                    }
                    OutlinedButton(
                        onClick = { showAddManualDialog = true },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("messages_add_manual_btn")
                    ) {
                        Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ثبت پیامک دریافتی")
                    }
                }
            }

            item {
                val isBridgeOnline = uiState.relayConnectionState is RelayConnectionState.Connected ||
                    uiState.relayConnectionState is RelayConnectionState.Relaying
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MintJade.copy(alpha = 0.35f)),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("messages_offline_cache_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storage,
                            contentDescription = "حافظه آفلاین Room",
                            tint = MintJade,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBridgeOnline) {
                                    "کش آفلاین Room فعال • همگام‌سازی زنده متصل"
                                } else {
                                    "حالت آفلاین • در دسترس بدون اتصال بلوتوث"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.smsRecords.size} پیامک در حافظه محلی (${uiState.cachedBridgeSmsCount} همگام‌سازی‌شده)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (filteredThreads.isEmpty()) {
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sms,
                                contentDescription = null,
                                tint = MintJade,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "صندوق پیامک خالی است",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "برای شروع یک «پیام جدید» بنویسید، فایل پشتیبان پیامک‌ها را وارد کنید یا پیام‌ها را از پل بلوتوث دریافت نمایید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredThreads, key = { it.phoneNumber }) { thread ->
                    SmsThreadRowCard(
                        thread = thread,
                        onClick = { onOpenThread(thread.phoneNumber) },
                        onDelete = { onDeleteThread(thread.phoneNumber) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showComposeDialog) {
        ComposeSmsDialog(
            contacts = uiState.contacts,
            activeSimLabel = uiState.activeSimLabel,
            onDismiss = { showComposeDialog = false },
            onSend = onSendSms
        )
    }

    if (showAddManualDialog) {
        AddSmsDialog(
            onDismiss = { showAddManualDialog = false },
            onConfirm = onAddManualSms
        )
    }

    if (showPasteDialog) {
        PasteTextDialog(
            onDismiss = { showPasteDialog = false },
            onConfirm = onImportPastedText
        )
    }
}

@Composable
private fun SmsThreadRowCard(
    thread: SmsThreadSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.US) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (thread.unreadCount > 0) MintJade.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("sms_thread_${thread.phoneNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = thread.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (thread.unreadCount > 0) {
                    Surface(
                        color = MintJade,
                        shape = CircleShape,
                        modifier = Modifier.size(12.dp)
                    ) {}
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = timeFmt.format(Date(thread.lastTimestampMillis)),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = thread.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = ChampagneGold.copy(alpha = 0.16f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "SIM ${thread.simSlot}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampagneGold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = "${thread.messageCount} پیام",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (thread.unreadCount > 0) {
                        Surface(
                            color = MintJade.copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${thread.unreadCount} جدید",
                                style = MaterialTheme.typography.labelSmall,
                                color = MintJade,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "حذف گفتگو",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConversationDetailPane(
    phoneNumber: String,
    displayName: String,
    messages: List<SmsRecordEntity>,
    activeSimSlot: Int,
    activeSimLabel: String,
    showDualSim: Boolean,
    showQuickTemplates: Boolean,
    onToggleSimSlot: () -> Unit,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onOpenSystemSms: (String) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember { mutableStateOf("") }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.US) }
    val quickTemplates = remember {
        listOf(
            "الان تماس می‌گیرم",
            "لطفاً پیام بدید",
            "در جلسه هستم",
            "تأیید شد، ممنون"
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            // Top iOS-style conversation header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("thread_back_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت")
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = phoneNumber,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = onCall,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("thread_call_btn")
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = "تماس با مخاطب", tint = MintJade)
                        }
                        IconButton(
                            onClick = { onOpenSystemSms(draft) },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("thread_system_sms_btn")
                        ) {
                            Icon(Icons.Rounded.Sms, contentDescription = "باز کردن در پیام‌رسان گوشی", tint = ChampagneGold)
                        }
                    }
                }
            }

            // Bubbles
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isOutgoing = msg.direction == SmsDirection.OUTGOING.name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isOutgoing) Arrangement.Start else Arrangement.End
                    ) {
                        Surface(
                            color = if (isOutgoing) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isOutgoing) 4.dp else 18.dp,
                                bottomEnd = if (isOutgoing) 18.dp else 4.dp
                            ),
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = msg.body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${timeFmt.format(Date(msg.timestampMillis))} • SIM ${msg.simSlot}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Quick Templates Strip
            if (showQuickTemplates) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickTemplates) { tmpl ->
                        AssistChip(
                            onClick = { draft = tmpl },
                            label = { Text(tmpl, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Bottom Composer Pill
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (showDualSim) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clickable { onToggleSimSlot() }
                                .padding(bottom = 6.dp)
                                .testTag("composer_sim_toggle")
                        ) {
                            Icon(Icons.Rounded.SimCard, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(14.dp))
                            Text(
                                text = "ارسال از: $activeSimLabel (برای تعویض لمس کنید)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampagneGold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            placeholder = { Text("پیام خود را بنویسید...") },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("thread_message_input")
                        )

                        FilledIconButton(
                            onClick = {
                                val text = draft.trim()
                                if (text.isNotEmpty()) {
                                    onSend(text)
                                    draft = ""
                                }
                            },
                            enabled = draft.isNotBlank(),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MintJade,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("thread_send_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "ارسال پیامک")
                        }
                    }
                }
            }
        }
    }
}
