package ng.name.gojodev.picnym.util

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeMediaTest {
    @Test
    fun keepsACompletedNonEmptyRecording() {
        val file = Files.createTempFile("picnym-voice", ".m4a").toFile()
        file.writeBytes(byteArrayOf(1, 2, 3))

        assertEquals(file, finalizeVoiceRecording(file, stopSucceeded = true, discard = false))
        assertTrue(file.exists())

        file.delete()
    }

    @Test
    fun deletesRecordingWhenMediaRecorderFailsToStop() {
        val file = Files.createTempFile("picnym-voice", ".m4a").toFile()
        file.writeBytes(byteArrayOf(1, 2, 3))

        assertNull(finalizeVoiceRecording(file, stopSucceeded = false, discard = false))
        assertFalse(file.exists())
    }

    @Test
    fun rejectsAnEmptyRecordingEvenAfterASuccessfulStop() {
        val file = Files.createTempFile("picnym-voice", ".m4a").toFile()

        assertNull(finalizeVoiceRecording(file, stopSucceeded = true, discard = false))
        assertFalse(file.exists())
    }

    @Test
    fun discardsACompletedRecordingWhenRequested() {
        val file = Files.createTempFile("picnym-voice", ".m4a").toFile()
        file.writeBytes(byteArrayOf(1, 2, 3))

        assertNull(finalizeVoiceRecording(file, stopSucceeded = true, discard = true))
        assertFalse(file.exists())
    }
}
