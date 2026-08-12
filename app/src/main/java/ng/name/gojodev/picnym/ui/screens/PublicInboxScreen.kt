package ng.name.gojodev.picnym.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ng.name.gojodev.picnym.data.AuthRepository
import ng.name.gojodev.picnym.data.Inbox
import ng.name.gojodev.picnym.data.PicnymApi
import ng.name.gojodev.picnym.ui.ErrorCard
import ng.name.gojodev.picnym.ui.LoadingScreen
import ng.name.gojodev.picnym.ui.PicnymTopBar
import ng.name.gojodev.picnym.ui.ProfileAvatar
import ng.name.gojodev.picnym.util.NativeVoiceRecorder
import ng.name.gojodev.picnym.util.copyUriToCache
import java.io.File

private val inboxPrompts = listOf(
    "What is something I do that people remember?",
    "What should I do more often?",
    "What is your first impression of me?",
    "Tell me an opinion you think I need to hear.",
    "What is one thing you have always wanted to ask me?"
)

@Composable
fun PublicInboxScreen(
    api: PicnymApi,
    auth: AuthRepository,
    slug: String,
    initialPrompt: String = "",
    onBack: () -> Unit,
    onProfile: (String) -> Unit,
    onPollCreated: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val recorder = remember { NativeVoiceRecorder(context) }
    var inbox by remember { mutableStateOf<Inbox?>(null) }
    var signedIn by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf("text") }
    var text by remember(initialPrompt) { mutableStateOf(initialPrompt) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var imageMime by remember { mutableStateOf("image/jpeg") }
    var voiceFile by remember { mutableStateOf<File?>(null) }
    var recording by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var reveal by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runCatching { copyUriToCache(context, uri, "image") }.onSuccess {
            imageFile = it.first; imageMime = it.second
        }.onFailure { error = it.message }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            runCatching { recorder.start() }.onSuccess { recording = true; info = "Recording…" }.onFailure { error = it.message }
        } else error = "Microphone permission is needed to record a voice note."
    }

    DisposableEffect(Unit) { onDispose { recorder.stop(discard = true) } }

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                inbox = api.publicInbox(slug)
                signedIn = auth.hasSession()
            } catch (t: Throwable) { error = t.message }
            finally { loading = false }
        }
    }
    LaunchedEffect(slug) { load() }

    fun resetComposer() {
        text = ""; imageFile = null; voiceFile = null; question = ""; options.clear(); options.addAll(listOf("", "")); reveal = false
    }

    fun send() {
        val box = inbox ?: return
        if (box.settings.paused) { error = "This inbox is paused."; return }
        sending = true; error = null; info = null
        scope.launch {
            try {
                val result = when (kind) {
                    "text" -> api.sendText(slug, text, reveal)
                    "image" -> api.sendImage(slug, imageFile ?: error("Choose an image."), imageMime, text, reveal)
                    "voice" -> api.sendVoice(slug, voiceFile ?: error("Record a voice note first."), "audio/mp4", reveal)
                    else -> api.sendPoll(slug, question, options.map { it.trim() }.filter { it.isNotBlank() }, reveal)
                }
                info = "Sent successfully."
                resetComposer()
                result.poll?.let { onPollCreated(it.slug) }
            } catch (t: Throwable) { error = t.message ?: "Could not send." }
            finally { sending = false }
        }
    }

    Scaffold(topBar = { PicnymTopBar("Send anonymously", onBack = onBack) }) { padding ->
        when {
            loading -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { LoadingScreen("Opening anonymous link…") }
            error != null && inbox == null -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { ErrorCard(error ?: "Inbox unavailable.", ::load) }
            else -> {
                val box = inbox ?: return@Scaffold
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileAvatar(box.profile, 76.dp)
                            Text(box.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                            box.profile?.let { profile ->
                                OutlinedButton(onClick = { onProfile(profile.username) }) { Text("@${profile.username}") }
                            }
                            Text("Send something through this PICNYM link. Your profile stays hidden unless you explicitly reveal it below.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (box.settings.paused || box.settings.registeredOnly || box.settings.friendsOnly) {
                        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
                            Text(
                                when {
                                    box.settings.paused -> "This inbox is paused and is not accepting messages."
                                    box.settings.friendsOnly -> "This inbox accepts messages only from signed-in PICNYM friends."
                                    else -> "You need to be signed into PICNYM to send here."
                                }, Modifier.padding(16.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("text", "image", "voice", "poll").forEach { value ->
                            val enabled = when (value) {
                                "image" -> box.settings.allowImages
                                "voice" -> box.settings.allowVoice
                                "poll" -> box.settings.allowPolls
                                else -> true
                            }
                            FilterChip(selected = kind == value, enabled = enabled, onClick = { kind = value }, shape = MaterialTheme.shapes.extraSmall, label = { Text(value.replaceFirstChar { it.uppercase() }) })
                        }
                    }

                    if (kind == "text") {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("Need a starting point?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                inboxPrompts.forEach { prompt ->
                                    AssistChip(onClick = { text = prompt }, shape = MaterialTheme.shapes.extraSmall, label = { Text(prompt) })
                                }
                            }
                        }
                    }

                    when (kind) {
                        "text" -> OutlinedTextField(text, { text = it.take(1200) }, modifier = Modifier.fillMaxWidth(), label = { Text("Anonymous message") }, minLines = 5)
                        "image" -> {
                            OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text(if (imageFile == null) "Choose image" else "Image ready: ${imageFile?.name}") }
                            OutlinedTextField(text, { text = it.take(500) }, modifier = Modifier.fillMaxWidth(), label = { Text("Caption (optional)") }, minLines = 2)
                        }
                        "voice" -> {
                            Button(onClick = {
                                if (recording) {
                                    voiceFile = recorder.stop(); recording = false; info = "Voice note ready."
                                } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }) { Text(if (recording) "Stop recording" else if (voiceFile != null) "Record again" else "Start recording") }
                            voiceFile?.let { Text("Ready: ${it.name}", color = MaterialTheme.colorScheme.primary) }
                        }
                        else -> {
                            OutlinedTextField(question, { question = it.take(180) }, modifier = Modifier.fillMaxWidth(), label = { Text("Poll question") })
                            options.forEachIndexed { index, value ->
                                OutlinedTextField(value, { options[index] = it.take(80) }, modifier = Modifier.fillMaxWidth(), label = { Text("Option ${index + 1}") })
                            }
                            if (options.size < 8) OutlinedButton(onClick = { options.add("") }) { Text("+ Add option") }
                        }
                    }

                    if (signedIn) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = reveal, onCheckedChange = { reveal = it })
                            Column {
                                Text("Reveal my PICNYM profile with this message", fontWeight = FontWeight.Bold)
                                Text("Optional. Leave this off to stay anonymous.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    info?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
                    Button(
                        enabled = !sending && !box.settings.paused && (!box.settings.registeredOnly || signedIn) && (!box.settings.friendsOnly || signedIn),
                        onClick = ::send,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (sending) "Sending…" else "Send ${if (kind == "text") "anonymously" else kind}") }
                }
            }
        }
    }
}
