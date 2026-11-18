package ng.name.gojodev.picnym.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputRulesTest {
    @Test
    fun normalizesHumanInputToServerCompatibleHandle() {
        assertEquals("gojo-codes", normalizeInboxHandle("  Gojo Codes!!  "))
        assertEquals("picnym-voice-polls", normalizeInboxHandle("PICNYM / Voice + Polls"))
    }

    @Test
    fun keepsHandleInsideUiLimitWithoutTrailingSeparator() {
        val result = normalizeInboxHandle("a very long PICNYM handle that keeps going")
        assertTrue(result.length <= 28)
        assertFalse(result.endsWith('-'))
        assertTrue(isValidInboxHandle(result))
    }

    @Test
    fun rejectsEmptyOrMalformedStoredHandles() {
        assertFalse(isValidInboxHandle(""))
        assertFalse(isValidInboxHandle("-picnym"))
        assertFalse(isValidInboxHandle("picnym-"))
        assertTrue(isValidInboxHandle("picnym-v3"))
    }
}
