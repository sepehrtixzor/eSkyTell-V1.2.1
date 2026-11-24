package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MintJade
import com.example.ui.theme.RoseError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun PasswordRecoveryDialog(
    initialEmail: String = "",
    currentPairingPin: String? = null,
    onRegeneratePairingPin: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var isSending by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.LockReset,
                contentDescription = null,
                tint = MintJade,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text("بازیابی رمز عبور و امنیت حساب", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ایمیل حساب خود را وارد کنید تا لینک بازنشانی رمز عبور به ایمیل شما ارسال شود، یا مستقیماً رمز حساب گوگل خود را بازیابی کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        feedbackMessage = null
                    },
                    label = { Text("نشانی ایمیل") },
                    leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recovery_email_input")
                )

                Button(
                    onClick = {
                        val clean = email.trim()
                        if (!Patterns.EMAIL_ADDRESS.matcher(clean).matches()) {
                            feedbackMessage = false to "لطفاً یک نشانی ایمیل معتبر وارد کنید."
                            return@Button
                        }
                        isSending = true
                        feedbackMessage = null
                        scope.launch {
                            val result = runCatching {
                                Firebase.auth.sendPasswordResetEmail(clean).await()
                            }
                            isSending = false
                            feedbackMessage = if (result.isSuccess) {
                                true to "لینک بازیابی رمز عبور به «$clean» ارسال شد. پوشه Inbox و Spam ایمیل خود را بررسی کنید."
                            } else {
                                val msg = result.exceptionOrNull()?.localizedMessage.orEmpty()
                                if (msg.contains("no user record", ignoreCase = true) ||
                                    msg.contains("USER_NOT_FOUND", ignoreCase = true)
                                ) {
                                    false to "حساب رمزدار مستقلی با این ایمیل ثبت نشده است. اگر با گوگل وارد می‌شوید از دکمه بازیابی گوگل در زیر استفاده کنید."
                                } else {
                                    false to "امکان ارسال ایمیل بازیابی نبود (${msg.ifBlank { "بررسی اتصال اینترنت" }}). می‌توانید از بازیابی حساب گوگل استفاده کنید."
                                }
                            }
                        }
                    },
                    enabled = !isSending && email.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("recovery_send_email_btn")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("ارسال لینک بازیابی رمز عبور")
                    }
                }

                feedbackMessage?.let { (isOk, msg) ->
                    val tone = if (isOk) EmeraldSuccess else RoseError
                    Surface(
                        color = tone.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, tone.copy(alpha = 0.45f)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = tone,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.google.com/signin/recovery"))
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("recovery_google_account_btn")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("بازیابی رمز حساب گوگل (Google Recovery)")
                }

                if (currentPairingPin != null && onRegeneratePairingPin != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Key, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "پین فعلی رمزنگاری بلوتوث: $currentPairingPin",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    onRegeneratePairingPin()
                                    feedbackMessage = true to "پین امنیتی جدید برای پل بلوتوث ایجاد و فعال شد."
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("recovery_reset_pin_btn")
                            ) {
                                Text("بازنشانی و تولید پین جدید بلوتوث")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )
}
