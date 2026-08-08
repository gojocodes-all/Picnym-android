package ng.name.gojodev.picnym.ui.screens

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ng.name.gojodev.picnym.BuildConfig
import ng.name.gojodev.picnym.data.AuthRepository
import ng.name.gojodev.picnym.data.Message
import ng.name.gojodev.picnym.data.PicnymApi
import ng.name.gojodev.picnym.data.Poll
import ng.name.gojodev.picnym.data.PublicProfilePage
import ng.name.gojodev.picnym.ui.AudioPlayerButton
import ng.name.gojodev.picnym.ui.ErrorCard
import ng.name.gojodev.picnym.ui.LoadingScreen
import ng.name.gojodev.picnym.ui.PicnymTopBar
import ng.name.gojodev.picnym.ui.PollSummary
import ng.name.gojodev.picnym.ui.PremiumBadge
import ng.name.gojodev.picnym.ui.ProfileAvatar
import ng.name.gojodev.picnym.util.shareText

@Composable
fun PublicProfileScreen(api: PicnymApi, auth: AuthRepository, username: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf<PublicProfilePage?>(null) }
    var signedIn by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var requested by remember { mutableStateOf(false) }

    fun load() { scope.launch {
        loading = true; error = null
        try { page = api.publicProfile(username); signedIn = auth.hasSession() } catch (t: Throwable) { error = t.message }
        finally { loading = false }
    } }
    LaunchedEffect(username) { load() }

    Scaffold(topBar = { PicnymTopBar("Profile", onBack = onBack) }) { padding ->
        when {
            loading -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { LoadingScreen("Loading profile…") }
            error != null && page == null -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { ErrorCard(error ?: "Profile unavailable.", ::load) }
            else -> {
                val data = page ?: return@Scaffold
                LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Card {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ProfileAvatar(data.profile, 86.dp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(data.profile.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                                    if (data.profile.premium) PremiumBadge()
                                }
                                Text("@${data.profile.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (data.profile.bio.isNotBlank()) Text(data.profile.bio)
                                Text("${data.friendCount} friends · ${data.publicAnswerCount} public answers")
                                if (signedIn) Button(enabled = !requested, onClick = { scope.launch {
                                    try { api.requestFriend(data.profile.username); requested = true } catch (t: Throwable) { error = t.message }
                                } }) { Text(if (requested) "Request sent" else "Add friend") }
                            }
                        }
                    }
                    item { Text("Public answers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
                    if (data.posts.isEmpty()) item { Card { Text("No public answers yet.", Modifier.padding(20.dp)) } }
                    items(data.posts, key = { it.id }) { PublicAnswerCard(it) }
                }
            }
        }
    }
}

@Composable
private fun PublicAnswerCard(message: Message) {
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ANONYMOUS MESSAGE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (message.text.isNotBlank()) Text(message.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            message.imageUrl?.let { AsyncImage(model = it, contentDescription = "Shared anonymous image", modifier = Modifier.fillMaxWidth()) }
            message.voiceUrl?.let { AudioPlayerButton(it) }
            message.poll?.let { PollSummary(it) }
            if (message.reply.isNotBlank()) {
                Text("REPLY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text(message.reply, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PollScreen(api: PicnymApi, slug: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var poll by remember { mutableStateOf<Poll?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var voted by remember { mutableStateOf(false) }
    val clientId = remember {
        "android:" + Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
    }

    fun load() { scope.launch {
        loading = true; error = null
        try { poll = api.poll(slug) } catch (t: Throwable) { error = t.message }
        finally { loading = false }
    } }
    LaunchedEffect(slug) { load() }

    Scaffold(topBar = { PicnymTopBar("Anonymous poll", onBack = onBack) }) { padding ->
        when {
            loading -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { LoadingScreen("Loading poll…") }
            error != null && poll == null -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { ErrorCard(error ?: "Poll unavailable.", ::load) }
            else -> {
                val data = poll ?: return@Scaffold
                Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Card {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(data.question, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                            if (voted) PollSummary(data) else data.options.forEach { option ->
                                OutlinedButton(
                                    onClick = { scope.launch {
                                        try { poll = api.vote(slug, option.id, clientId); voted = true; error = null }
                                        catch (t: Throwable) { error = t.message }
                                    } },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(option.text) }
                            }
                            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            Text("${data.totalVotes} votes · identities are not shown", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(onClick = { shareText(context, "${BuildConfig.SITE_URL}/poll/$slug") }, modifier = Modifier.fillMaxWidth()) { Text("Share poll") }
                }
            }
        }
    }
}
