package ng.name.gojodev.picnym.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ng.name.gojodev.picnym.BuildConfig
import ng.name.gojodev.picnym.data.InboxMessages
import ng.name.gojodev.picnym.data.Message
import ng.name.gojodev.picnym.data.PicnymApi
import ng.name.gojodev.picnym.data.Poll
import ng.name.gojodev.picnym.ui.AudioPlayerButton
import ng.name.gojodev.picnym.ui.ErrorCard
import ng.name.gojodev.picnym.ui.LoadingScreen
import ng.name.gojodev.picnym.ui.PicnymTopBar
import ng.name.gojodev.picnym.ui.PollSummary
import ng.name.gojodev.picnym.ui.ProfileAvatar
import ng.name.gojodev.picnym.util.ShareCardRenderer
import ng.name.gojodev.picnym.util.shareText

@Composable
fun DashboardScreen(
    api: PicnymApi,
    slug: String,
    onBack: () -> Unit,
    onOpenPoll: (String) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf<InboxMessages?>(null) }
    var polls by remember { mutableStateOf<List<Poll>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("all") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    var replyTo by remember { mutableStateOf<Message?>(null) }
    var reportTarget by remember { mutableStateOf<Message?>(null) }

    fun refresh() {
        scope.launch {
            loading = data == null
            error = null
            try {
                data = api.messages(slug)
                polls = api.polls(slug)
            } catch (t: Throwable) { error = t.message }
            finally { loading = false }
        }
    }
    LaunchedEffect(slug) { refresh() }

    Scaffold(
        topBar = {
            PicnymTopBar("/${slug}", onBack = onBack, actions = {
                IconButton(onClick = { shareText(context, "${BuildConfig.SITE_URL}/u/$slug") }) { Icon(Icons.Outlined.Share, "Share inbox") }
                IconButton(onClick = { refresh() }) { Icon(Icons.Outlined.Refresh, "Refresh") }
            })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Inbox") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Polls") })
            }
            when {
                loading -> Column(Modifier.padding(24.dp)) { LoadingScreen("Loading inbox…") }
                error != null && data == null -> Column(Modifier.padding(24.dp)) { ErrorCard(error ?: "Could not load inbox.", ::refresh) }
                tab == 0 -> {
                    val messages = data?.messages.orEmpty().filter { message ->
                        val typeOk = kind == "all" || message.kind == kind
                        val queryOk = query.isBlank() || listOf(message.text, message.reply, message.poll?.question.orEmpty()).any { it.contains(query, true) }
                        val favoriteOk = !favoritesOnly || message.favorite
                        val archiveOk = if (showArchived) message.archived else !message.archived
                        typeOk && queryOk && favoriteOk && archiveOk
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search messages and replies") }, singleLine = true)
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("all", "text", "image", "voice", "poll").forEach { value ->
                                    FilterChip(selected = kind == value, onClick = { kind = value }, label = { Text(value.replaceFirstChar { it.uppercase() }) })
                                }
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(favoritesOnly, { favoritesOnly = it }); Text("Favorites")
                                Checkbox(showArchived, { showArchived = it }); Text("Archived")
                            }
                        }
                        if (messages.isEmpty()) item { Card { Text("No messages match these filters.", Modifier.padding(22.dp)) } }
                        items(messages, key = { it.id }) { message ->
                            NativeMessageCard(
                                message = message,
                                inboxName = data?.inbox?.displayName.orEmpty(),
                                onReply = { replyTo = message },
                                onShare = { ShareCardRenderer.share(context, message, data?.inbox?.displayName.orEmpty()) },
                                onFavorite = { scope.launch { runCatching { api.setMessageState(message.id, favorite = !message.favorite) }; refresh() } },
                                onArchive = { scope.launch { runCatching { api.setMessageState(message.id, archived = !message.archived) }; refresh() } },
                                onPublish = { scope.launch { runCatching { api.setMessageState(message.id, isPublic = !message.isPublic) }; refresh() } },
                                onBlock = { scope.launch { runCatching { api.blockSender(message.id) }; refresh() } },
                                onDelete = { scope.launch { runCatching { api.deleteMessage(message.id) }; refresh() } },
                                onReport = { reportTarget = message },
                                onOpenProfile = onOpenProfile
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (polls.isEmpty()) item { Card { Text("No polls received yet.", Modifier.padding(22.dp)) } }
                        items(polls, key = { it.id }) { poll ->
                            Card(onClick = { onOpenPoll(poll.slug) }) { Column(Modifier.padding(18.dp)) { PollSummary(poll) } }
                        }
                    }
                }
            }
        }
    }

    replyTo?.let { message ->
        var reply by remember(message.id) { mutableStateOf(message.reply) }
        AlertDialog(
            onDismissRequest = { replyTo = null },
            title = { Text("Reply") },
            text = { OutlinedTextField(reply, { reply = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Your reply") }, minLines = 3) },
            confirmButton = { Button(onClick = { scope.launch { try { api.reply(message.id, reply); replyTo = null; refresh() } catch (t: Throwable) { error = t.message } } }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { replyTo = null }) { Text("Cancel") } }
        )
    }

    reportTarget?.let { message ->
        var reason by remember(message.id) { mutableStateOf("Harassment or bullying") }
        var details by remember(message.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("Report message") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(reason, { reason = it }, label = { Text("Reason") })
                OutlinedTextField(details, { details = it }, label = { Text("Details") }, minLines = 3)
            } },
            confirmButton = { Button(onClick = { scope.launch { try { api.reportMessage(message.id, reason, details); reportTarget = null } catch (t: Throwable) { error = t.message } } }) { Text("Report") } },
            dismissButton = { TextButton(onClick = { reportTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun NativeMessageCard(
    message: Message,
    inboxName: String,
    onReply: () -> Unit,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onArchive: () -> Unit,
    onPublish: () -> Unit,
    onBlock: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (message.senderProfile != null) {
                    ProfileAvatar(message.senderProfile, 34.dp)
                    TextButton(onClick = { onOpenProfile(message.senderProfile.username) }) { Text("@${message.senderProfile.username}") }
                } else Text("ANONYMOUS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text(message.createdAt.take(16).replace("T", " "), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(if (message.favorite) "Remove favorite" else "Favorite") }, onClick = { menu = false; onFavorite() })
                    DropdownMenuItem(text = { Text(if (message.archived) "Unarchive" else "Archive") }, onClick = { menu = false; onArchive() })
                    if (message.reply.isNotBlank()) DropdownMenuItem(text = { Text(if (message.isPublic) "Remove from profile" else "Publish answer") }, onClick = { menu = false; onPublish() })
                    DropdownMenuItem(text = { Text("Block sender") }, onClick = { menu = false; onBlock() })
                    DropdownMenuItem(text = { Text("Report") }, onClick = { menu = false; onReport() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
            if (message.text.isNotBlank()) Text(message.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            message.imageUrl?.let { AsyncImage(model = it, contentDescription = "Anonymous image", modifier = Modifier.fillMaxWidth()) }
            message.voiceUrl?.let { AudioPlayerButton(it) }
            message.poll?.let { PollSummary(it) }
            if (message.reply.isNotBlank()) {
                HorizontalDivider()
                Text("Your reply", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(message.reply, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onReply, modifier = Modifier.weight(1f)) { Text("Reply") }
                TextButton(onClick = onShare) { Text("Share card") }
            }
        }
    }
}
