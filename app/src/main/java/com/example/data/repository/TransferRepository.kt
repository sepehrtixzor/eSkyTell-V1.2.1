package com.example.data.repository

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.util.Xml
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.CallRecordEntity
import com.example.data.local.ContactEntity
import com.example.data.local.SmsRecordEntity
import com.example.data.local.TransferDao
import com.example.data.local.TransferHistoryEntity
import com.example.data.model.BridgeCommandType
import com.example.data.model.CallDirection
import com.example.data.model.ExportFormat
import com.example.data.model.ImportResult
import com.example.data.model.IncomingBridgeEvent
import com.example.data.model.SmsDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GeneratedExportFile(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val byteSize: Long
)

data class BridgePayloadResult(
    val importResult: ImportResult,
    val incomingEvent: IncomingBridgeEvent?
)

class TransferRepository(
    private val context: Context,
    private val dao: TransferDao
) {
    val smsRecords: Flow<List<SmsRecordEntity>> = dao.observeSmsRecords()
    val callRecords: Flow<List<CallRecordEntity>> = dao.observeCallRecords()
    val contacts: Flow<List<ContactEntity>> = dao.observeContacts()
    val transferHistory: Flow<List<TransferHistoryEntity>> = dao.observeTransferHistory()
    val cachedBridgeSmsCount: Flow<Int> = dao.observeCachedBridgeSmsCount()
    val cachedBridgeContactsCount: Flow<Int> = dao.observeCachedBridgeContactsCount()

    companion object {
        private const val BRIDGE_CHANNEL_ID = "eskytell_bridge_alerts"
    }

    private fun normalizePhoneKey(phone: String): String {
        val trimmed = phone.trim()
        val digits = trimmed.filter { it.isDigit() }
        return when {
            digits.length >= 9 -> digits.takeLast(9)
            digits.isNotEmpty() -> digits
            else -> trimmed.lowercase(Locale.ROOT)
        }
    }

    suspend fun addManualSms(
        senderName: String,
        phoneNumber: String,
        body: String,
        direction: SmsDirection,
        simSlot: Int = 1,
        sourceLabel: String = "ثبت مستقیم",
        isSyncedFromBridge: Boolean = false,
        syncedAtMillis: Long = if (isSyncedFromBridge) System.currentTimeMillis() else 0L,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        val cleanPhone = phoneNumber.trim().ifBlank { "نامشخص" }
        val cleanBody = body.trim()
        if (isSyncedFromBridge && dao.findMatchingSmsCount(cleanPhone, cleanBody, timestampMillis) > 0) {
            return
        }
        dao.insertSms(
            SmsRecordEntity(
                senderName = senderName.ifBlank { cleanPhone },
                phoneNumber = cleanPhone,
                body = cleanBody,
                timestampMillis = timestampMillis,
                direction = direction.name,
                sourceLabel = sourceLabel,
                simSlot = simSlot,
                isRead = direction != SmsDirection.INCOMING,
                isSyncedFromBridge = isSyncedFromBridge,
                syncedAtMillis = syncedAtMillis
            )
        )
    }

    suspend fun markThreadRead(phoneNumber: String) {
        dao.markThreadRead(phoneNumber)
    }

    suspend fun deleteSmsThread(phoneNumber: String) {
        dao.deleteSmsThread(phoneNumber)
    }

    suspend fun addManualCall(
        contactName: String,
        phoneNumber: String,
        durationSeconds: Int,
        direction: CallDirection,
        simSlot: Int = 1,
        sourceLabel: String = "شماره‌گیر eSkyTell"
    ) {
        dao.insertCall(
            CallRecordEntity(
                contactName = contactName.ifBlank { phoneNumber },
                phoneNumber = phoneNumber.trim(),
                durationSeconds = durationSeconds.coerceAtLeast(0),
                direction = direction.name,
                sourceLabel = sourceLabel,
                simSlot = simSlot
            )
        )
    }

    suspend fun upsertContact(
        displayName: String,
        phoneNumber: String,
        email: String = "",
        notes: String = "",
        isFavorite: Boolean = false,
        existingId: Long? = null,
        syncedFromBridge: Boolean = false,
        syncedDeviceName: String = ""
    ) {
        val cleanPhone = phoneNumber.trim()
        val cleanName = displayName.trim().ifBlank { cleanPhone }
        val now = System.currentTimeMillis()
        if (existingId != null) {
            dao.updateContact(
                ContactEntity(
                    id = existingId,
                    displayName = cleanName,
                    phoneNumber = cleanPhone,
                    email = email.trim(),
                    notes = notes.trim(),
                    isFavorite = isFavorite,
                    syncedToSystem = false,
                    sourceLabel = if (syncedFromBridge) "کش بلوتوث" else "ویرایش‌شده",
                    updatedAtMillis = now,
                    syncedFromBridge = syncedFromBridge,
                    syncedDeviceName = syncedDeviceName
                )
            )
        } else {
            val normalizedTarget = normalizePhoneKey(cleanPhone)
            val existing = dao.findContactByPhone(cleanPhone)
                ?: dao.getAllContactsSnapshot().firstOrNull {
                    normalizePhoneKey(it.phoneNumber) == normalizedTarget
                }
            if (existing != null) {
                dao.updateContact(
                    existing.copy(
                        displayName = cleanName.ifBlank { existing.displayName },
                        email = email.trim().ifBlank { existing.email },
                        notes = notes.trim().ifBlank { existing.notes },
                        isFavorite = isFavorite || existing.isFavorite,
                        updatedAtMillis = now,
                        syncedFromBridge = syncedFromBridge || existing.syncedFromBridge,
                        syncedDeviceName = syncedDeviceName.ifBlank { existing.syncedDeviceName }
                    )
                )
            } else {
                dao.insertContact(
                    ContactEntity(
                        displayName = cleanName,
                        phoneNumber = cleanPhone,
                        email = email.trim(),
                        notes = notes.trim(),
                        isFavorite = isFavorite,
                        sourceLabel = if (syncedFromBridge && syncedDeviceName.isNotBlank()) {
                            "بلوتوث: $syncedDeviceName"
                        } else if (syncedFromBridge) {
                            "کش بلوتوث"
                        } else {
                            "برنامه"
                        },
                        updatedAtMillis = now,
                        syncedFromBridge = syncedFromBridge,
                        syncedDeviceName = syncedDeviceName
                    )
                )
            }
        }
    }

    suspend fun setContactFavorite(id: Long, favorite: Boolean) =
        dao.setContactFavorite(id, favorite)

    suspend fun deleteContact(id: Long) = dao.deleteContact(id)

    suspend fun importDeviceContacts(): Int = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext 0
        }

        val batch = mutableListOf<ContactEntity>()
        val seen = mutableSetOf<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )
        cursor?.use { c ->
            val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext()) {
                val name = if (nameIdx >= 0) c.getString(nameIdx).orEmpty() else ""
                val phone = if (phoneIdx >= 0) c.getString(phoneIdx).orEmpty().trim() else ""
                if (phone.isNotBlank() && seen.add(phone)) {
                    val existing = dao.findContactByPhone(phone)
                    if (existing == null) {
                        batch.add(
                            ContactEntity(
                                displayName = name.ifBlank { phone },
                                phoneNumber = phone,
                                syncedToSystem = true,
                                sourceLabel = "دفترچه گوشی"
                            )
                        )
                    }
                }
            }
        }
        if (batch.isNotEmpty()) {
            dao.insertContacts(batch)
        }
        batch.size
    }

    suspend fun markContactSynced(id: Long) = dao.setContactSyncedToSystem(id, true)

    suspend fun toggleSmsSelection(id: Long, selected: Boolean) = dao.setSmsSelected(id, selected)
    suspend fun toggleCallSelection(id: Long, selected: Boolean) = dao.setCallSelected(id, selected)
    suspend fun setAllSelected(selected: Boolean) {
        dao.setAllSmsSelected(selected)
        dao.setAllCallsSelected(selected)
    }

    suspend fun deleteSms(id: Long) = dao.deleteSms(id)
    suspend fun deleteCall(id: Long) = dao.deleteCall(id)
    suspend fun clearQueue() {
        dao.clearAllSms()
        dao.clearAllCalls()
    }

    suspend fun deleteHistory(id: Long) = dao.deleteHistory(id)
    suspend fun clearHistory() = dao.clearAllHistory()

    suspend fun recordTransferHistory(entry: TransferHistoryEntity) = dao.insertHistory(entry)

    fun postBridgeNotification(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BRIDGE_CHANNEL_ID,
                "رویدادهای پل بلوتوث eSkyTell",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, BRIDGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_eskytell_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    suspend fun importSharedOrPastedText(rawText: String, sourceLabel: String = "کلیپ‌بورد"): ImportResult =
        withContext(Dispatchers.IO) {
            val trimmed = rawText.trim()
            if (trimmed.isEmpty()) return@withContext ImportResult(0, 0, sourceLabel)

            if (trimmed.startsWith("{")) {
                val parsed = parseJsonBundle(trimmed, sourceLabel, markAsSyncedCache = false)
                if (parsed.smsAdded > 0 || parsed.callsAdded > 0 || parsed.contactsAdded > 0) {
                    return@withContext parsed
                }
            }

            if (trimmed.startsWith("<")) {
                val parsed = parseXmlBackup(trimmed, sourceLabel)
                if (parsed.smsAdded > 0 || parsed.callsAdded > 0) return@withContext parsed
            }

            val phoneRegex = Regex("""(\+?\d[\d\s\-()]{6,16}\d)""")
            val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val smsBatch = mutableListOf<SmsRecordEntity>()
            val callBatch = mutableListOf<CallRecordEntity>()

            if (lines.size > 1) {
                for (line in lines) {
                    val match = phoneRegex.find(line)
                    val phone = match?.value?.replace(Regex("""[\s\-()]"""), "") ?: ""
                    val lower = line.lowercase(Locale.ROOT)
                    if (phone.isNotEmpty() && (line.contains("تماس") || lower.contains("call") || lower.contains("missed"))) {
                        val dir = when {
                            line.contains("از دست") || line.contains("بی‌پاسخ") || lower.contains("missed") -> CallDirection.MISSED
                            line.contains("خروجی") || lower.contains("outgoing") -> CallDirection.OUTGOING
                            else -> CallDirection.INCOMING
                        }
                        callBatch.add(
                            CallRecordEntity(
                                contactName = line.replace(match?.value ?: "", "").trim().ifBlank { phone }.take(40),
                                phoneNumber = phone,
                                durationSeconds = 0,
                                direction = dir.name,
                                sourceLabel = sourceLabel
                            )
                        )
                    } else {
                        smsBatch.add(
                            SmsRecordEntity(
                                senderName = if (phone.isNotEmpty()) phone else "پیام واردشده",
                                phoneNumber = phone.ifBlank { "نامشخص" },
                                body = line,
                                direction = SmsDirection.INCOMING.name,
                                sourceLabel = sourceLabel,
                                isRead = false
                            )
                        )
                    }
                }
            } else {
                val match = phoneRegex.find(trimmed)
                val phone = match?.value?.replace(Regex("""[\s\-()]"""), "") ?: ""
                smsBatch.add(
                    SmsRecordEntity(
                        senderName = if (phone.isNotEmpty()) phone else "متن اشتراک‌گذاری",
                        phoneNumber = phone.ifBlank { "نامشخص" },
                        body = trimmed,
                        direction = SmsDirection.INCOMING.name,
                        sourceLabel = sourceLabel,
                        isRead = false
                    )
                )
            }

            if (smsBatch.isNotEmpty()) dao.insertSmsList(smsBatch)
            if (callBatch.isNotEmpty()) dao.insertCallList(callBatch)
            ImportResult(smsBatch.size, callBatch.size, sourceLabel)
        }

    suspend fun importFromUri(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val fileName = resolveFileName(uri) ?: "فایل پشتیبان"
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
            it.readText()
        } ?: return@withContext ImportResult(0, 0, fileName)

        val lowerName = fileName.lowercase(Locale.ROOT)
        val trimmed = content.trim()
        return@withContext when {
            lowerName.endsWith(".json") || trimmed.startsWith("{") -> parseJsonBundle(trimmed, fileName)
            lowerName.endsWith(".xml") || trimmed.startsWith("<") -> parseXmlBackup(trimmed, fileName)
            lowerName.endsWith(".vcf") || trimmed.contains("BEGIN:VCARD", ignoreCase = true) ->
                parseVCard(trimmed, fileName)
            lowerName.endsWith(".csv") -> parseCsv(trimmed, fileName)
            else -> importSharedOrPastedText(trimmed, fileName)
        }
    }

    suspend fun importBridgePayload(jsonPayload: String, senderDeviceName: String): BridgePayloadResult =
        withContext(Dispatchers.IO) {
            try {
                val root = JSONObject(jsonPayload)
                val bridgeTypeRaw = root.optString("bridgeType", "")
                if (bridgeTypeRaw.isNotBlank()) {
                    val cmdType = BridgeCommandType.entries.firstOrNull { it.name == bridgeTypeRaw }
                        ?: BridgeCommandType.SMS_RELAY
                    val phone = root.optString("phoneNumber", "")
                    val contact = root.optString("contactName", phone.ifBlank { "مخاطب" })
                    val body = root.optString("messageBody", "")
                    val simSlot = root.optInt("simSlot", 1).coerceIn(1, 2)
                    val source = root.optString("sourceDeviceName", senderDeviceName)
                    val eventTimestamp = root.optLong("timestampMillis", System.currentTimeMillis())

                    var smsAdded = 0
                    var callsAdded = 0
                    var contactsAdded = 0
                    when (cmdType) {
                        BridgeCommandType.CALL_TRANSFER -> {
                            addManualCall(
                                contactName = contact,
                                phoneNumber = phone.ifBlank { "نامشخص" },
                                durationSeconds = 0,
                                direction = CallDirection.INCOMING,
                                simSlot = simSlot,
                                sourceLabel = "پل بلوتوث: $source"
                            )
                            callsAdded = 1
                            postBridgeNotification("تماس دریافتی از $source", "$contact ($phone)")
                        }
                        BridgeCommandType.SMS_RELAY -> {
                            val cleanPhone = phone.trim().ifBlank { "نامشخص" }
                            val cleanBody = body.trim().ifBlank { "پیام از پل بلوتوث" }
                            if (dao.findMatchingSmsCount(cleanPhone, cleanBody, eventTimestamp) == 0) {
                                addManualSms(
                                    senderName = contact,
                                    phoneNumber = cleanPhone,
                                    body = cleanBody,
                                    direction = SmsDirection.INCOMING,
                                    simSlot = simSlot,
                                    sourceLabel = "پل بلوتوث: $source",
                                    isSyncedFromBridge = true,
                                    syncedAtMillis = System.currentTimeMillis(),
                                    timestampMillis = eventTimestamp
                                )
                                smsAdded = 1
                            }
                            postBridgeNotification("پیامک جدید از $source", "$contact: $cleanBody")
                        }
                        BridgeCommandType.CONTACT_SYNC -> {
                            val contactsArray = root.optJSONArray("contacts")
                            if (contactsArray != null && contactsArray.length() > 0) {
                                contactsAdded = cacheSyncedContactsArray(
                                    contactsArray = contactsArray,
                                    sourceLabel = "پل بلوتوث: $source",
                                    deviceName = source,
                                    markAsSyncedCache = true
                                )
                            } else if (phone.isNotBlank()) {
                                upsertContact(
                                    displayName = contact,
                                    phoneNumber = phone,
                                    notes = "همگام‌سازی‌شده از $source",
                                    syncedFromBridge = true,
                                    syncedDeviceName = source
                                )
                                contactsAdded = 1
                            }
                        }
                    }

                    val event = IncomingBridgeEvent(
                        type = cmdType,
                        sourceDeviceName = source,
                        contactName = contact,
                        phoneNumber = phone,
                        messageBody = body,
                        simSlot = simSlot
                    )
                    return@withContext BridgePayloadResult(
                        ImportResult(smsAdded, callsAdded, "بلوتوث: $source", contactsAdded),
                        event
                    )
                }
            } catch (_: Exception) {
            }

            val importResult = parseJsonBundle(
                json = jsonPayload,
                sourceLabel = "بلوتوث: $senderDeviceName",
                markAsSyncedCache = true,
                syncedDeviceName = senderDeviceName
            )
            BridgePayloadResult(importResult, null)
        }

    fun buildRfcommJsonPayload(
        smsList: List<SmsRecordEntity>,
        callList: List<CallRecordEntity>,
        contactsList: List<ContactEntity> = emptyList()
    ): String {
        val root = JSONObject()
        root.put("app", "eSkyTell")
        root.put("version", 3)
        root.put("exportedAt", System.currentTimeMillis())

        val smsArray = JSONArray()
        smsList.forEach { sms ->
            smsArray.put(
                JSONObject().apply {
                    put("senderName", sms.senderName)
                    put("phoneNumber", sms.phoneNumber)
                    put("body", sms.body)
                    put("timestampMillis", sms.timestampMillis)
                    put("direction", sms.direction)
                    put("simSlot", sms.simSlot)
                }
            )
        }
        root.put("sms", smsArray)

        val callArray = JSONArray()
        callList.forEach { call ->
            callArray.put(
                JSONObject().apply {
                    put("contactName", call.contactName)
                    put("phoneNumber", call.phoneNumber)
                    put("timestampMillis", call.timestampMillis)
                    put("durationSeconds", call.durationSeconds)
                    put("direction", call.direction)
                    put("simSlot", call.simSlot)
                }
            )
        }
        root.put("calls", callArray)

        val contactsArray = JSONArray()
        contactsList.forEach { contact ->
            contactsArray.put(
                JSONObject().apply {
                    put("displayName", contact.displayName)
                    put("phoneNumber", contact.phoneNumber)
                    put("email", contact.email)
                    put("notes", contact.notes)
                    put("isFavorite", contact.isFavorite)
                    put("updatedAtMillis", contact.updatedAtMillis)
                }
            )
        }
        root.put("contacts", contactsArray)

        return root.toString()
    }

    suspend fun createContactsVCardFile(contactsList: List<ContactEntity>): GeneratedExportFile =
        withContext(Dispatchers.IO) {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "eSkyTell_Contacts_${stamp}.vcf"
            val outFile = File(exportDir, fileName)

            val sb = StringBuilder()
            contactsList.forEach { c ->
                sb.appendLine("BEGIN:VCARD")
                sb.appendLine("VERSION:3.0")
                sb.appendLine("FN:${c.displayName}")
                sb.appendLine("TEL;TYPE=CELL:${c.phoneNumber}")
                if (c.email.isNotBlank()) sb.appendLine("EMAIL:${c.email}")
                if (c.notes.isNotBlank()) sb.appendLine("NOTE:${c.notes.replace("\n", " ")}")
                sb.appendLine("END:VCARD")
            }
            outFile.writeText(sb.toString(), Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
            GeneratedExportFile(uri, fileName, "text/vcard", outFile.length())
        }

    suspend fun createExportFile(
        format: ExportFormat,
        smsList: List<SmsRecordEntity>,
        callList: List<CallRecordEntity>
    ): GeneratedExportFile = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "eSkyTell_${stamp}.${format.extension}"
        val outFile = File(exportDir, fileName)

        val content = when (format) {
            ExportFormat.JSON_BUNDLE -> buildRfcommJsonPayload(smsList, callList)
            ExportFormat.VCARD_CONTACTS -> buildVCardContent(smsList, callList)
            ExportFormat.HTML_REPORT -> buildHtmlReport(smsList, callList)
            ExportFormat.CSV_TABLE -> buildCsvContent(smsList, callList)
            ExportFormat.PLAIN_TEXT -> buildPlainTextContent(smsList, callList)
        }

        outFile.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
        GeneratedExportFile(
            uri = uri,
            fileName = fileName,
            mimeType = format.mimeType,
            byteSize = outFile.length()
        )
    }

    private suspend fun cacheSyncedContactsArray(
        contactsArray: JSONArray,
        sourceLabel: String,
        deviceName: String,
        markAsSyncedCache: Boolean
    ): Int {
        val (toInsert, toUpdate) = prepareContactsForBatch(
            contactsArray = contactsArray,
            sourceLabel = sourceLabel,
            deviceName = deviceName,
            markAsSyncedCache = markAsSyncedCache
        )
        dao.cacheSyncedBatch(
            smsToInsert = emptyList(),
            callsToInsert = emptyList(),
            contactsToInsert = toInsert,
            contactsToUpdate = toUpdate
        )
        return toInsert.size + toUpdate.size
    }

    private suspend fun prepareContactsForBatch(
        contactsArray: JSONArray?,
        sourceLabel: String,
        deviceName: String,
        markAsSyncedCache: Boolean
    ): Pair<List<ContactEntity>, List<ContactEntity>> {
        if (contactsArray == null || contactsArray.length() == 0) {
            return emptyList<ContactEntity>() to emptyList()
        }
        val now = System.currentTimeMillis()
        val existingByKey = dao.getAllContactsSnapshot()
            .associateBy { normalizePhoneKey(it.phoneNumber) }
            .toMutableMap()
        val toInsert = mutableListOf<ContactEntity>()
        val toUpdateMap = linkedMapOf<Long, ContactEntity>()
        val seenInsertKeys = mutableSetOf<String>()

        for (i in 0 until contactsArray.length()) {
            val obj = contactsArray.optJSONObject(i) ?: continue
            val phone = obj.optString("phoneNumber", obj.optString("phone", "")).trim()
            if (phone.isBlank()) continue
            val name = obj.optString("displayName", obj.optString("name", phone)).trim().ifBlank { phone }
            val email = obj.optString("email", "").trim()
            val notes = obj.optString("notes", "").trim()
            val favorite = obj.optBoolean("isFavorite", false)
            val updatedAt = obj.optLong("updatedAtMillis", now)
            val key = normalizePhoneKey(phone)
            val existing = existingByKey[key]
            if (existing != null) {
                val updated = existing.copy(
                    displayName = name.ifBlank { existing.displayName },
                    email = email.ifBlank { existing.email },
                    notes = notes.ifBlank { existing.notes },
                    isFavorite = favorite || existing.isFavorite,
                    updatedAtMillis = maxOf(existing.updatedAtMillis, updatedAt),
                    syncedFromBridge = markAsSyncedCache || existing.syncedFromBridge,
                    syncedDeviceName = if (markAsSyncedCache && deviceName.isNotBlank()) {
                        deviceName
                    } else {
                        existing.syncedDeviceName
                    }
                )
                toUpdateMap[existing.id] = updated
                existingByKey[key] = updated
            } else if (seenInsertKeys.add(key)) {
                toInsert.add(
                    ContactEntity(
                        displayName = name,
                        phoneNumber = phone,
                        email = email,
                        notes = notes,
                        isFavorite = favorite,
                        sourceLabel = sourceLabel,
                        updatedAtMillis = updatedAt,
                        syncedFromBridge = markAsSyncedCache,
                        syncedDeviceName = if (markAsSyncedCache) deviceName else ""
                    )
                )
            }
        }
        return toInsert to toUpdateMap.values.toList()
    }

    private suspend fun parseJsonBundle(
        json: String,
        sourceLabel: String,
        markAsSyncedCache: Boolean = false,
        syncedDeviceName: String = ""
    ): ImportResult {
        return try {
            val root = JSONObject(json)
            val now = System.currentTimeMillis()
            val smsBatch = mutableListOf<SmsRecordEntity>()
            val callBatch = mutableListOf<CallRecordEntity>()
            val seenSmsKeys = mutableSetOf<String>()

            val smsArray = root.optJSONArray("sms")
            if (smsArray != null) {
                for (i in 0 until smsArray.length()) {
                    val obj = smsArray.optJSONObject(i) ?: continue
                    val body = obj.optString("body", "").trim()
                    val phone = obj.optString("phoneNumber", obj.optString("address", "")).trim()
                    if (body.isNotBlank() || phone.isNotBlank()) {
                        val cleanPhone = phone.ifBlank { "نامشخص" }
                        val ts = obj.optLong("timestampMillis", now)
                        val dedupKey = "${normalizePhoneKey(cleanPhone)}|$body|${ts / 5000L}"
                        if (seenSmsKeys.add(dedupKey) &&
                            dao.findMatchingSmsCount(cleanPhone, body, ts) == 0
                        ) {
                            smsBatch.add(
                                SmsRecordEntity(
                                    senderName = obj.optString("senderName", cleanPhone.ifBlank { "مخاطب" }),
                                    phoneNumber = cleanPhone,
                                    body = body,
                                    timestampMillis = ts,
                                    direction = obj.optString("direction", SmsDirection.INCOMING.name),
                                    sourceLabel = sourceLabel,
                                    simSlot = obj.optInt("simSlot", 1).coerceIn(1, 2),
                                    isRead = false,
                                    isSyncedFromBridge = markAsSyncedCache,
                                    syncedAtMillis = if (markAsSyncedCache) now else 0L
                                )
                            )
                        }
                    }
                }
            }

            val callArray = root.optJSONArray("calls")
            if (callArray != null) {
                for (i in 0 until callArray.length()) {
                    val obj = callArray.optJSONObject(i) ?: continue
                    val phone = obj.optString("phoneNumber", obj.optString("number", "")).trim()
                    if (phone.isNotBlank()) {
                        callBatch.add(
                            CallRecordEntity(
                                contactName = obj.optString("contactName", phone),
                                phoneNumber = phone,
                                timestampMillis = obj.optLong("timestampMillis", now),
                                durationSeconds = obj.optInt("durationSeconds", 0),
                                direction = obj.optString("direction", CallDirection.INCOMING.name),
                                sourceLabel = sourceLabel,
                                simSlot = obj.optInt("simSlot", 1).coerceIn(1, 2)
                            )
                        )
                    }
                }
            }

            val (contactsToInsert, contactsToUpdate) = prepareContactsForBatch(
                contactsArray = root.optJSONArray("contacts"),
                sourceLabel = sourceLabel,
                deviceName = syncedDeviceName.ifBlank { sourceLabel },
                markAsSyncedCache = markAsSyncedCache
            )

            dao.cacheSyncedBatch(
                smsToInsert = smsBatch,
                callsToInsert = callBatch,
                contactsToInsert = contactsToInsert,
                contactsToUpdate = contactsToUpdate
            )
            ImportResult(
                smsAdded = smsBatch.size,
                callsAdded = callBatch.size,
                sourceName = sourceLabel,
                contactsAdded = contactsToInsert.size + contactsToUpdate.size
            )
        } catch (_: Exception) {
            ImportResult(0, 0, sourceLabel, 0)
        }
    }

    private suspend fun parseXmlBackup(xml: String, sourceLabel: String): ImportResult {
        val smsBatch = mutableListOf<SmsRecordEntity>()
        val callBatch = mutableListOf<CallRecordEntity>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name.lowercase(Locale.ROOT)) {
                        "sms" -> {
                            val address = parser.getAttributeValue(null, "address") ?: ""
                            val body = parser.getAttributeValue(null, "body") ?: ""
                            val contact = parser.getAttributeValue(null, "contact_name")
                                ?.takeUnless { it == "(Unknown)" } ?: address
                            val date = parser.getAttributeValue(null, "date")?.toLongOrNull()
                                ?: System.currentTimeMillis()
                            val type = parser.getAttributeValue(null, "type")?.toIntOrNull() ?: 1
                            val dir = when (type) {
                                2 -> SmsDirection.OUTGOING
                                3 -> SmsDirection.DRAFT
                                else -> SmsDirection.INCOMING
                            }
                            if (body.isNotBlank() || address.isNotBlank()) {
                                smsBatch.add(
                                    SmsRecordEntity(
                                        senderName = contact.ifBlank { "مخاطب" },
                                        phoneNumber = address.ifBlank { "نامشخص" },
                                        body = body,
                                        timestampMillis = date,
                                        direction = dir.name,
                                        sourceLabel = sourceLabel
                                    )
                                )
                            }
                        }
                        "call" -> {
                            val number = parser.getAttributeValue(null, "number") ?: ""
                            val contact = parser.getAttributeValue(null, "contact_name")
                                ?.takeUnless { it == "(Unknown)" } ?: number
                            val date = parser.getAttributeValue(null, "date")?.toLongOrNull()
                                ?: System.currentTimeMillis()
                            val duration = parser.getAttributeValue(null, "duration")?.toIntOrNull() ?: 0
                            val type = parser.getAttributeValue(null, "type")?.toIntOrNull() ?: 1
                            val dir = when (type) {
                                2 -> CallDirection.OUTGOING
                                3 -> CallDirection.MISSED
                                5 -> CallDirection.REJECTED
                                else -> CallDirection.INCOMING
                            }
                            if (number.isNotBlank()) {
                                callBatch.add(
                                    CallRecordEntity(
                                        contactName = contact.ifBlank { number },
                                        phoneNumber = number,
                                        timestampMillis = date,
                                        durationSeconds = duration,
                                        direction = dir.name,
                                        sourceLabel = sourceLabel
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
        }
        if (smsBatch.isNotEmpty()) dao.insertSmsList(smsBatch)
        if (callBatch.isNotEmpty()) dao.insertCallList(callBatch)
        return ImportResult(smsBatch.size, callBatch.size, sourceLabel)
    }

    private suspend fun parseVCard(vcf: String, sourceLabel: String): ImportResult {
        val contactBatch = mutableListOf<ContactEntity>()
        val cards = vcf.split("BEGIN:VCARD", ignoreCase = true)
        for (card in cards) {
            if (!card.contains("TEL", ignoreCase = true)) continue
            var name = ""
            var phone = ""
            var email = ""
            card.lines().forEach { line ->
                val clean = line.trim()
                when {
                    clean.startsWith("FN:", ignoreCase = true) ->
                        name = clean.substringAfter(":").trim()
                    clean.startsWith("TEL", ignoreCase = true) && clean.contains(":") ->
                        phone = clean.substringAfter(":").trim()
                    clean.startsWith("EMAIL", ignoreCase = true) && clean.contains(":") ->
                        email = clean.substringAfter(":").trim()
                }
            }
            if (phone.isNotBlank()) {
                val existing = dao.findContactByPhone(phone)
                if (existing == null) {
                    contactBatch.add(
                        ContactEntity(
                            displayName = name.ifBlank { phone },
                            phoneNumber = phone,
                            email = email,
                            sourceLabel = sourceLabel
                        )
                    )
                }
            }
        }
        if (contactBatch.isNotEmpty()) dao.insertContacts(contactBatch)
        return ImportResult(0, contactBatch.size, sourceLabel)
    }

    private suspend fun parseCsv(csv: String, sourceLabel: String): ImportResult {
        val smsBatch = mutableListOf<SmsRecordEntity>()
        val callBatch = mutableListOf<CallRecordEntity>()
        val lines = csv.lines().map { it.trim() }.filter { it.isNotEmpty() }
        for ((index, line) in lines.withIndex()) {
            if (index == 0 && (line.contains("Type", ignoreCase = true) || line.contains("نوع"))) {
                continue
            }
            val cols = line.split(",").map { it.trim().trim('"') }
            if (cols.size >= 3) {
                val type = cols[0].lowercase(Locale.ROOT)
                if (type.contains("call") || type.contains("تماس")) {
                    callBatch.add(
                        CallRecordEntity(
                            contactName = cols.getOrNull(1).orEmpty().ifBlank { cols.getOrNull(2).orEmpty() },
                            phoneNumber = cols.getOrNull(2).orEmpty(),
                            durationSeconds = cols.getOrNull(3)?.toIntOrNull() ?: 0,
                            direction = CallDirection.INCOMING.name,
                            sourceLabel = sourceLabel
                        )
                    )
                } else {
                    smsBatch.add(
                        SmsRecordEntity(
                            senderName = cols.getOrNull(1).orEmpty().ifBlank { "مخاطب" },
                            phoneNumber = cols.getOrNull(2).orEmpty().ifBlank { "نامشخص" },
                            body = cols.getOrNull(3).orEmpty().ifBlank { line },
                            direction = SmsDirection.INCOMING.name,
                            sourceLabel = sourceLabel
                        )
                    )
                }
            }
        }
        if (smsBatch.isNotEmpty()) dao.insertSmsList(smsBatch)
        if (callBatch.isNotEmpty()) dao.insertCallList(callBatch)
        return ImportResult(smsBatch.size, callBatch.size, sourceLabel)
    }

    private fun buildVCardContent(
        smsList: List<SmsRecordEntity>,
        callList: List<CallRecordEntity>
    ): String {
        val seenNumbers = linkedMapOf<String, String>()
        callList.forEach { call ->
            if (call.phoneNumber.isNotBlank() && call.phoneNumber != "نامشخص") {
                seenNumbers.putIfAbsent(call.phoneNumber, call.contactName)
            }
        }
        smsList.forEach { sms ->
            if (sms.phoneNumber.isNotBlank() && sms.phoneNumber != "نامشخص") {
                seenNumbers.putIfAbsent(sms.phoneNumber, sms.senderName)
            }
        }

        val sb = StringBuilder()
        seenNumbers.forEach { (phone, name) ->
            val noteSms = smsList.filter { it.phoneNumber == phone }.take(3)
                .joinToString(" | ") { it.body.replace("\n", " ") }
            sb.appendLine("BEGIN:VCARD")
            sb.appendLine("VERSION:3.0")
            sb.appendLine("FN:$name")
            sb.appendLine("TEL;TYPE=CELL:$phone")
            if (noteSms.isNotBlank()) {
                sb.appendLine("NOTE:$noteSms")
            }
            sb.appendLine("END:VCARD")
        }
        if (seenNumbers.isEmpty()) {
            sb.appendLine("BEGIN:VCARD")
            sb.appendLine("VERSION:3.0")
            sb.appendLine("FN:eSkyTell Contact")
            sb.appendLine("NOTE:No phone numbers queued")
            sb.appendLine("END:VCARD")
        }
        return sb.toString()
    }

    private fun buildCsvContent(
        smsList: List<SmsRecordEntity>,
        callList: List<CallRecordEntity>
    ): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder("\uFEFF")
        sb.appendLine("نوع,نام مخاطب,شماره تلفن,متن یا مدت(ثانیه),وضعیت,سیم‌کارت,تاریخ")
        smsList.forEach { sms ->
            val dir = SmsDirection.entries.find { it.name == sms.direction }?.labelFa ?: sms.direction
            val safeBody = sms.body.replace("\"", "\"\"").replace("\n", " ")
            sb.appendLine("\"پیامک\",\"${sms.senderName}\",\"${sms.phoneNumber}\",\"$safeBody\",\"$dir\",\"SIM ${sms.simSlot}\",\"${fmt.format(Date(sms.timestampMillis))}\"")
        }
        callList.forEach { call ->
            val dir = CallDirection.entries.find { it.name == call.direction }?.labelFa ?: call.direction
            sb.appendLine("\"تماس\",\"${call.contactName}\",\"${call.phoneNumber}\",\"${call.durationSeconds}\",\"$dir\",\"SIM ${call.simSlot}\",\"${fmt.format(Date(call.timestampMillis))}\"")
        }
        return sb.toString()
    }

    private fun buildPlainTextContent(
        smsList: List<SmsRecordEntity>,
        callList: List<CallRecordEntity>
    ): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("=== بسته ارتباطی eSkyTell ===")
        sb.appendLine("تعداد پیامک‌ها: ${smsList.size} | تعداد تماس‌ها: ${callList.size}")
        sb.appendLine()
        if (smsList.isNotEmpty()) {
            sb.appendLine("--- پیامک‌ها ---")
            smsList.forEach { sms ->
                val dir = SmsDirection.entries.find { it.name == sms.direction }?.labelFa ?: sms.direction
                sb.appendLine("[$dir | SIM ${sms.simSlot}] ${sms.senderName} (${sms.phoneNumber}) - ${fmt.format(Date(sms.timestampMillis))}")
                sb.appendLine(sms.body)
                sb.appendLine("----------------")
            }
            sb.appendLine()
        }
        if (callList.isNotEmpty()) {
            sb.appendLine("--- تماس‌ها ---")
            callList.forEach { call ->
                val dir = CallDirection.entries.find { it.name == call.direction }?.labelFa ?: call.direction
                sb.appendLine("[$dir | SIM ${call.simSlot}] ${call.contactName} (${call.phoneNumber}) | مدت: ${call.durationSeconds} ثانیه | ${fmt.format(Date(call.timestampMillis))}")
            }
        }
        return sb.toString()
    }

    private fun buildHtmlReport(
        smsList: List<SmsRecordEntity>,
        callList: List<CallRecordEntity>
    ): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        fun esc(input: String): String = input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

        val smsHtml = smsList.joinToString("\n") { sms ->
            val dir = SmsDirection.entries.find { it.name == sms.direction }?.labelFa ?: sms.direction
            """
            <div class="card">
              <div class="row">
                <strong>${esc(sms.senderName)}</strong>
                <span class="badge">${esc(dir)} • SIM ${sms.simSlot}</span>
              </div>
              <div class="meta">${esc(sms.phoneNumber)} • ${fmt.format(Date(sms.timestampMillis))}</div>
              <p class="body">${esc(sms.body)}</p>
            </div>
            """.trimIndent()
        }

        val callHtml = callList.joinToString("\n") { call ->
            val dir = CallDirection.entries.find { it.name == call.direction }?.labelFa ?: call.direction
            """
            <div class="card">
              <div class="row">
                <strong>${esc(call.contactName)}</strong>
                <span class="badge call">${esc(dir)} • SIM ${call.simSlot}</span>
              </div>
              <div class="meta">${esc(call.phoneNumber)} • مدت: ${call.durationSeconds} ثانیه • ${fmt.format(Date(call.timestampMillis))}</div>
            </div>
            """.trimIndent()
        }

        return """
            <!DOCTYPE html>
            <html lang="fa" dir="rtl">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>گزارش eSkyTell</title>
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Tahoma, sans-serif; background: #09110F; color: #F1F7F5; margin: 0; padding: 20px; }
                .container { max-width: 720px; margin: 0 auto; }
                h1 { color: #00D4AA; font-size: 22px; margin-bottom: 4px; }
                h2 { color: #F4B860; font-size: 17px; margin-top: 24px; border-bottom: 1px solid #223A34; padding-bottom: 6px; }
                .summary { color: #93ACA5; font-size: 13px; margin-bottom: 18px; }
                .card { background: #0F1B18; border: 1px solid #223A34; border-radius: 14px; padding: 14px; margin-bottom: 12px; }
                .row { display: flex; justify-content: space-between; align-items: center; }
                .badge { background: #0D3D33; color: #00D4AA; padding: 3px 10px; border-radius: 99px; font-size: 12px; }
                .badge.call { background: #3A2C12; color: #F4B860; }
                .meta { color: #93ACA5; font-size: 12px; margin-top: 6px; direction: ltr; text-align: right; }
                .body { margin: 10px 0 0 0; line-height: 1.7; font-size: 14px; }
              </style>
            </head>
            <body>
              <div class="container">
                <h1>گزارش پل ارتباطی eSkyTell</h1>
                <div class="summary">پیامک‌ها: ${smsList.size} مورد | تماس‌ها: ${callList.size} مورد</div>
                <h2>پیامک‌ها</h2>
                ${smsHtml.ifBlank { "<p class='summary'>پیامکی انتخاب نشده است.</p>" }}
                <h2>تاریخچه تماس‌ها</h2>
                ${callHtml.ifBlank { "<p class='summary'>تماسی انتخاب نشده است.</p>" }}
              </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun resolveFileName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIdx >= 0 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
        }
    }
}
