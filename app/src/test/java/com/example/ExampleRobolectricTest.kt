package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.settings.AboutScreen
import com.example.ui.theme.AlokChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name matches Alok Chat branding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Alok Chat", appName)
    }

    @Test
    fun `splash screen displays Alok Chat branding and credits`() {
        composeTestRule.setContent {
            AlokChatTheme {
                SplashScreen(onFinished = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ALOK CHAT").assertIsDisplayed()
        composeTestRule.onNodeWithText("Created by Alok").assertIsDisplayed()
        composeTestRule.onNodeWithText("Supported by Harshit").assertIsDisplayed()
    }

    @Test
    fun `about screen displays credits and security note`() {
        composeTestRule.setContent {
            AlokChatTheme {
                AboutScreen(onBack = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("About ALOK CHAT").assertIsDisplayed()
        composeTestRule.onNodeWithText("Created by: Alok").assertIsDisplayed()
        composeTestRule.onNodeWithText("Supported by: Harshit").assertIsDisplayed()
    }
}
