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
