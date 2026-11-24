package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.AnnouncementCategory
import com.example.data.model.AnnouncementModule
import com.example.data.model.PaymentRequestStatus
import com.example.data.model.RelayConnectionState
import com.example.data.model.SubscriptionTier
import com.example.data.model.formatTomanAmount
import com.example.ui.MainUiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MintJade
import com.example.ui.theme.RoseError

@Composable
fun MoreAndAccountScreen(
    uiState: MainUiState,
    onOpenAdminStudio: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenPermissionsSheet: () -> Unit,
    onOpenHistorySheet: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleAutoReconnect: (Boolean) -> Unit,
    onToggleRenewalNotifications: (Boolean) -> Unit,
    onSendTestRenewalNotification: () -> Unit,
    onOpenPasswordRecovery: () -> Unit,
    onOpenLegalDocument: (LegalDocumentType) -> Unit,
    onSaveSimLabels: (sim1: String, sim2: String) -> Unit,
    onOpenExternalAction: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = uiState.appConfig
    val profile = uiState.userProfile
    val hasUpdate = config.latestVersionCode > BuildConfig.VERSION_CODE || config.updateNotes.isNotBlank()

    var sim1Draft by remember(profile?.sim1Label) { mutableStateOf(uiState.sim1DisplayLabel) }
    var sim2Draft by remember(profile?.sim2Label) { mutableStateOf(uiState.sim2DisplayLabel) }
    LaunchedEffect(profile?.sim1Label, profile?.sim2Label) {
        if (profile != null) {
            sim1Draft = profile.sim1Label
            sim2Draft = profile.sim2Label
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. User Account Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (profile?.displayName ?: uiState.currentUserEmail).take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MintJade
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = profile?.displayName?.ifBlank { "کاربر eSkyTell" } ?: "کاربر eSkyTell",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (uiState.isAdmin) {
                                        Surface(color = ChampagneGold.copy(alpha = 0.2f), shape = CircleShape) {
                                            Text(
                                                text = "مدیر کل",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ChampagneGold,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = uiState.currentUserEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Easy No-Code Admin Studio Card (Only for Admin)
            if (uiState.isAdmin) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ChampagneGold.copy(alpha = 0.14f)),
                        border = BorderStroke(1.5.dp, ChampagneGold.copy(alpha = 0.7f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "استودیوی مدیریت بدون کد (Admin Studio)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "مدیریت کارت‌ها و کیف پول، تعرفه تومان، تایید فیش‌های واریزی و انتشار آپدیت",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (uiState.pendingPaymentRequestsCount > 0) {
                                Surface(
                                    color = AmberWarning.copy(alpha = 0.2f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${uiState.pendingPaymentRequestsCount} فیش واریزی در انتظار تایید",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AmberWarning,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Button(
                                onClick = onOpenAdminStudio,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChampagneGold,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("open_admin_studio_btn")
                            ) {
                                Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("ورود به پنل مدیریت eSkyTell", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Live App Update Banner (Published by Admin)
            if (hasUpdate) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MintJade.copy(alpha = 0.14f)),
                        border = BorderStroke(1.dp, MintJade.copy(alpha = 0.65f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = MintJade)
                                Text(
                                    text = "نسخه ${config.latestVersionName} ${if (config.forceUpdate) "(آپدیت مهم)" else "در دسترس است"}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (config.updateNotes.isNotBlank()) {
                                Text(
                                    text = config.updateNotes,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (config.updateUrl.isNotBlank()) {
                                FilledTonalButton(
                                    onClick = { onOpenExternalAction(config.updateUrl) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("دریافت نسخه جدید")
                                }
                            }
                        }
                    }
                }
            }

            // 4. Subscription & Pricing Status Card
            item {
                val tier = SubscriptionTier.fromRaw(profile?.subscriptionTier ?: SubscriptionTier.FREE.name)
                val isFreeMode = !config.subscriptionRequired
                val isPro = profile?.hasActiveSubscription == true || uiState.isAdmin

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isFreeMode || isPro) Icons.Rounded.Verified else Icons.Rounded.WorkspacePremium,
                                    contentDescription = null,
                                    tint = if (isFreeMode || isPro) EmeraldSuccess else ChampagneGold
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "وضعیت اشتراک eSkyTell",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when {
                                            isFreeMode -> "حالت فعلی: کاملاً رایگان برای همه کاربران"
                                            isPro -> "اشتراک فعال: ${tier.labelFa}"
                                            else -> "نیاز به فعال‌سازی اشتراک Pro"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isFreeMode || isPro) EmeraldSuccess else ChampagneGold
                                    )
                                }
                            }
                        }

                        val daysLeft = uiState.subscriptionDaysRemaining
                        if (daysLeft != null) {
                            Text(
                                text = if (daysLeft == 0) "مهلت اشتراک به پایان رسیده است" else "$daysLeft روز تا زمان تمدید اشتراک باقی مانده",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (daysLeft <= 5) RoseError else MintJade,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val latestRequest = uiState.userPaymentRequests.firstOrNull()
                        if (latestRequest != null) {
                            val reqStatus = PaymentRequestStatus.fromRaw(latestRequest.status)
                            val reqColor = when (reqStatus) {
                                PaymentRequestStatus.PENDING -> AmberWarning
                                PaymentRequestStatus.APPROVED -> EmeraldSuccess
                                PaymentRequestStatus.REJECTED -> RoseError
                            }
                            Surface(
                                color = reqColor.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, reqColor.copy(alpha = 0.4f)),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "وضعیت آخرین فیش: ${reqStatus.labelFa}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = reqColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (latestRequest.adminNote.isNotBlank()) {
                                        Text(
                                            text = "پیام مدیر: ${latestRequest.adminNote}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("نوتیفیکیشن یادآور تمدید اشتراک", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("اطلاع‌رسانی پیش از اتمام مهلت اشتراک", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = uiState.renewalNotificationsEnabled,
                                onCheckedChange = onToggleRenewalNotifications,
                                modifier = Modifier.testTag("settings_renewal_notif_switch")
                            )
                        }

                        FilledTonalButton(
                            onClick = onSendTestRenewalNotification,
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("test_renewal_notification_btn")
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ارسال تست نوتیفیکیشن تمدید")
                        }

                        val monthlyDisplay = if (config.monthlyPriceToman > 0L) {
                            formatTomanAmount(config.monthlyPriceToman)
                        } else {
                            config.monthlyPriceLabel
                        }
                        if (!isFreeMode && !isPro) {
                            Button(
                                onClick = onOpenPaywall,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("more_upgrade_btn")
                            ) {
                                Text("خرید اشتراک و ارسال فیش ($monthlyDisplay)")
                            }
                        } else if (!isPro) {
                            OutlinedButton(
                                onClick = onOpenPaywall,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("more_upgrade_btn")
                            ) {
                                Text("مشاهده پلن‌ها و ارسال فیش واریزی")
                            }
                        }
                    }
                }
            }

            // 5. Dynamic Published Modules / New Features / Tips from Admin
            if (uiState.publishedAnnouncements.isNotEmpty()) {
                item {
                    Text(
                        text = "امکانات و اطلاعیه‌های جدید",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(uiState.publishedAnnouncements, key = { it.id }) { mod ->
                    PublishedModuleCard(
                        module = mod,
                        onActionClick = { onOpenExternalAction(mod.actionValue) }
                    )
                }
            }

            // 6. Dual-SIM Custom Labels
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SimCard, contentDescription = null, tint = ChampagneGold)
                            Spacer(Modifier.width(8.dp))
                            Text("نام‌گذاری خطوط دو سیم‌کارت", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(
                            value = sim1Draft,
                            onValueChange = { sim1Draft = it },
                            label = { Text("برچسب سیم‌کارت ۱") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("settings_sim1_input")
                        )
                        OutlinedTextField(
                            value = sim2Draft,
                            onValueChange = { sim2Draft = it },
                            label = { Text("برچسب سیم‌کارت ۲") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("settings_sim2_input")
                        )
                        FilledTonalButton(
                            onClick = { onSaveSimLabels(sim1Draft, sim2Draft) },
                            enabled = !uiState.isCloudSaving,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_save_sims_btn")
                        ) {
                            Text("ذخیره نام سیم‌کارت‌ها")
                        }
                    }
                }
            }

            // 7. Theme, Permissions, History & Sign Out
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.large
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                    contentDescription = null,
                                    tint = MintJade
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("تم تاریک Obsidian Jade", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("تغییر ظاهر بین حالت شب و روز", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = uiState.isDarkTheme,
                                onCheckedChange = { onToggleTheme() },
                                modifier = Modifier.testTag("settings_theme_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = null,
                                    tint = MintJade
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("اتصال مجدد خودکار بلوتوث", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    val statusLabel = when (val st = uiState.relayConnectionState) {
                                        is RelayConnectionState.Connected -> "وضعیت فعلی: متصل به ${st.deviceName}"
                                        is RelayConnectionState.Reconnecting -> "وضعیت فعلی: تلاش مجدد (${st.attempt}/${st.maxAttempts})"
                                        is RelayConnectionState.Connecting -> "وضعیت فعلی: در حال اتصال..."
                                        is RelayConnectionState.Listening -> "وضعیت فعلی: آماده دریافت"
                                        is RelayConnectionState.Relaying -> "وضعیت فعلی: در حال رله"
                                        is RelayConnectionState.Error -> "وضعیت فعلی: خطا در اتصال"
                                        RelayConnectionState.Disconnected -> "بازیابی خودکار لینک هنگام قطع سیگنال"
                                    }
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when (uiState.relayConnectionState) {
                                            is RelayConnectionState.Connected -> EmeraldSuccess
                                            is RelayConnectionState.Reconnecting -> AmberWarning
                                            is RelayConnectionState.Error -> RoseError
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.testTag("settings_connection_status_badge")
                                    )
                                }
                            }
                            Switch(
                                checked = uiState.autoReconnectEnabled,
                                onCheckedChange = onToggleAutoReconnect,
                                modifier = Modifier.testTag("settings_auto_reconnect_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenPermissionsSheet,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_permissions_btn")
                            ) {
                                Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("مجوزها")
                            }
                            OutlinedButton(
                                onClick = onOpenHistorySheet,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_history_btn")
                            ) {
                                Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تاریخچه پل")
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenPasswordRecovery,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_password_recovery_btn")
                        ) {
                            Icon(Icons.Rounded.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("بازیابی رمز عبور و پین امنیتی حساب")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onOpenLegalDocument(LegalDocumentType.TERMS) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_terms_btn")
                            ) {
                                Icon(Icons.Rounded.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("قوانین و مقررات")
                            }
                            OutlinedButton(
                                onClick = { onOpenLegalDocument(LegalDocumentType.PRIVACY) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_privacy_btn")
                            ) {
                                Icon(Icons.Rounded.PrivacyTip, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("حریم خصوصی")
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            border = BorderStroke(1.dp, RoseError.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, tint = RoseError, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("خروج از حساب کاربری", color = RoseError)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PublishedModuleCard(
    module: AnnouncementModule,
    onActionClick: () -> Unit
) {
    val cat = AnnouncementCategory.fromRaw(module.category)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MintJade.copy(alpha = 0.35f)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                Text(module.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                    Text(
                        text = cat.labelFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Text(module.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (module.actionValue.isNotBlank()) {
                FilledTonalButton(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text(module.actionLabel.ifBlank { "اجرا / مشاهده" })
                }
            }
        }
    }
}
