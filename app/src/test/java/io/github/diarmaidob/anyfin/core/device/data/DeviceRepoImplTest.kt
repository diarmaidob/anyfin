package io.github.diarmaidob.anyfin.core.device.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class DeviceRepoImplTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var sharedPreferences: SharedPreferences

    @MockK(relaxed = true)
    lateinit var editor: SharedPreferences.Editor

    private lateinit var deviceRepo: DeviceRepoImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        deviceRepo = DeviceRepoImpl(context)
    }

    @Test
    fun `getDeviceId returns existing id when it is already stored`() {
        val storedId = "stored-uuid-1234"
        every { sharedPreferences.getString("device_id", null) } returns storedId

        val result = deviceRepo.getDeviceId()

        assertEquals(storedId, result)
        verify(exactly = 0) { sharedPreferences.edit() }
    }

    @Test
    fun `getDeviceId generates and saves a new id when nothing is stored`() {
        every { sharedPreferences.getString("device_id", null) } returns null
        every { sharedPreferences.edit() } returns editor

        val result = deviceRepo.getDeviceId()

        assertNotEquals(null, result)
        verify { editor.putString("device_id", result) }
        verify { editor.apply() }
    }

    @Test
    fun `getDeviceId generates a unique id on every call if storage remains empty`() {
        every { sharedPreferences.getString("device_id", null) } returns null
        every { sharedPreferences.edit() } returns editor

        val firstId = deviceRepo.getDeviceId()
        val secondId = deviceRepo.getDeviceId()

        assertNotEquals(firstId, secondId)
    }

    @Test
    fun `getDeviceId uses the correct preferences key and name`() {
        every { sharedPreferences.getString("device_id", null) } returns null
        every { sharedPreferences.edit() } returns editor

        deviceRepo.getDeviceId()

        verify { context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE) }
        verify { sharedPreferences.getString("device_id", null) }
    }
}