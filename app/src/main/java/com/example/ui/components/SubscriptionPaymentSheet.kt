package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CurrencyBitcoin
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppGlobalConfig
import com.example.data.model.PaymentMethodType
import com.example.data.model.PaymentRequest
import com.example.data.model.PaymentRequestStatus
import com.example.data.model.SubscriptionTier
import com.example.data.model.formatTomanAmount
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JakartaNumericFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintJade
import com.example.ui.theme.RoseError
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaywallBottomSheet(
    appConfig: AppGlobalConfig,
    userPaymentRequests: List<PaymentRequest>,
    isCloudSaving: Boolean,
    onOpenPaymentUrl: (String) -> Unit,
    onSubmitPaymentRequest: (
        requestedTier: SubscriptionTier,
        amountToman: Long,
        paymentMethod: PaymentMethodType,
        targetAccountLabel: String,
        trackingCode: String,
        receiptImageBase64: String,
        userNote: String,
        onSubmitted: () -> Unit
    ) -> Unit,
    onNotifyCopied: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedTier by remember { mutableStateOf(SubscriptionTier.YEARLY) }
    var paymentMethod by remember {
        mutableStateOf(
            when {
                appConfig.bankCards.isNotEmpty() -> PaymentMethodType.CARD_TO_CARD
                appConfig.cryptoWallets.isNotEmpty() -> PaymentMethodType.CRYPTO
                else -> PaymentMethodType.CARD_TO_CARD
            }
        )
    }
    var selectedAccountLabel by remember { mutableStateOf("") }
    var trackingCode by remember { mutableStateOf("") }
    var userNote by remember { mutableStateOf("") }
    var receiptBase64 by remember { mutableStateOf("") }
    var isProcessingImage by remember { mutableStateOf(false) }

    val receiptBitmap = remember(receiptBase64) {
        ReceiptImageHelper.decodeBase64ToImageBitmap(receiptBase64)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingImage = true
            scope.launch {
                val encoded = ReceiptImageHelper.encodeUriToBase64(context, uri)
                isProcessingImage = false
                if (encoded != null) {
                    receiptBase64 = encoded
                } else {
                    onNotifyCopied("امکان پردازش تصویر فیش وجود نداشت؛ تصویر دیگری انتخاب کنید.")
                }
            }
        }
    }

    val monthlyPriceText = if (appConfig.monthlyPriceToman > 0L) {
        formatTomanAmount(appConfig.monthlyPriceToman, "/ ماه")
    } else {
        appConfig.monthlyPriceLabel
    }
    val yearlyPriceText = if (appConfig.yearlyPriceToman > 0L) {
        formatTomanAmount(appConfig.yearlyPriceToman, "/ سال")
    } else {
        appConfig.yearlyPriceLabel
    }
    val selectedAmountToman = if (selectedTier == SubscriptionTier.YEARLY) {
        appConfig.yearlyPriceToman
    } else {
        appConfig.monthlyPriceToman
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = ChampagneGold.copy(alpha = 0.18f),
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = ChampagneGold,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = "ارتقا به اشتراک ویژه eSkyTell Pro",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = appConfig.paywallMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // 1. Select Plan (Monthly / Yearly)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SelectablePlanCard(
                    title = "پلن ماهانه",
                    price = monthlyPriceText,
                    badge = if (appConfig.freeTrialDays > 0) "${appConfig.freeTrialDays} روز تست" else null,
                    selected = selectedTier == SubscriptionTier.MONTHLY,
                    onClick = { selectedTier = SubscriptionTier.MONTHLY },
                    modifier = Modifier.weight(1f).testTag("paywall_plan_monthly")
                )
                SelectablePlanCard(
                    title = "پلن سالانه",
                    price = yearlyPriceText,
                    badge = "به‌صرفه‌تر",
                    selected = selectedTier == SubscriptionTier.YEARLY,
                    onClick = { selectedTier = SubscriptionTier.YEARLY },
                    modifier = Modifier.weight(1f).testTag("paywall_plan_yearly")
                )
            }

            // 2. Payment Destinations (Bank Cards & Crypto Wallets configured by Admin)
            if (appConfig.bankCards.isNotEmpty() || appConfig.cryptoWallets.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "اطلاعات واریز وجه (${formatTomanAmount(selectedAmountToman)})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        appConfig.bankCards.forEach { card ->
                            val label = "${card.bankName.ifBlank { "بانک" }} • ${card.cardNumber}"
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedAccountLabel == label) MintJade else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                ),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        paymentMethod = PaymentMethodType.CARD_TO_CARD
                                        selectedAccountLabel = label
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = MintJade, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = listOf(card.bankName, card.holderName).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "کارت بانکی" },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = formatCardNumberDisplay(card.cardNumber),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                                                color = ChampagneGold
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(card.cardNumber))
                                            paymentMethod = PaymentMethodType.CARD_TO_CARD
                                            selectedAccountLabel = label
                                            onNotifyCopied("شماره کارت کپی شد.")
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی شماره کارت", tint = MintJade)
                                    }
                                }
                            }
                        }

                        appConfig.cryptoWallets.forEach { wallet ->
                            val label = "${wallet.networkName.ifBlank { "کریپتو" }} • ${wallet.address.take(12)}..."
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedAccountLabel == label) ChampagneGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                ),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        paymentMethod = PaymentMethodType.CRYPTO
                                        selectedAccountLabel = label
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Rounded.CurrencyBitcoin, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = wallet.networkName.ifBlank { "کیف پول کریپتو" },
                                                style = MaterialTheme.typography.labelMedium,
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
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(wallet.address))
                                            paymentMethod = PaymentMethodType.CRYPTO
                                            selectedAccountLabel = label
                                            onNotifyCopied("آدرس کیف پول کپی شد.")
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی آدرس کیف پول", tint = ChampagneGold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (appConfig.paymentUrl.isNotBlank()) {
                OutlinedButton(
                    onClick = { onOpenPaymentUrl(appConfig.paymentUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("paywall_checkout_button")
                ) {
                    Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("پرداخت از طریق لینک درگاه آنلاین")
                }
            }

            // 3. Submit Receipt / Tracking Code Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MintJade.copy(alpha = 0.45f)),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = MintJade)
                        Text(
                            text = "ثبت کد پیگیری یا آپلود تصویر فیش",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaymentMethodType.entries.forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method.labelFa) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = trackingCode,
                        onValueChange = { trackingCode = it },
                        label = { Text("کد پیگیری بانکی یا هش تراکنش (TxHash)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("payment_tracking_input")
                    )

                    FilledTonalButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessingImage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("payment_pick_receipt_btn")
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (receiptBitmap != null) "تغییر تصویر فیش واریزی"
                            else "انتخاب تصویر فیش واریزی از گالری"
                        )
                    }

                    if (isProcessingImage) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    if (receiptBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .clip(MaterialTheme.shapes.medium)
                        ) {
                            Image(
                                bitmap = receiptBitmap,
                                contentDescription = "پیش‌نمایش فیش واریزی",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                IconButton(
                                    onClick = { receiptBase64 = "" },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "حذف تصویر فیش", tint = RoseError)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = userNote,
                        onValueChange = { userNote = it },
                        label = { Text("توضیحات (اختیاری - مثلاً ۴ رقم آخر کارت مبدأ)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSubmitPaymentRequest(
                                selectedTier,
                                selectedAmountToman,
                                paymentMethod,
                                selectedAccountLabel,
                                trackingCode,
                                receiptBase64,
                                userNote
                            ) {
                                trackingCode = ""
                                receiptBase64 = ""
                                userNote = ""
                            }
                        },
                        enabled = !isCloudSaving && !isProcessingImage && (trackingCode.isNotBlank() || receiptBase64.isNotBlank()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_payment_request_btn")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ثبت و ارسال برای تایید مدیریت")
                    }
                }
            }

            // 4. User's Submitted Requests History
            if (userPaymentRequests.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "درخواست‌های ثبت‌شده شما (${userPaymentRequests.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    userPaymentRequests.take(5).forEach { req ->
                        val st = PaymentRequestStatus.fromRaw(req.status)
                        val tier = SubscriptionTier.fromRaw(req.requestedTier)
                        val statusColor = when (st) {
                            PaymentRequestStatus.PENDING -> AmberWarning
                            PaymentRequestStatus.APPROVED -> EmeraldSuccess
                            PaymentRequestStatus.REJECTED -> RoseError
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${tier.labelFa} • ${formatTomanAmount(req.amountToman)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(color = statusColor.copy(alpha = 0.18f), shape = CircleShape) {
                                        Text(
                                            text = st.labelFa,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (req.trackingCode.isNotBlank()) {
                                    Text(
                                        text = "کد پیگیری: ${req.trackingCode}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (req.adminNote.isNotBlank()) {
                                    Text(
                                        text = "پاسخ مدیریت: ${req.adminNote}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    }
}

@Composable
private fun SelectablePlanCard(
    title: String,
    price: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) ChampagneGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = price,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JakartaNumericFamily),
                color = if (selected) ChampagneGold else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (badge != null) {
                Surface(
                    color = ChampagneGold.copy(alpha = 0.18f),
                    shape = CircleShape
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampagneGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

fun formatCardNumberDisplay(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length == 16) {
        digits.chunked(4).joinToString(" - ")
    } else {
        raw
    }
}
