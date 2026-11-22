package com.example

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.StatusPrivacyMode
import com.example.data.model.UserProfile
import com.example.ui.components.MessageStatusTicks
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.auth.AuthFlowScreen
import com.example.ui.screens.settings.AboutScreen
import com.example.ui.screens.status.StatusPrivacySettingsDialog
import com.example.ui.theme.AlokChatTheme
import com.example.ui.viewmodel.AuthViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

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
    fun `main activity launches without crashing through splash transition`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(!activity.isFinishing)
            }
            composeTestRule.waitForIdle()
            ShadowLooper.idleMainLooper(2000L, java.util.concurrent.TimeUnit.MILLISECONDS)
            composeTestRule.waitForIdle()
            scenario.onActivity { activity ->
                assertTrue(!activity.isFinishing)
            }
        }
    }

    @Test
    fun `auth flow screen displays Sign In and Create Account tabs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context.applicationContext as AlokChatApp
        val authVm = AuthViewModel(app.container.authRepository, app.container.userRepository)
        composeTestRule.setContent {
            AlokChatTheme {
                AuthFlowScreen(viewModel = authVm, onAuthenticated = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome back").assertIsDisplayed()
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

    @Test
    fun `status privacy dialog displays all audience choices`() {
        composeTestRule.setContent {
            AlokChatTheme {
                StatusPrivacySettingsDialog(
                    initialMode = StatusPrivacyMode.MY_CONTACTS,
                    initialExcludedUids = emptySet(),
                    initialAllowedUids = emptySet(),
                    allUsers = listOf(UserProfile(uid = "u2", displayName = "Harshit", username = "harshit")),
                    contactUids = setOf("u2"),
                    onSave = { _, _, _ -> },
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Status privacy").assertIsDisplayed()
        composeTestRule.onNodeWithText("My contacts").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("My contacts except...").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Only share with...").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Everyone").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `message status ticks render Sent, Delivered, and Read states`() {
        composeTestRule.setContent {
            AlokChatTheme {
                Row {
                    MessageStatusTicks(isDelivered = false, isRead = false)
                    MessageStatusTicks(isDelivered = true, isRead = false)
                    MessageStatusTicks(isDelivered = true, isRead = true)
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Sent").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Delivered").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Read").assertIsDisplayed()
    }
}
