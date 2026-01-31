package io.github.diarmaidob.anyfin.core.device.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.diarmaidob.anyfin.core.device.DeviceRepo
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepoImpl @Inject constructor(
    @ApplicationContext context: Context
) : DeviceRepo {

    private companion object {
        private const val PREFS_NAME = "device_prefs"
        private const val DEVICE_ID_KEY = "device_id"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getDeviceId(): String {
        return sharedPreferences.getString(DEVICE_ID_KEY, null) ?: generateAndSaveDeviceId()
    }

    private fun generateAndSaveDeviceId(): String {
        val newDeviceId = UUID.randomUUID().toString()
        sharedPreferences.edit {
            putString(DEVICE_ID_KEY, newDeviceId)
        }
        return newDeviceId
    }
}
