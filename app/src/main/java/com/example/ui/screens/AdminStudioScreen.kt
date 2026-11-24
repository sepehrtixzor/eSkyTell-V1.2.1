package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AnnouncementCategory
import com.example.data.model.AnnouncementModule
import com.example.data.model.AppGlobalConfig
import com.example.data.model.BankCardInfo
import com.example.data.model.CryptoWalletInfo
import com.example.data.model.PaymentRequest
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserProfile
import com.example.data.model.formatTomanAmount
import com.example.ui.MainUiState
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MintJade
import com.example.ui.theme.RoseError
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStudioScreen(
    uiState: MainUiState,
    onSaveGlobalConfig: (AppGlobalConfig) -> Unit,
    onSaveAnnouncement: (
        existingId: String?,
        title: String,
        summary: String,
        category: AnnouncementCategory,
        actionLabel: String,
        actionValue: String,
        published: Boolean
    ) -> Unit,
    onToggleAnnouncementPublished: (id: String, published: Boolean) -> Unit,
    onDeleteAnnouncement: (id: String) -> Unit,
    onSetUserSubscription: (targetUserId: String, hasActive: Boolean, tier: SubscriptionTier) -> Unit,
    onReviewPaymentRequest: (request: PaymentRequest, approve: Boolean, adminNote: String) -> Unit,
    onSendRenewalNotification: (targetUserId: String, message: String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingModule by remember { mutableStateOf<AnnouncementModule?>(null) }
    var showNewModuleDialog by remember { mutableStateOf(false) }

    var showNewCardDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<Pair<Int, BankCardInfo>?>(null) }
    var showNewWalletDialog by remember { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<Pair<Int, CryptoWalletInfo>?>(null) }

    val config = uiState.appConfig
    var subRequired by remember(config.subscriptionRequired) { mutableStateOf(config.subscriptionRequired) }
    var monthlyPriceTomanText by remember(config.monthlyPriceToman) { mutableStateOf(config.monthlyPriceToman.toString()) }
    var yearlyPriceTomanText by remember(config.yearlyPriceToman) { mutableStateOf(config.yearlyPriceToman.toString()) }
    var monthlyPrice by remember(config.monthlyPriceLabel) { mutableStateOf(config.monthlyPriceLabel) }
    var yearlyPrice by remember(config.yearlyPriceLabel) { mutableStateOf(config.yearlyPriceLabel) }
    var trialDaysText by remember(config.freeTrialDays) { mutableStateOf(config.freeTrialDays.toString()) }
    var paywallMessage by remember(config.paywallMessage) { mutableStateOf(config.paywallMessage) }
    var paymentUrl by remember(config.paymentUrl) { mutableStateOf(config.paymentUrl) }
    var bankCards by remember(config.bankCards) { mutableStateOf(config.bankCards) }
    var cryptoWallets by remember(config.cryptoWallets) { mutableStateOf(config.cryptoWallets) }

    var versionName by remember(config.latestVersionName) { mutableStateOf(config.latestVersionName) }
    var versionCodeText by remember(config.latestVersionCode) { mutableStateOf(config.latestVersionCode.toString()) }
    var updateNotes by remember(config.updateNotes) { mutableStateOf(config.updateNotes) }
    var updateUrl by remember(config.updateUrl) { mutableStateOf(config.updateUrl) }
    var forceUpdate by remember(config.forceUpdate) { mutableStateOf(config.forceUpdate) }

    var enableDualSim by remember(config.enableDualSim) { mutableStateOf(config.enableDualSim) }
    var enableQuickTemplates by remember(config.enableQuickTemplates) { mutableStateOf(config.enableQuickTemplates) }
    var enableQuietHours by remember(config.enableQuietHours) { mutableStateOf(config.enableQuietHours) }
    var enableBleGatt by remember(config.enableBleGattBridge) { mutableStateOf(config.enableBleGattBridge) }

    LaunchedEffect(config) {
        subRequired = config.subscriptionRequired
        monthlyPriceTomanText = config.monthlyPriceToman.toString()
        yearlyPriceTomanText = config.yearlyPriceToman.toString()
        monthlyPrice = config.monthlyPriceLabel
        yearlyPrice = config.yearlyPriceLabel
        trialDaysText = config.freeTrialDays.toString()
        paywallMessage = config.paywallMessage
        paymentUrl = config.paymentUrl
        bankCards = config.bankCards
        cryptoWallets = config.cryptoWallets
        versionName = config.latestVersionName
        versionCodeText = config.latestVersionCode.toString()
        updateNotes = config.updateNotes
        updateUrl = config.updateUrl
        forceUpdate = config.forceUpdate
        enableDualSim = config.enableDualSim
        enableQuickTemplates = config.enableQuickTemplates
        enableQuietHours = config.enableQuietHours
        enableBleGatt = config.enableBleGattBridge
    }

    fun emitUpdatedConfig(
        cardsOverride: List<BankCardInfo> = bankCards,
        walletsOverride: List<CryptoWalletInfo> = cryptoWallets
    ) {
        val monthlyToman = (monthlyPriceTomanText.toLongOrNull() ?: config.monthlyPriceToman).coerceAtLeast(0L)
        val yearlyToman = (yearlyPriceTomanText.toLongOrNull() ?: config.yearlyPriceToman).coerceAtLeast(0L)
        onSaveGlobalConfig(
            config.copy(
                subscriptionRequired = subRequired,
                monthlyPriceToman = monthlyToman,
                yearlyPriceToman = yearlyToman,
                monthlyPriceLabel = monthlyPrice.ifBlank { formatTomanAmount(monthlyToman, "/ ماه") },
                yearlyPriceLabel = yearlyPrice.ifBlank { formatTomanAmount(yearlyToman, "/ سال") },
                freeTrialDays = trialDaysText.toLongOrNull() ?: 0L,
                paywallMessage = paywallMessage,
                paymentUrl = paymentUrl,
                bankCards = cardsOverride,
                cryptoWallets = walletsOverride,
                latestVersionName = versionName,
                latestVersionCode = (versionCodeText.toLongOrNull() ?: 1L).coerceAtLeast(1L),
                updateNotes = updateNotes,
                updateUrl = updateUrl,
                forceUpdate = forceUpdate,
                enableDualSim = enableDualSim,
                enableQuickTemplates = enableQuickTemplates,
                enableQuietHours = enableQuietHours,
                enableBleGattBridge = enableBleGatt
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("پنل مدیریت eSkyTell", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("مدیریت پرداخت، اشتراک و کاربران", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_back_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
                ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("اشتراک و پرداخت") },
                        icon = { Icon(Icons.Rounded.WorkspacePremium, contentDescription = null) },
                        modifier = Modifier.testTag("admin_tab_monetization")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("فیش‌های واریزی") },
                        icon = {
                            if (uiState.pendingPaymentRequestsCount > 0) {
                                BadgedBox(badge = { Badge { Text(uiState.pendingPaymentRequestsCount.toString()) } }) {
                                    Icon(Icons.Rounded.ReceiptLong, contentDescription = null)
                                }
                            } else {
                                Icon(Icons.Rounded.ReceiptLong, contentDescription = null)
                            }
                        },
                        modifier = Modifier.testTag("admin_tab_payment_requests")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("آپدیت و امکانات") },
                        icon = { Icon(Icons.Rounded.SettingsSuggest, contentDescription = null) },
                        modifier = Modifier.testTag("admin_tab_features")
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("کاربران (${uiState.adminUsers.size})") },
                        icon = { Icon(Icons.Rounded.People, contentDescription = null) },
                        modifier = Modifier.testTag("admin_tab_users")
                    )
                }

                when (selectedTab) {
                    0 -> MonetizationAdminTab(
                        subRequired = subRequired,
                        onSubRequiredChange = { subRequired = it },
                        monthlyPriceTomanText = monthlyPriceTomanText,
                        onMonthlyPriceTomanChange = { raw ->
                            val digits = raw.filter { ch -> ch.isDigit() }
                            monthlyPriceTomanText = digits
                            digits.toLongOrNull()?.let { monthlyPrice = formatTomanAmount(it, "/ ماه") }
                        },
                        yearlyPriceTomanText = yearlyPriceTomanText,
                        onYearlyPriceTomanChange = { raw ->
                            val digits = raw.filter { ch -> ch.isDigit() }
                            yearlyPriceTomanText = digits
                            digits.toLongOrNull()?.let { yearlyPrice = formatTomanAmount(it, "/ سال") }
                        },
                        trialDaysText = trialDaysText,
                        onTrialDaysChange = { trialDaysText = it.filter { ch -> ch.isDigit() } },
                        paywallMessage = paywallMessage,
                        onPaywallMessageChange = { paywallMessage = it },
                        paymentUrl = paymentUrl,
                        onPaymentUrlChange = { paymentUrl = it },
                        bankCards = bankCards,
                        onAddCardClick = { showNewCardDialog = true },
                        onEditCardClick = { idx, card -> editingCard = idx to card },
                        onDeleteCardClick = { idx ->
                            val updated = bankCards.toMutableList().apply { removeAt(idx) }
                            bankCards = updated
                            emitUpdatedConfig(cardsOverride = updated)
                        },
                        cryptoWallets = cryptoWallets,
                        onAddWalletClick = { showNewWalletDialog = true },
                        onEditWalletClick = { idx, wallet -> editingWallet = idx to wallet },
                        onDeleteWalletClick = { idx ->
                            val updated = cryptoWallets.toMutableList().apply { removeAt(idx) }
                            cryptoWallets = updated
                            emitUpdatedConfig(walletsOverride = updated)
                        },
                        isSaving = uiState.isCloudSaving,
                        onSave = { emitUpdatedConfig() }
                    )

                    1 -> PaymentRequestsAdminTab(
                        requests = uiState.adminPaymentRequests,
                        isSaving = uiState.isCloudSaving,
                        onReviewRequest = onReviewPaymentRequest
                    )

                    2 -> ReleasesAndModulesAdminTab(
                        versionName = versionName,
                        onVersionNameChange = { versionName = it },
                        versionCodeText = versionCodeText,
                        onVersionCodeChange = { versionCodeText = it.filter { ch -> ch.isDigit() } },
                        updateNotes = updateNotes,
                        onUpdateNotesChange = { updateNotes = it },
                        updateUrl = updateUrl,
                        onUpdateUrlChange = { updateUrl = it },
                        forceUpdate = forceUpdate,
                        onForceUpdateChange = { forceUpdate = it },
                        enableDualSim = enableDualSim,
                        onDualSimChange = { enableDualSim = it },
                        enableQuickTemplates = enableQuickTemplates,
                        onQuickTemplatesChange = { enableQuickTemplates = it },
                        enableQuietHours = enableQuietHours,
                        onQuietHoursChange = { enableQuietHours = it },
                        enableBleGatt = enableBleGatt,
                        onBleGattChange = { enableBleGatt = it },
                        isSaving = uiState.isCloudSaving,
                        modules = uiState.adminAnnouncements,
                        onSaveConfig = { emitUpdatedConfig() },
                        onNewModuleClick = { showNewModuleDialog = true },
                        onEditModule = { editingModule = it },
                        onToggleModulePublished = onToggleAnnouncementPublished,
                        onDeleteModule = onDeleteAnnouncement
                    )

                    else -> UsersAdminTab(
                        users = uiState.adminUsers,
                        onSetSubscription = onSetUserSubscription,
                        onSendRenewalNotification = onSendRenewalNotification
                    )
                }
            }
        }
    }

    if (showNewCardDialog) {
        BankCardEditorDialog(
            initial = null,
            onDismiss = { showNewCardDialog = false },
            onSave = { card ->
                val updated = bankCards + card
                bankCards = updated
                emitUpdatedConfig(cardsOverride = updated)
            }
        )
    }

    editingCard?.let { (idx, card) ->
        BankCardEditorDialog(
            initial = card,
            onDismiss = { editingCard = null },
            onSave = { edited ->
                val updated = bankCards.toMutableList().apply { set(idx, edited) }
                bankCards = updated
                emitUpdatedConfig(cardsOverride = updated)
            }
        )
    }

    if (showNewWalletDialog) {
        CryptoWalletEditorDialog(
            initial = null,
            onDismiss = { showNewWalletDialog = false },
            onSave = { wallet ->
                val updated = cryptoWallets + wallet
                cryptoWallets = updated
                emitUpdatedConfig(walletsOverride = updated)
            }
        )
    }

    editingWallet?.let { (idx, wallet) ->
        CryptoWalletEditorDialog(
            initial = wallet,
            onDismiss = { editingWallet = null },
            onSave = { edited ->
                val updated = cryptoWallets.toMutableList().apply { set(idx, edited) }
                cryptoWallets = updated
                emitUpdatedConfig(walletsOverride = updated)
            }
        )
    }

    if (showNewModuleDialog) {
        ModuleEditorDialog(
            initial = null,
            onDismiss = { showNewModuleDialog = false },
            onSave = { title, summary, category, actionLabel, actionValue, published ->
                onSaveAnnouncement(null, title, summary, category, actionLabel, actionValue, published)
            }
        )
    }

    editingModule?.let { mod ->
        ModuleEditorDialog(
            initial = mod,
            onDismiss = { editingModule = null },
            onSave = { title, summary, category, actionLabel, actionValue, published ->
                onSaveAnnouncement(mod.id, title, summary, category, actionLabel, actionValue, published)
            }
        )
    }
}

