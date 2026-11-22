package com.example

import com.example.data.model.AppSettingsState
import com.example.data.model.DEFAULT_COUNTRY_CODES
import com.example.data.model.FontSizePreference
import com.example.data.model.PrivacyOption
import com.example.data.model.ThemePreference
import com.example.data.model.UserProfile
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `deterministicChatId is symmetric regardless of argument order`() {
        val idA = FormatUtils.deterministicChatId("uid_alpha", "uid_beta")
        val idB = FormatUtils.deterministicChatId("uid_beta", "uid_alpha")
        assertEquals("uid_alpha_uid_beta", idA)
        assertEquals(idA, idB)
    }

    @Test
    fun `normalizePhoneE164 formats local and international numbers cleanly`() {
        assertEquals("+919876543210", FormatUtils.normalizePhoneE164("98765 43210", "+91"))
        assertEquals("+919876543210", FormatUtils.normalizePhoneE164("09876543210", "+91"))
        assertEquals("+14155550199", FormatUtils.normalizePhoneE164("+1 (415) 555-0199", "+91"))
        assertEquals("+447911123456", FormatUtils.normalizePhoneE164("00447911123456", "+91"))
    }

    @Test
    fun `formatDuration and formatFileSize return expected human strings`() {
        assertEquals("02:05", FormatUtils.formatDurationSeconds(125))
        assertEquals("00:09", FormatUtils.formatDurationMillis(9500))
        assertEquals("512 B", FormatUtils.formatFileSize(512))
        assertEquals("4 KB", FormatUtils.formatFileSize(4096))
        assertEquals("2.0 MB", FormatUtils.formatFileSize(2L * 1024L * 1024L))
    }

    @Test
    fun `default country is India +91 and privacy defaults are initialized`() {
        assertEquals("+91", DEFAULT_COUNTRY_CODES.first().dialCode)
        assertEquals("India", DEFAULT_COUNTRY_CODES.first().name)

        val user = UserProfile(email = "alok@example.com", username = "alok_chat")
        assertEquals("alok", user.bestDisplayName())
        assertEquals("Alok Kumar", user.copy(displayName = "Alok Kumar").bestDisplayName())
        assertEquals(PrivacyOption.EVERYONE.name, user.privacyPhoto)
        assertTrue(user.readReceiptsEnabled)

        val settings = AppSettingsState()
        assertEquals(ThemePreference.SYSTEM, settings.themePreference)
        assertEquals(FontSizePreference.MEDIUM, settings.fontSizePreference)
    }
}
