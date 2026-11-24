package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.ContactEntity
import com.example.data.local.TransferHistoryEntity
import com.example.data.model.ActiveBridgeCall
import com.example.data.model.CallDirection
import com.example.data.model.PermissionSummary
import com.example.data.model.SmsDirection
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JakartaNumericFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintJade
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RoseError
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSmsDialog(
    onDismiss: () -> Unit,
    onConfirm: (sender: String, phone: String, body: String, direction: SmsDirection) -> Unit
) {
    var sender by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(SmsDirection.INCOMING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Sms, contentDescription = null) },
        title = { Text("افزودن پیامک به صندوق") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text("نام فرستنده / مخاطب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sms_sender_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("شماره تلفن") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sms_phone_input")
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("متن پیامک") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("sms_body_input")
                )
                Text("نوع پیام:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmsDirection.entries.forEach { item ->
                        FilterChip(
                            selected = direction == item,
                            onClick = { direction = item },
                            label = { Text(item.labelFa) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (body.isNotBlank() || phone.isNotBlank()) {
                        onConfirm(sender, phone.ifBlank { "نامشخص" }, body, direction)
                        onDismiss()
                    }
                },
                enabled = body.isNotBlank() || phone.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_sms_button")
            ) {
                Text("افزودن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun ComposeSmsDialog(
    contacts: List<ContactEntity>,
    activeSimLabel: String,
    onDismiss: () -> Unit,
    onSend: (phone: String, name: String, body: String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null) },
        title = { Text("پیام جدید eSkyTell") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.SimCard,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "ارسال از طریق: $activeSimLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (contacts.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(contacts.take(10), key = { it.id }) { contact ->
                            FilterChip(
                                selected = phone == contact.phoneNumber,
                                onClick = {
                                    phone = contact.phoneNumber
                                    name = contact.displayName
                                },
                                label = { Text(contact.displayName) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("شماره گیرنده *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("compose_sms_phone")
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام مخاطب (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("compose_sms_name")
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("متن پیامک *") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("compose_sms_body")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phone.isNotBlank() && body.isNotBlank()) {
                        onSend(phone, name, body)
                        onDismiss()
                    }
                },
                enabled = phone.isNotBlank() && body.isNotBlank(),
                modifier = Modifier.testTag("confirm_compose_sms_button")
            ) {
                Text("ارسال پیامک")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCallDialog(
    onDismiss: () -> Unit,
    onConfirm: (contact: String, phone: String, durationSeconds: Int, direction: CallDirection) -> Unit
) {
    var contact by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("60") }
    var direction by remember { mutableStateOf(CallDirection.INCOMING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.PhoneIphone, contentDescription = null) },
        title = { Text("افزودن سابقه تماس") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("نام مخاطب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("call_contact_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("شماره تلفن *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("call_phone_input")
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("مدت تماس (ثانیه)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("call_duration_input")
                )
                Text("وضعیت تماس:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CallDirection.entries.forEach { item ->
                        FilterChip(
                            selected = direction == item,
                            onClick = { direction = item },
                            label = { Text(item.labelFa) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phone.isNotBlank()) {
                        onConfirm(
                            contact,
                            phone,
                            durationText.toIntOrNull() ?: 0,
                            direction
                        )
                        onDismiss()
                    }
                },
                enabled = phone.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_call_button")
            ) {
                Text("افزودن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun AddEditContactDialog(
    initialContact: ContactEntity? = null,
    initialPhone: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, email: String, notes: String, isFavorite: Boolean, existingId: Long?) -> Unit
) {
    var name by remember { mutableStateOf(initialContact?.displayName.orEmpty()) }
    var phone by remember { mutableStateOf(initialContact?.phoneNumber ?: initialPhone) }
    var email by remember { mutableStateOf(initialContact?.email.orEmpty()) }
    var notes by remember { mutableStateOf(initialContact?.notes.orEmpty()) }
    var favorite by remember { mutableStateOf(initialContact?.isFavorite ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
        title = { Text(if (initialContact == null) "افزودن مخاطب جدید" else "ویرایش مخاطب") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام و نام خانوادگی *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("contact_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("شماره تلفن *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("contact_phone_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ایمیل (اختیاری)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("contact_email_input")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("contact_notes_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مخاطب برگزیده", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = favorite, onCheckedChange = { favorite = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phone.isNotBlank()) {
                        onConfirm(
                            name.ifBlank { phone },
                            phone,
                            email,
                            notes,
                            favorite,
                            initialContact?.id
                        )
                        onDismiss()
                    }
                },
                enabled = phone.isNotBlank(),
                modifier = Modifier.testTag("confirm_save_contact_button")
            ) {
                Text("ذخیره مخاطب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun PasteTextDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.ContentPaste, contentDescription = null) },
        title = { Text("ورود سریع متن یا شماره‌ها") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "متن پیامک، فهرست شماره‌ها یا خروجی XML/JSON را اینجا بچسبانید تا وارد eSkyTell شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        clipboard.getText()?.text?.let { text = it }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("paste_clipboard_button")
                ) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("چسباندن از کلیپ‌بورد")
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("متن پیامک یا تماس‌ها") },
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier.fillMaxWidth().testTag("paste_text_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text)
                        onDismiss()
                    }
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("confirm_paste_button")
            ) {
                Text("استخراج و افزودن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsManagerSheet(
    summary: PermissionSummary,
    onRequestAllPermitted: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "مجوزهای فعال eSkyTell",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "دسترسی‌های لازم برای ارتباط زنده بلوتوثی (BLE/Classic)، همگام‌سازی دفترچه مخاطبین، مسیریابی صدا و اعلان‌های فوری:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PermissionRow(
                icon = Icons.Rounded.Bluetooth,
                title = "بلوتوث و اسکن BLE / Classic",
                description = "جستجو، جفت‌سازی و اتصال دوطرفه با آیفون و گوشی‌های دیگر",
                granted = summary.bluetoothGranted
            )
            PermissionRow(
                icon = Icons.Rounded.Contacts,
                title = "خواندن و ذخیره مخاطبین",
                description = "همگام‌سازی مخاطبین گوشی و ارسال مستقیم به آیفون با فرمت vCard",
                granted = summary.contactsGranted
            )
            PermissionRow(
                icon = Icons.Rounded.Notifications,
                title = "اعلان‌های زنده تماس و پیامک",
                description = "نمایش فوری هشدارهای دریافتی از پل بلوتوث",
                granted = summary.notificationsGranted
            )
            PermissionRow(
                icon = Icons.Rounded.Call,
                title = "شماره‌گیر و مسیریابی صدا (بدون مجوز حساس)",
                description = "اجرای تماس از شماره‌گیر استاندارد و انتقال صوت روی بلوتوث",
                granted = true
            )

            Button(
                onClick = {
                    onRequestAllPermitted()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("grant_all_permitted_button")
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (summary.allGranted) "همه مجوزهای لازم فعال هستند" else "اعطای مجوزهای بلوتوث، مخاطبین و اعلان")
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (granted) EmeraldSuccess else AmberWarning
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                contentDescription = if (granted) "فعال" else "نیاز به تأیید",
                tint = if (granted) EmeraldSuccess else AmberWarning
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IphoneGuideSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.PhoneIphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "راهنمای پل بلوتوثی آیفون در eSkyTell",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            GuideStepCard(
                step = "۱",
                title = "جفت‌سازی آیفون یا گوشی دوم",
                body = "در تب «پل بلوتوث»، بلوتوث آیفون را روشن و جستجو را لمس کنید؛ سپس حالت «آماده دریافت زنده (BLE + RFCOMM)» را فعال نگه دارید."
            )
            GuideStepCard(
                step = "۲",
                title = "شماره‌گیری و پیامک با رابط آشنا و دو سیم‌کارت",
                body = "در تب «تلفن» یا «پیامک»، سیم‌کارت دلخواه (SIM 1 / SIM 2) را انتخاب کنید و تماس یا پیام را مستقیم یا از طریق پل بلوتوث بفرستید."
            )
            GuideStepCard(
                step = "۳",
                title = "اشتراک فایل به آیفون (vCard و HTML)",
                body = "برای انتقال فوری مخاطبین و گزارش پیامک‌ها به آیفون بدون نیاز به نصب اپ ثانویه، از خروجی استاندارد vCard یا گزارش HTML استفاده کنید."
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("متوجه شدم")
            }
        }
    }
}

@Composable
private fun GuideStepCard(step: String, title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActiveBridgeCallOverlay(
    activeCall: ActiveBridgeCall,
    simLabel: String,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleBluetoothAudio: () -> Unit,
    onLaunchSystemDialer: () -> Unit,
    onEndCall: () -> Unit
) {
    var elapsedSec by remember(activeCall.startedAtMillis) { mutableLongStateOf(0L) }
    LaunchedEffect(activeCall.startedAtMillis) {
        while (true) {
            elapsedSec = ((System.currentTimeMillis() - activeCall.startedAtMillis) / 1000L).coerceAtLeast(0L)
            delay(1000L)
        }
    }
    val mins = elapsedSec / 60
    val secs = elapsedSec % 60
    val timerText = String.format(Locale.US, "%02d:%02d", mins, secs)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF061612), ObsidianBg, Color(0xFF0B1E1A))
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 36.dp)
            ) {
                Surface(
                    color = MintJade.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, MintJade.copy(alpha = 0.45f)),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.BluetoothConnected,
                            contentDescription = null,
                            tint = MintJade,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${activeCall.targetDeviceName} • $simLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MintJade
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Surface(
                    color = MintJade.copy(alpha = 0.15f),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, ChampagneGold.copy(alpha = 0.55f)),
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = activeCall.contactName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = activeCall.contactName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = activeCall.phoneNumber,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = Color(0xFF9EC2B9)
                )
                Text(
                    text = "تماس پل زنده • $timerText",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                    color = ChampagneGold
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallControlCircle(
                        icon = Icons.Rounded.MicOff,
                        label = "بی‌صدا",
                        active = activeCall.isMuted,
                        onClick = onToggleMute,
                        testTag = "call_mute_btn"
                    )
                    CallControlCircle(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        label = "اسپیکر",
                        active = activeCall.isSpeaker,
                        onClick = onToggleSpeaker,
                        testTag = "call_speaker_btn"
                    )
                    CallControlCircle(
                        icon = Icons.Rounded.BluetoothAudio,
                        label = "صوت بلوتوث",
                        active = activeCall.isBluetoothAudio,
                        onClick = onToggleBluetoothAudio,
                        testTag = "call_bt_audio_btn"
                    )
                    CallControlCircle(
                        icon = Icons.Rounded.Dialpad,
                        label = "شماره‌گیر گوشی",
                        active = false,
                        onClick = onLaunchSystemDialer,
                        testTag = "call_native_dialer_btn"
                    )
                }

                Button(
                    onClick = onEndCall,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("end_bridge_call_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        contentDescription = "پایان تماس",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CallControlCircle(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (active) MintJade else Color(0xFF1A2F2A),
            modifier = Modifier
                .size(58.dp)
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) Color(0xFF00241C) else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFC2DBD5))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    history: List<TransferHistoryEntity>,
    onDeleteEntry: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "گزارش انتقال‌های بلوتوثی (${history.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory, modifier = Modifier.testTag("clear_history_button")) {
                        Text("پاک کردن", color = RoseError)
                    }
                }
            }

            if (history.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "هنوز انتقالی ثبت نشده است. پس از ارسال یا دریافت پیامک/تماس از طریق بلوتوث، گزارش اینجا نمایش داده می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(340.dp)
                ) {
                    items(history, key = { it.id }) { entry ->
                        val statusColor = when {
                            entry.status.contains("ناموفق") -> RoseError
                            entry.status.contains("موفق") || entry.status.contains("ارسال") -> EmeraldSuccess
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(entry.targetDeviceName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(entry.status, style = MaterialTheme.typography.labelSmall, color = statusColor)
                                    }
                                    Text(
                                        text = "${entry.channelMethod} • ${entry.exportFormat}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${entry.smsCount} پیامک • ${entry.callCount} تماس • ${dateFmt.format(Date(entry.timestampMillis))}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteEntry(entry.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف گزارش")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