@Composable
private fun MonetizationAdminTab(
    subRequired: Boolean,
    onSubRequiredChange: (Boolean) -> Unit,
    monthlyPriceTomanText: String,
    onMonthlyPriceTomanChange: (String) -> Unit,
    yearlyPriceTomanText: String,
    onYearlyPriceTomanChange: (String) -> Unit,
    trialDaysText: String,
    onTrialDaysChange: (String) -> Unit,
    paywallMessage: String,
    onPaywallMessageChange: (String) -> Unit,
    paymentUrl: String,
    onPaymentUrlChange: (String) -> Unit,
    bankCards: List<BankCardInfo>,
    onAddCardClick: () -> Unit,
    onEditCardClick: (Int, BankCardInfo) -> Unit,
    onDeleteCardClick: (Int) -> Unit,
    cryptoWallets: List<CryptoWalletInfo>,
    onAddWalletClick: () -> Unit,
    onEditWalletClick: (Int, CryptoWalletInfo) -> Unit,
    onDeleteWalletClick: (Int) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (subRequired) ChampagneGold.copy(alpha = 0.14f) else EmeraldSuccess.copy(alpha = 0.14f)
                ),
                border = BorderStroke(1.dp, if (subRequired) ChampagneGold else EmeraldSuccess),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (subRequired) "حالت اشتراکی فعال است" else "حالت فعلی: کاملاً رایگان",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "هر زمان که بخواهید با تغییر این کلید می‌توانید کل برنامه را بین رایگان و اشتراکی جابه‌جا کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = subRequired,
                        onCheckedChange = onSubRequiredChange,
                        modifier = Modifier.testTag("admin_subscription_switch")
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("مبالغ اشتراک به تومان و تنظیمات صفحه خرید", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = monthlyPriceTomanText,
                        onValueChange = onMonthlyPriceTomanChange,
                        label = { Text("مبلغ اشتراک ماهانه (تومان)") },
                        supportingText = {
                            monthlyPriceTomanText.toLongOrNull()?.let {
                                Text("نمایش برای کاربر: ${formatTomanAmount(it, "/ ماه")}", color = ChampagneGold)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_monthly_price_toman")
                    )
                    OutlinedTextField(
                        value = yearlyPriceTomanText,
                        onValueChange = onYearlyPriceTomanChange,
                        label = { Text("مبلغ اشتراک سالانه (تومان)") },
                        supportingText = {
                            yearlyPriceTomanText.toLongOrNull()?.let {
                                Text("نمایش برای کاربر: ${formatTomanAmount(it, "/ سال")}", color = ChampagneGold)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_yearly_price_toman")
                    )
                    OutlinedTextField(
                        value = trialDaysText,
                        onValueChange = onTrialDaysChange,
                        label = { Text("تعداد روز تست رایگان") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paywallMessage,
                        onValueChange = onPaywallMessageChange,
                        label = { Text("پیام دعوت به خرید اشتراک") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paymentUrl,
                        onValueChange = onPaymentUrlChange,
                        label = { Text("لینک درگاه پرداخت یا صفحه پشتیبانی (اختیاری)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = onSave,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_save_monetization_btn")
                    ) {
                        Icon(Icons.Rounded.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("انتشار تغییرات اشتراک و مبالغ برای همه کاربران")
                    }
                }
            }
        }

        item {
            BankCardsManagerCard(
                cards = bankCards,
                onAddClick = onAddCardClick,
                onEditClick = onEditCardClick,
                onDeleteClick = onDeleteCardClick
            )
        }

        item {
            CryptoWalletsManagerCard(
                wallets = cryptoWallets,
                onAddClick = onAddWalletClick,
                onEditClick = onEditWalletClick,
                onDeleteClick = onDeleteWalletClick
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ReleasesAndModulesAdminTab(
    versionName: String,
    onVersionNameChange: (String) -> Unit,
    versionCodeText: String,
    onVersionCodeChange: (String) -> Unit,
    updateNotes: String,
    onUpdateNotesChange: (String) -> Unit,
    updateUrl: String,
    onUpdateUrlChange: (String) -> Unit,
    forceUpdate: Boolean,
    onForceUpdateChange: (Boolean) -> Unit,
    enableDualSim: Boolean,
    onDualSimChange: (Boolean) -> Unit,
    enableQuickTemplates: Boolean,
    onQuickTemplatesChange: (Boolean) -> Unit,
    enableQuietHours: Boolean,
    onQuietHoursChange: (Boolean) -> Unit,
    enableBleGatt: Boolean,
    onBleGattChange: (Boolean) -> Unit,
    isSaving: Boolean,
    modules: List<AnnouncementModule>,
    onSaveConfig: () -> Unit,
    onNewModuleClick: () -> Unit,
    onEditModule: (AnnouncementModule) -> Unit,
    onToggleModulePublished: (String, Boolean) -> Unit,
    onDeleteModule: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("انتشار آپدیت جدید برای عموم", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = versionName,
                            onValueChange = onVersionNameChange,
                            label = { Text("نام نسخه (مثلاً 1.2.0)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = versionCodeText,
                            onValueChange = onVersionCodeChange,
                            label = { Text("کد نسخه") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = updateNotes,
                        onValueChange = onUpdateNotesChange,
                        label = { Text("توضیحات تغییرات نسخه جدید") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = updateUrl,
                        onValueChange = onUpdateUrlChange,
                        label = { Text("لینک دانلود یا استور نسخه جدید (https://...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("نشان‌گذاری به عنوان آپدیت مهم", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = forceUpdate, onCheckedChange = onForceUpdateChange)
                    }

                    Text("کلیدهای روشن/خاموش امکانات داخلی", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    FeatureToggleRow("پشتیبانی دو سیم‌کارت (SIM 1 / SIM 2)", enableDualSim, onDualSimChange)
                    FeatureToggleRow("قالب‌های پاسخ سریع پیامک", enableQuickTemplates, onQuickTemplatesChange)
                    FeatureToggleRow("حالت سکوت پل (Quiet Hours)", enableQuietHours, onQuietHoursChange)
                    FeatureToggleRow("پل کم‌مصرف بلوتوث BLE GATT", enableBleGatt, onBleGattChange)

                    Button(
                        onClick = onSaveConfig,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_save_release_btn")
                    ) {
                        Icon(Icons.Rounded.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ذخیره و انتشار آپدیت و امکانات")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ماژول‌ها و اطلاعیه‌های پویا (${modules.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FilledTonalButton(
                    onClick = onNewModuleClick,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_add_module_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("افزودن مورد جدید")
                }
            }
        }

        items(modules, key = { it.id }) { mod ->
            val cat = AnnouncementCategory.fromRaw(mod.category)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mod.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(cat.labelFa, style = MaterialTheme.typography.labelSmall, color = MintJade)
                        }
                        Switch(
                            checked = mod.published,
                            onCheckedChange = { onToggleModulePublished(mod.id, it) }
                        )
                    }
                    Text(mod.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { onEditModule(mod) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Rounded.Edit, contentDescription = "ویرایش")
                        }
                        IconButton(onClick = { onDeleteModule(mod.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = RoseError)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun FeatureToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UsersAdminTab(
    users: List<UserProfile>,
    onSetSubscription: (targetUserId: String, hasActive: Boolean, tier: SubscriptionTier) -> Unit,
    onSendRenewalNotification: (targetUserId: String, message: String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var renewalTargetUser by remember { mutableStateOf<UserProfile?>(null) }
    val q = search.trim().lowercase(Locale.ROOT)
    val filtered = remember(users, q) {
        users.filter {
            q.isEmpty() ||
                it.displayName.lowercase(Locale.ROOT).contains(q) ||
                it.email.lowercase(Locale.ROOT).contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("جستجوی کاربر با نام یا ایمیل...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(filtered, key = { it.userId }) { user ->
            val currentTier = SubscriptionTier.fromRaw(user.subscriptionTier)
            val daysLeft = remember(user.subscriptionExpiresAtMillis) {
                if (user.subscriptionExpiresAtMillis > 0L) {
                    val diff = user.subscriptionExpiresAtMillis - System.currentTimeMillis()
                    if (diff <= 0L) 0 else kotlin.math.ceil(diff / 86_400_000.0).toInt().coerceAtLeast(1)
                } else null
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (daysLeft != null) {
                                Text(
                                    text = if (daysLeft == 0) "اشتراک منقضی شده" else "$daysLeft روز باقی‌مانده تا تمدید",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (daysLeft <= 5) RoseError else ChampagneGold
                                )
                            }
                        }
                        Surface(
                            color = if (user.hasActiveSubscription) EmeraldSuccess.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ) {
                            Text(
                                text = currentTier.labelFa,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (user.hasActiveSubscription) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SubscriptionTier.entries.forEach { tier ->
                            val isCurrent = currentTier == tier
                            FilterChip(
                                selected = isCurrent,
                                onClick = {
                                    onSetSubscription(
                                        user.userId,
                                        tier != SubscriptionTier.FREE,
                                        tier
                                    )
                                },
                                label = { Text(tier.labelFa) }
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { renewalTargetUser = user },
                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("admin_send_renewal_notice_btn")
                    ) {
                        Text("ارسال نوتیفیکیشن یادآوری تمدید")
                    }
                }
            }
        }
    }

    renewalTargetUser?.let { target ->
        var msg by remember(target.userId) {
            mutableStateOf("اشتراک eSkyTell Pro شما رو به اتمام است. لطفاً جهت جلوگیری از قطع پل بلوتوث اقدام به تمدید نمایید.")
        }
        AlertDialog(
            onDismissRequest = { renewalTargetUser = null },
            title = { Text("ارسال نوتیفیکیشن تمدید اشتراک") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "گیرنده: ${target.displayName} (${target.email})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = msg,
                        onValueChange = { msg = it },
                        label = { Text("متن نوتیفیکیشن یادآور") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSendRenewalNotification(target.userId, msg)
                        renewalTargetUser = null
                    },
                    enabled = msg.isNotBlank()
                ) {
                    Text("ارسال فوری نوتیفیکیشن")
                }
            },
            dismissButton = {
                TextButton(onClick = { renewalTargetUser = null }) { Text("انصراف") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleEditorDialog(
    initial: AnnouncementModule?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        summary: String,
        category: AnnouncementCategory,
        actionLabel: String,
        actionValue: String,
        published: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var summary by remember { mutableStateOf(initial?.summary.orEmpty()) }
    var category by remember {
        mutableStateOf(
            if (initial != null) AnnouncementCategory.fromRaw(initial.category)
            else AnnouncementCategory.FEATURE
        )
    }
    var actionLabel by remember { mutableStateOf(initial?.actionLabel.orEmpty()) }
    var actionValue by remember { mutableStateOf(initial?.actionValue.orEmpty()) }
    var published by remember { mutableStateOf(initial?.published ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "افزودن قابلیت یا اطلاعیه جدید" else "ویرایش ماژول") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_module_title")
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("توضیحات برای کاربران *") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("admin_module_summary")
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AnnouncementCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.labelFa) }
                        )
                    }
                }
                OutlinedTextField(
                    value = actionLabel,
                    onValueChange = { actionLabel = it },
                    label = { Text("متن دکمه اقدام (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = actionValue,
                    onValueChange = { actionValue = it },
                    label = { Text("لینک وب یا کد شماره‌گیری tel:*100# (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("انتشار فوری برای کاربران", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && summary.isNotBlank()) {
                        onSave(title, summary, category, actionLabel, actionValue, published)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && summary.isNotBlank(),
                modifier = Modifier.testTag("admin_module_save_btn")
            ) {
                Text("ذخیره و انتشار")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
