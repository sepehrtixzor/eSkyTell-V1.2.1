package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.AnnouncementCategory
import com.example.data.model.AnnouncementModule
import com.example.data.model.AppGlobalConfig
import com.example.data.model.PaymentMethodType
import com.example.data.model.PaymentRequest
import com.example.data.model.PaymentRequestStatus
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserProfile
import com.example.data.model.formatTomanAmount
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudAdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "sepehrtixzor1@gmail.com"
        private const val CONFIG_DOC_ID = "global"
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("کاربر باید ابتدا وارد حساب گوگل شود.")
    }

    fun isCurrentUserAdmin(user: FirebaseUser? = auth.currentUser): Boolean {
        val email = user?.email?.lowercase()?.trim()
        return email == BOOTSTRAPPED_ADMIN_EMAIL
    }

    fun observeGlobalConfig(): Flow<AppGlobalConfig> = flow {
        val uid = requireUserId()
        if (uid.isBlank()) throw IllegalStateException("شناسه کاربری نامعتبر است.")
        val docRef = db.collection("app_config").document(CONFIG_DOC_ID)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            AppGlobalConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: AppGlobalConfig()
                    } else {
                        AppGlobalConfig()
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    emit(AppGlobalConfig())
                }
        )
    }

    fun observePublishedAnnouncements(): Flow<List<AnnouncementModule>> = flow {
        val uid = requireUserId()
        val path = "announcements"
        emitAll(
            db.collection(path)
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AnnouncementModule::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeAllAnnouncementsForAdmin(): Flow<List<AnnouncementModule>> = flow {
        val uid = requireUserId()
        val path = "announcements"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AnnouncementModule::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeCurrentUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "عدم تطابق شناسه کاربر" }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    emit(null)
                }
        )
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeUserPaymentRequests(userId: String): Flow<List<PaymentRequest>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "عدم تطابق شناسه کاربر" }
        val path = "payment_requests"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PaymentRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeAllPaymentRequestsForAdmin(): Flow<List<PaymentRequest>> = flow {
        val uid = requireUserId()
        val path = "payment_requests"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PaymentRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<PaymentRequest> { it.status != PaymentRequestStatus.PENDING.name }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val docPath = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "کاربر تغییر کرده است." }
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            val safeName = (user.displayName?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@") ?: "کاربر eSkyTell").take(100)
            val safeEmail = (user.email ?: "").take(200)
            val safePhoto = (user.photoUrl?.toString() ?: "").take(500)

            if (!snap.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "email" to safeEmail,
                    "displayName" to safeName,
                    "sim1Label" to "SIM 1 • خط کاری",
                    "sim2Label" to "SIM 2 • خط شخصی",
                    "hasActiveSubscription" to false,
                    "subscriptionTier" to SubscriptionTier.FREE.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safePhoto.isNotBlank()) {
                    payload["photoUrl"] = safePhoto
                }
                docRef.set(payload).await()
            } else {
                val updateMap = mutableMapOf<String, Any>(
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safePhoto.isNotBlank()) {
                    updateMap["photoUrl"] = safePhoto
                }
                docRef.update(updateMap).await()
            }

            // If bootstrapped admin signs in and global config isn't created yet, seed defaults
            if (isCurrentUserAdmin(user)) {
                val configRef = db.collection("app_config").document(CONFIG_DOC_ID)
                val configSnap = configRef.get().await()
                if (!configSnap.exists()) {
                    saveGlobalConfig(AppGlobalConfig())
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSimLabels(sim1Label: String, sim2Label: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val clean1 = sim1Label.trim().ifBlank { "SIM 1" }.take(40)
            val clean2 = sim2Label.trim().ifBlank { "SIM 2" }.take(40)
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "sim1Label" to clean1,
                    "sim2Label" to clean2,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveGlobalConfig(config: AppGlobalConfig): Result<Unit> {
        val docPath = "app_config/$CONFIG_DOC_ID"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("app_config").document(CONFIG_DOC_ID)
            val monthlyToman = config.monthlyPriceToman.coerceIn(0L, 1_000_000_000L)
            val yearlyToman = config.yearlyPriceToman.coerceIn(0L, 1_000_000_000L)
            val monthlyLabel = config.monthlyPriceLabel.trim()
                .ifBlank { formatTomanAmount(monthlyToman, "/ ماه") }
                .take(80)
            val yearlyLabel = config.yearlyPriceLabel.trim()
                .ifBlank { formatTomanAmount(yearlyToman, "/ سال") }
                .take(80)
            val cardsPayload = config.bankCards
                .filter { it.cardNumber.isNotBlank() }
                .take(20)
                .map { card ->
                    mapOf(
                        "bankName" to card.bankName.trim().take(60),
                        "cardNumber" to card.cardNumber.trim().take(32),
                        "holderName" to card.holderName.trim().take(80)
                    )
                }
            val walletsPayload = config.cryptoWallets
                .filter { it.address.isNotBlank() }
                .take(20)
                .map { wallet ->
                    mapOf(
                        "networkName" to wallet.networkName.trim().take(60),
                        "address" to wallet.address.trim().take(200)
                    )
                }

            val payload = mutableMapOf<String, Any>(
                "subscriptionRequired" to config.subscriptionRequired,
                "monthlyPriceToman" to monthlyToman,
                "yearlyPriceToman" to yearlyToman,
                "monthlyPriceLabel" to monthlyLabel,
                "yearlyPriceLabel" to yearlyLabel,
                "freeTrialDays" to config.freeTrialDays.coerceIn(0L, 365L),
                "paywallMessage" to config.paywallMessage.trim().take(400),
                "paymentUrl" to config.paymentUrl.trim().take(500),
                "bankCards" to cardsPayload,
                "cryptoWallets" to walletsPayload,
                "latestVersionName" to config.latestVersionName.trim().ifBlank { "1.0.0" }.take(32),
                "latestVersionCode" to config.latestVersionCode.coerceIn(1L, 100000L),
                "updateNotes" to config.updateNotes.trim().take(1000),
                "updateUrl" to config.updateUrl.trim().take(500),
                "forceUpdate" to config.forceUpdate,
                "enableDualSim" to config.enableDualSim,
                "enableQuickTemplates" to config.enableQuickTemplates,
                "enableQuietHours" to config.enableQuietHours,
                "enableBleGattBridge" to config.enableBleGattBridge,
                "updatedBy" to uid.take(128),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(payload).await()
            } else {
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveAnnouncement(
        existingId: String?,
        title: String,
        summary: String,
        category: AnnouncementCategory,
        actionLabel: String,
        actionValue: String,
        published: Boolean
    ): Result<Unit> {
        val cleanTitle = title.trim().take(120)
        val cleanSummary = summary.trim().take(600)
        require(cleanTitle.isNotEmpty() && cleanSummary.isNotEmpty()) {
            "عنوان و توضیحات نمی‌توانند خالی باشند."
        }

        var docPath = "announcements/${existingId ?: "new"}"
        return try {
            val uid = requireUserId()
            val col = db.collection("announcements")
            if (existingId.isNullOrBlank()) {
                val docRef = col.document()
                docPath = docRef.path
                val payload = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "summary" to cleanSummary,
                    "category" to category.name,
                    "published" to published,
                    "createdBy" to uid.take(128),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (actionLabel.isNotBlank()) payload["actionLabel"] = actionLabel.trim().take(40)
                if (actionValue.isNotBlank()) payload["actionValue"] = actionValue.trim().take(500)
                docRef.set(payload).await()
            } else {
                val docRef = col.document(existingId)
                docPath = docRef.path
                val payload = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "summary" to cleanSummary,
                    "category" to category.name,
                    "actionLabel" to actionLabel.trim().take(40),
                    "actionValue" to actionValue.trim().take(500),
                    "published" to published,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleAnnouncementPublished(id: String, published: Boolean): Result<Unit> {
        val docPath = "announcements/$id"
        return try {
            requireUserId()
            require(id.isNotBlank()) { "شناسه نامعتبر است." }
            db.collection("announcements").document(id).update(
                mapOf(
                    "published" to published,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(id: String): Result<Unit> {
        val docPath = "announcements/$id"
        return try {
            requireUserId()
            require(id.isNotBlank()) { "شناسه نامعتبر است." }
            db.collection("announcements").document(id).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setUserSubscription(
        targetUserId: String,
        hasActiveSubscription: Boolean,
        tier: SubscriptionTier
    ): Result<Unit> {
        val docPath = "users/$targetUserId"
        return try {
            requireUserId()
            require(targetUserId.isNotBlank()) { "شناسه کاربر نامعتبر است." }
            val expiresAtMillis = computeExpiryMillis(hasActiveSubscription, tier)
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "hasActiveSubscription" to hasActiveSubscription,
                    "subscriptionTier" to tier.name,
                    "subscriptionExpiresAtMillis" to expiresAtMillis,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendUserRenewalNotification(
        targetUserId: String,
        message: String
    ): Result<Unit> {
        val docPath = "users/$targetUserId"
        return try {
            requireUserId()
            require(targetUserId.isNotBlank()) { "شناسه کاربر نامعتبر است." }
            val cleanMessage = message.trim()
                .ifBlank { "اشتراک eSkyTell Pro شما رو به اتمام است. برای جلوگیری از قطع پل بلوتوث، اشتراک خود را تمدید کنید." }
                .take(300)
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "renewalNoticeMessage" to cleanMessage,
                    "renewalNoticeAtMillis" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private fun computeExpiryMillis(hasActive: Boolean, tier: SubscriptionTier): Long {
        if (!hasActive) return 0L
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        return when (tier) {
            SubscriptionTier.MONTHLY -> now + 30L * dayMs
            SubscriptionTier.YEARLY -> now + 365L * dayMs
            SubscriptionTier.LIFETIME, SubscriptionTier.FREE -> 0L
        }
    }

    suspend fun submitPaymentRequest(
        requestedTier: SubscriptionTier,
        amountToman: Long,
        paymentMethod: PaymentMethodType,
        targetAccountLabel: String,
        trackingCode: String,
        receiptImageBase64: String,
        userNote: String
    ): Result<Unit> {
        var docPath = "payment_requests/new"
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("کاربر وارد حساب نشده است.")
            val uid = user.uid
            val cleanTracking = trackingCode.trim().take(200)
            val cleanReceipt = receiptImageBase64.trim().take(450_000)
            require(cleanTracking.isNotEmpty() || cleanReceipt.isNotEmpty()) {
                "لطفاً کد پیگیری یا تصویر فیش واریزی را وارد کنید."
            }
            val safeTier = if (requestedTier == SubscriptionTier.YEARLY) {
                SubscriptionTier.YEARLY.name
            } else {
                SubscriptionTier.MONTHLY.name
            }
            val safeName = (user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "کاربر eSkyTell").take(100)
            val safeEmail = (user.email ?: "").take(200)

            val docRef = db.collection("payment_requests").document()
            docPath = docRef.path

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "userEmail" to safeEmail,
                "userDisplayName" to safeName,
                "requestedTier" to safeTier,
                "amountToman" to amountToman.coerceIn(0L, 1_000_000_000L),
                "paymentMethod" to paymentMethod.name,
                "status" to PaymentRequestStatus.PENDING.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (targetAccountLabel.isNotBlank()) {
                payload["targetAccountLabel"] = targetAccountLabel.trim().take(200)
            }
            if (cleanTracking.isNotEmpty()) {
                payload["trackingCode"] = cleanTracking
            }
            if (cleanReceipt.isNotEmpty()) {
                payload["receiptImageBase64"] = cleanReceipt
            }
            if (userNote.isNotBlank()) {
                payload["userNote"] = userNote.trim().take(400)
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun reviewPaymentRequest(
        request: PaymentRequest,
        approve: Boolean,
        adminNote: String = ""
    ): Result<Unit> {
        val docPath = "payment_requests/${request.id}"
        return try {
            val adminUid = requireUserId()
            require(request.id.isNotBlank() && request.userId.isNotBlank()) {
                "اطلاعات درخواست پرداخت نامعتبر است."
            }
            val newStatus = if (approve) {
                PaymentRequestStatus.APPROVED.name
            } else {
                PaymentRequestStatus.REJECTED.name
            }
            val cleanNote = adminNote.trim().take(400)
            val requestRef = db.collection("payment_requests").document(request.id)

            if (approve) {
                val targetTierEnum = if (request.requestedTier == SubscriptionTier.YEARLY.name) {
                    SubscriptionTier.YEARLY
                } else {
                    SubscriptionTier.MONTHLY
                }
                val expiresAtMillis = computeExpiryMillis(hasActive = true, tier = targetTierEnum)
                val userRef = db.collection("users").document(request.userId)
                val batch = db.batch()
                batch.update(
                    requestRef,
                    mapOf(
                        "status" to newStatus,
                        "adminNote" to cleanNote,
                        "reviewedBy" to adminUid.take(128),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    userRef,
                    mapOf(
                        "hasActiveSubscription" to true,
                        "subscriptionTier" to targetTierEnum.name,
                        "subscriptionExpiresAtMillis" to expiresAtMillis,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
            } else {
                requestRef.update(
                    mapOf(
                        "status" to newStatus,
                        "adminNote" to cleanNote,
                        "reviewedBy" to adminUid.take(128),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
