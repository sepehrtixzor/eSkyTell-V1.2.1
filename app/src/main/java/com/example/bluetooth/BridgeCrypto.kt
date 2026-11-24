package com.example.bluetooth

import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BridgeCrypto {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_BITS = 128

    fun generatePairingPin(): String {
        val code = 100_000 + SecureRandom().nextInt(900_000)
        return code.toString()
    }

    private fun deriveKey(pin: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("eSkyTell::$pin".toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }

    fun encryptEnvelope(plainJson: String, pin: String): String {
        return try {
            val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(pin), GCMParameterSpec(TAG_BITS, iv))
            val encrypted = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
            JSONObject().apply {
                put("enc", "AES-256-GCM")
                put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            }.toString()
        } catch (_: Exception) {
            plainJson
        }
    }

    fun decryptEnvelopeIfNeeded(rawPayload: String, pin: String): String {
        return try {
            val root = JSONObject(rawPayload)
            if (root.optString("enc") == "AES-256-GCM") {
                val iv = Base64.decode(root.getString("iv"), Base64.NO_WRAP)
                val cipherBytes = Base64.decode(root.getString("data"), Base64.NO_WRAP)
                val cipher = Cipher.getInstance(ALGORITHM)
                cipher.init(Cipher.DECRYPT_MODE, deriveKey(pin), GCMParameterSpec(TAG_BITS, iv))
                String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
            } else {
                rawPayload
            }
        } catch (_: Exception) {
            rawPayload
        }
    }
}
