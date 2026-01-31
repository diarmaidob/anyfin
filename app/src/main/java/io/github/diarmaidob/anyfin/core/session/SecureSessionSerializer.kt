package io.github.diarmaidob.anyfin.core.session

import android.util.Log
import androidx.datastore.core.Serializer
import com.google.crypto.tink.Aead
import com.squareup.moshi.Moshi
import io.github.diarmaidob.anyfin.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class SecureSessionSerializer @Inject constructor(
    private val aead: Aead,
    moshi: Moshi
) : Serializer<StoredSession> {

    private companion object {
        private const val TAG = "SecureSessionSerializer"
    }

    private val adapter = moshi.adapter(StoredSession::class.java)

    override val defaultValue: StoredSession = StoredSession()

    override suspend fun readFrom(input: InputStream): StoredSession {
        return withContext(Dispatchers.IO) {
            try {
                val encryptedBytes = input.readBytes()
                if (encryptedBytes.isEmpty()) return@withContext defaultValue

                val decryptedBytes = aead.decrypt(encryptedBytes, null)

                val jsonString = String(decryptedBytes, Charsets.UTF_8)
                adapter.fromJson(jsonString) ?: defaultValue
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to read/decrypt session store", e)
                }
                defaultValue
            }
        }
    }

    override suspend fun writeTo(t: StoredSession, output: OutputStream) {
        withContext(Dispatchers.IO) {
            val jsonString = adapter.toJson(t)
            val plainBytes = jsonString.toByteArray(Charsets.UTF_8)

            val encryptedBytes = aead.encrypt(plainBytes, null)

            output.write(encryptedBytes)
        }
    }
}