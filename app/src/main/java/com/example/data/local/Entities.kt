package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_records")
data class SmsRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderName: String,
    val phoneNumber: String,
    val body: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val direction: String = "INCOMING",
    val isSelected: Boolean = true,
    val sourceLabel: String = "دستی",
    val simSlot: Int = 1,
    val isRead: Boolean = true,
    val isSyncedFromBridge: Boolean = false,
    val syncedAtMillis: Long = 0L
)

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val phoneNumber: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val direction: String = "INCOMING",
    val isSelected: Boolean = true,
    val sourceLabel: String = "دستی",
    val simSlot: Int = 1
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val phoneNumber: String,
    val email: String = "",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val syncedToSystem: Boolean = false,
    val sourceLabel: String = "برنامه",
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val syncedFromBridge: Boolean = false,
    val syncedDeviceName: String = ""
)

@Entity(tableName = "transfer_history")
data class TransferHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetDeviceName: String,
    val targetAddress: String,
    val targetPlatform: String,
    val channelMethod: String,
    val exportFormat: String,
    val smsCount: Int,
    val callCount: Int,
    val bytesTransferred: Long,
    val status: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
