package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.nlu.NluResult
import com.example.nlu.OfflineIntentEngine
import com.example.system.SystemActionExecutor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aura IA", appName)
    }

    @Test
    fun `offline intent engine parses play store download intent`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val executor = SystemActionExecutor(context)
        val engine = OfflineIntentEngine(executor)

        val result = engine.processCommand(
            rawQuery = "selecione play store e baixe este app whatsapp",
            assistantName = "Aura"
        )

        assertTrue(result is NluResult.Executed)
        val exec = result as NluResult.Executed
        assertEquals("PLAY_STORE_DOWNLOAD", exec.intentType)
    }

    @Test
    fun `offline intent engine evaluates simple math`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val executor = SystemActionExecutor(context)
        val engine = OfflineIntentEngine(executor)

        val result = engine.processCommand(
            rawQuery = "quanto é 25 * 4",
            assistantName = "Aura"
        )

        assertTrue(result is NluResult.Executed)
        val exec = result as NluResult.Executed
        assertEquals("MATH_CALC", exec.intentType)
        assertTrue(exec.assistantResponse.contains("100"))
    }
}
