package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CurrencyBitcoin
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BankCardInfo
import com.example.data.model.CryptoWalletInfo
import com.example.data.model.PaymentMethodType
import com.example.data.model.PaymentRequest
import com.example.data.model.PaymentRequestStatus
import com.example.data.model.SubscriptionTier
import com.example.data.model.formatTomanAmount
import com.example.ui.components.ReceiptImageHelper
import com.example.ui.components.formatCardNumberDisplay
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintJade
import com.example.ui.theme.RoseError
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun BankCardsManagerCard(
    cards: List<BankCardInfo>,
    onAddClick: () -> Unit,
    onEditClick: (Int, BankCardInfo) -> Unit,
    onDeleteClick: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = MintJade)
                    Text(
                        text = "کارت‌های بانکی (${cards.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                FilledTonalButton(
                    onClick = onAddClick,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_add_bank_card_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("افزودن کارت")
                }
            }

            if (cards.isEmpty()) {
                Text(
                    text = "هنوز شماره کارتی ثبت نشده است. با افزودن کارت، کاربران در صفحه خرید اشتراک آن را مشاهده می‌کنند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                cards.forEachIndexed { index, card ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = listOf(card.bankName, card.holderName)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" • ")
                                        .ifBlank { "کارت بانکی" },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatCardNumberDisplay(card.cardNumber),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                                    color = ChampagneGold
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { onEditClick(index, card) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "ویرایش کارت")
                                }
                                IconButton(
                                    onClick = { onDeleteClick(index) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف کارت", tint = RoseError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CryptoWalletsManagerCard(
    wallets: List<CryptoWalletInfo>,
    onAddClick: () -> Unit,
    onEditClick: (Int, CryptoWalletInfo) -> Unit,
    onDeleteClick: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.CurrencyBitcoin, contentDescription = null, tint = ChampagneGold)
                    Text(
                        text = "کیف پول‌های کریپتو (${wallets.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                FilledTonalButton(
                    onClick = onAddClick,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_add_crypto_wallet_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("افزودن ولت")
                }
            }

            if (wallets.isEmpty()) {
                Text(
                    text = "هنوز آدرس کیف پول ارز دیجیتالی ثبت نشده است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                wallets.forEachIndexed { index, wallet ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = wallet.networkName.ifBlank { "رمزارز" },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = wallet.address,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { onEditClick(index, wallet) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "ویرایش کیف پول")
                                }
                                IconButton(
                                    onClick = { onDeleteClick(index) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف کیف پول", tint = RoseError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BankCardEditorDialog(
    initial: BankCardInfo?,
    onDismiss: () -> Unit,
    onSave: (BankCardInfo) -> Unit
) {
    var bankName by remember { mutableStateOf(initial?.bankName.orEmpty()) }
    var cardNumber by remember { mutableStateOf(initial?.cardNumber.orEmpty()) }
    var holderName by remember { mutableStateOf(initial?.holderName.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "افزودن شماره کارت بانکی" else "ویرایش کارت بانکی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("نام بانک (مثلاً بانک ملت)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_card_bank_name")
                )
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { input -> cardNumber = input.filter { it.isDigit() || it == '-' || it == ' ' }.take(24) },
                    label = { Text("شماره کارت ۱۶ رقمی *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_card_number_input")
                )
                OutlinedTextField(
                    value = holderName,
                    onValueChange = { holderName = it },
                    label = { Text("نام صاحب حساب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_card_holder_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanNumber = cardNumber.filter { it.isDigit() }.ifBlank { cardNumber.trim() }
                    if (cleanNumber.isNotBlank()) {
                        onSave(
                            BankCardInfo(
                                bankName = bankName.trim(),
                                cardNumber = cleanNumber,
                                holderName = holderName.trim()
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = cardNumber.isNotBlank(),
                modifier = Modifier.testTag("admin_card_save_btn")
            ) {
                Text("ذخیره کارت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
internal fun CryptoWalletEditorDialog(
    initial: CryptoWalletInfo?,
    onDismiss: () -> Unit,
    onSave: (CryptoWalletInfo) -> Unit
) {
    var networkName by remember { mutableStateOf(initial?.networkName.orEmpty()) }
    var address by remember { mutableStateOf(initial?.address.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "افزودن کیف پول کریپتوکارنسی" else "ویرایش کیف پول") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = networkName,
                    onValueChange = { networkName = it },
                    label = { Text("نام ارز و شبکه (مثلاً USDT TRC20) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_wallet_network_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("آدرس کیف پول *") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("admin_wallet_address_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (networkName.isNotBlank() && address.isNotBlank()) {
                        onSave(
                            CryptoWalletInfo(
                                networkName = networkName.trim(),
                                address = address.trim()
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = networkName.isNotBlank() && address.isNotBlank(),
                modifier = Modifier.testTag("admin_wallet_save_btn")
            ) {
                Text("ذخیره کیف پول")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PaymentRequestsAdminTab(
    requests: List<PaymentRequest>,
    isSaving: Boolean,
    onReviewRequest: (request: PaymentRequest, approve: Boolean, adminNote: String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var filterPendingOnly by remember { mutableStateOf(true) }
    var zoomedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var reviewTarget by remember { mutableStateOf<Pair<PaymentRequest, Boolean>?>(null) }

    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }
    val pendingCount = remember(requests) {
        requests.count { PaymentRequestStatus.fromRaw(it.status) == PaymentRequestStatus.PENDING }
    }
    val visibleRequests = remember(requests, filterPendingOnly) {
        if (filterPendingOnly) {
            requests.filter { PaymentRequestStatus.fromRaw(it.status) == PaymentRequestStatus.PENDING }
        } else {
            requests
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterPendingOnly,
                    onClick = { filterPendingOnly = true },
                    label = { Text("در انتظار تایید ($pendingCount)") },
                    modifier = Modifier.testTag("admin_payments_filter_pending")
                )
                FilterChip(
                    selected = !filterPendingOnly,
                    onClick = { filterPendingOnly = false },
                    label = { Text("همه درخواست‌ها (${requests.size})") },
                    modifier = Modifier.testTag("admin_payments_filter_all")
                )
            }
        }

        if (visibleRequests.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (filterPendingOnly) "فیش واریزی جدیدی در انتظار بررسی نیست."
                            else "هنوز هیچ درخواست پرداختی ثبت نشده است.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(visibleRequests, key = { it.id }) { req ->
                val status = PaymentRequestStatus.fromRaw(req.status)
                val tier = SubscriptionTier.fromRaw(req.requestedTier)
                val method = PaymentMethodType.fromRaw(req.paymentMethod)
                val statusColor = when (status) {
                    PaymentRequestStatus.PENDING -> AmberWarning
                    PaymentRequestStatus.APPROVED -> EmeraldSuccess
                    PaymentRequestStatus.REJECTED -> RoseError
                }
                val receiptBitmap = remember(req.receiptImageBase64) {
                    ReceiptImageHelper.decodeBase64ToImageBitmap(req.receiptImageBase64)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = req.userDisplayName.ifBlank { req.userEmail.ifBlank { "کاربر eSkyTell" } },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (req.userEmail.isNotBlank()) {
                                    Text(
                                        text = req.userEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(color = statusColor.copy(alpha = 0.18f), shape = CircleShape) {
                                Text(
                                    text = status.labelFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${tier.labelFa} • ${formatTomanAmount(req.amountToman)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                            req.createdAt?.toDate()?.let { d ->
                                Text(
                                    text = dateFormatter.format(d),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "روش واریز: ${method.labelFa}" +
                                if (req.targetAccountLabel.isNotBlank()) " (${req.targetAccountLabel})" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (req.trackingCode.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "کد پیگیری: ${req.trackingCode}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { clipboard.setText(AnnotatedString(req.trackingCode)) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی کد پیگیری", tint = MintJade)
                                    }
                                }
                            }
                        }

                        if (req.userNote.isNotBlank()) {
                            Text(
                                text = "توضیحات کاربر: ${req.userNote}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (receiptBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 190.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { zoomedBitmap = receiptBitmap }
                            ) {
                                Image(
                                    bitmap = receiptBitmap,
                                    contentDescription = "تصویر فیش واریزی",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("بزرگ‌نمایی فیش", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        if (req.adminNote.isNotBlank()) {
                            Text(
                                text = "یادداشت مدیر: ${req.adminNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor
                            )
                        }

                        if (status == PaymentRequestStatus.PENDING) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { reviewTarget = req to true },
                                    enabled = !isSaving,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("admin_approve_payment_btn")
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تایید و ارتقا")
                                }
                                OutlinedButton(
                                    onClick = { reviewTarget = req to false },
                                    enabled = !isSaving,
                                    border = BorderStroke(1.dp, RoseError.copy(alpha = 0.7f)),
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("admin_reject_payment_btn")
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = null, tint = RoseError, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("رد درخواست", color = RoseError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    zoomedBitmap?.let { bmp ->
        AlertDialog(
            onDismissRequest = { zoomedBitmap = null },
            title = { Text("تصویر کامل فیش واریزی") },
            text = {
                Image(
                    bitmap = bmp,
                    contentDescription = "تصویر فیش واریزی",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            },
            confirmButton = {
                TextButton(onClick = { zoomedBitmap = null }) { Text("بستن") }
            }
        )
    }

    reviewTarget?.let { (req, approve) ->
        var adminNote by remember(req.id, approve) { mutableStateOf("") }
        val tier = SubscriptionTier.fromRaw(req.requestedTier)
        AlertDialog(
            onDismissRequest = { reviewTarget = null },
            title = {
                Text(if (approve) "تایید پرداخت و ارتقای خودکار" else "رد درخواست پرداخت")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (approve) {
                            "با تایید این درخواست، اشتراک کاربر «${req.userDisplayName.ifBlank { req.userEmail }}» به صورت خودکار به «${tier.labelFa}» ارتقا می‌یابد."
                        } else {
                            "آیا از رد درخواست کاربر «${req.userDisplayName.ifBlank { req.userEmail }}» مطمئن هستید؟"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = adminNote,
                        onValueChange = { adminNote = it },
                        label = { Text(if (approve) "یادداشت تایید (اختیاری)" else "علت رد درخواست (اختیاری)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_payment_review_note")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReviewRequest(req, approve, adminNote.trim())
                        reviewTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (approve) EmeraldSuccess else RoseError
                    ),
                    modifier = Modifier.testTag("admin_payment_review_confirm")
                ) {
                    Text(if (approve) "تایید و ارتقای کاربر" else "تایید رد درخواست")
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewTarget = null }) { Text("انصراف") }
            }
        )
    }
}
