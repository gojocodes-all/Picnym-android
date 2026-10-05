package ng.name.gojodev.picnym.util

import java.util.Locale

private val nonHandleCharacters = Regex("[^a-z0-9]+")
private val validInboxHandle = Regex("^[a-z0-9](?:[a-z0-9-]{0,26}[a-z0-9])?$")

fun normalizeInboxHandle(value: String): String = value
    .lowercase(Locale.ROOT)
    .replace(nonHandleCharacters, "-")
    .trim('-')
    .take(28)
    .trimEnd('-')

fun isValidInboxHandle(value: String): Boolean = validInboxHandle.matches(value)

fun isValidPasswordForAuth(password: String, creatingAccount: Boolean): Boolean =
    if (creatingAccount) password.length >= 8 else password.isNotBlank()

fun publicMessageValidationError(
    kind: String,
    text: String = "",
    hasImage: Boolean = false,
    hasVoice: Boolean = false,
    question: String = "",
    options: List<String> = emptyList()
): String? = when (kind) {
    "text" -> if (text.isBlank()) "Write a message before sending." else null
    "image" -> if (!hasImage) "Choose an image before sending." else null
    "voice" -> if (!hasVoice) "Record a voice note before sending." else null
    "poll" -> when {
        question.isBlank() -> "Add a poll question before sending."
        options.count { it.isNotBlank() } < 2 -> "Add at least two poll options before sending."
        else -> null
    }
    else -> "Choose a supported message type."
}
