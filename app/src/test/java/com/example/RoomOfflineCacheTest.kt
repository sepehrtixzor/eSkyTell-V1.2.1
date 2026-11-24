package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CallRecordEntity
import com.example.data.local.ContactEntity
import com.example.data.local.SmsRecordEntity
import com.example.data.local.TransferDatabase
import com.example.data.model.BridgeCommandType
import com.example.data.model.CallDirection
import com.example.data.model.SmsDirection
import com.example.data.repository.TransferRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomOfflineCacheTest {

    private lateinit var context: Context
    private lateinit var database: TransferDatabase
    private lateinit var repository: TransferRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, TransferDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TransferRepository(context, database.transferDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `rfcomm json bundle caches synced sms calls and contacts offline and deduplicates repeats`() = runTest {
        val ts = 1_760_000_000_000L
        val smsToSend = listOf(
            SmsRecordEntity(
                senderName = "Sara Ahmadi",
                phoneNumber = "+989121112233",
                body = "Code: 849201",
                timestampMillis = ts,
                direction = SmsDirection.INCOMING.name,
                simSlot = 1
            )
        )
        val callsToSend = listOf(
            CallRecordEntity(
                contactName = "Sara Ahmadi",
                phoneNumber = "+989121112233",
                timestampMillis = ts,
                durationSeconds = 42,
                direction = CallDirection.INCOMING.name,
                simSlot = 1
            )
        )
        val contactsToSend = listOf(
            ContactEntity(
                displayName = "Sara Ahmadi",
                phoneNumber = "+989121112233",
                email = "sara@example.com",
                isFavorite = true
            ),
            ContactEntity(
                displayName = "Reza Karimi",
                phoneNumber = "09123334455"
            )
        )

        val payload = repository.buildRfcommJsonPayload(smsToSend, callsToSend, contactsToSend)

        val firstImport = repository.importBridgePayload(payload, "iPhone 16 Pro")
        assertEquals(1, firstImport.importResult.smsAdded)
        assertEquals(1, firstImport.importResult.callsAdded)
        assertEquals(2, firstImport.importResult.contactsAdded)

        val cachedSms = repository.smsRecords.first()
        val cachedContacts = repository.contacts.first()
        assertEquals(1, cachedSms.size)
        assertTrue(cachedSms.first().isSyncedFromBridge)
        assertEquals(1, repository.cachedBridgeSmsCount.first())
        assertEquals(2, cachedContacts.size)
        assertTrue(cachedContacts.all { it.syncedFromBridge && it.syncedDeviceName == "iPhone 16 Pro" })
        assertEquals(2, repository.cachedBridgeContactsCount.first())

        // Re-importing the exact same bundle should not duplicate SMS or contact rows.
        val secondImport = repository.importBridgePayload(payload, "iPhone 16 Pro")
        assertEquals(0, secondImport.importResult.smsAdded)
        assertEquals(1, repository.smsRecords.first().size)
        assertEquals(2, repository.contacts.first().size)
    }

    @Test
    fun `bridge commands SMS_RELAY and CONTACT_SYNC cache records into Room cleanly`() = runTest {
        val smsCommand = JSONObject().apply {
            put("bridgeType", BridgeCommandType.SMS_RELAY.name)
            put("contactName", "Nima")
            put("phoneNumber", "09120001122")
            put("messageBody", "Synced while bridge connected")
            put("simSlot", 2)
            put("sourceDeviceName", "Primary SIM Phone")
            put("timestampMillis", 1_760_000_123_000L)
        }.toString()

        val contactSyncCommand = JSONObject().apply {
            put("bridgeType", BridgeCommandType.CONTACT_SYNC.name)
            put("sourceDeviceName", "Primary SIM Phone")
            put(
                "contacts",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("displayName", "Nima")
                            put("phoneNumber", "+989120001122")
                            put("notes", "VIP client")
                        }
                    )
                }
            )
        }.toString()

        val smsRes = repository.importBridgePayload(smsCommand, "Primary SIM Phone")
        val smsRetryRes = repository.importBridgePayload(smsCommand, "Primary SIM Phone")
        val contactRes = repository.importBridgePayload(contactSyncCommand, "Primary SIM Phone")

        assertEquals(1, smsRes.importResult.smsAdded)
        assertEquals(0, smsRetryRes.importResult.smsAdded)
        assertEquals(1, contactRes.importResult.contactsAdded)

        assertEquals(1, repository.cachedBridgeSmsCount.first())
        assertEquals(1, repository.cachedBridgeContactsCount.first())

        val storedContact = repository.contacts.first().single()
        assertEquals("Nima", storedContact.displayName)
        assertTrue(storedContact.syncedFromBridge)
        assertEquals("Primary SIM Phone", storedContact.syncedDeviceName)
    }
}
