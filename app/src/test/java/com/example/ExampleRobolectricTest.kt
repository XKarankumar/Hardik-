package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.update.AppUpdateRepository
import com.example.domain.model.BuiltInPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HARDIK BUDS", appName)
    }

    @Test
    fun `semantic version comparison works for GitHub releases`() {
        assertTrue(AppUpdateRepository.isRemoteNewer("1.0.0", "1.0.1"))
        assertTrue(AppUpdateRepository.isRemoteNewer("1.0.0", "1.2.0"))
        assertFalse(AppUpdateRepository.isRemoteNewer("1.0.0", "1.0.0"))
        assertFalse(AppUpdateRepository.isRemoteNewer("2.1.0", "2.0.9"))
    }

    @Test
    fun `all built in eq presets have 10 bands`() {
        assertTrue(BuiltInPresets.defaults.isNotEmpty())
        BuiltInPresets.defaults.forEach { preset ->
            assertEquals("Preset ${preset.name} must have 10 bands", 10, preset.bandsDb.size)
        }
    }
}
