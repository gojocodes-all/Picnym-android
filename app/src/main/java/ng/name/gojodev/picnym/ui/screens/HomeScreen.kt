package ng.name.gojodev.picnym.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import kotlinx.coroutines.launch
import ng.name.gojodev.picnym.BuildConfig
import ng.name.gojodev.picnym.data.AccountData
import ng.name.gojodev.picnym.data.Inbox
import ng.name.gojodev.picnym.data.PicnymApi
import ng.name.gojodev.picnym.ui.ErrorCard
import ng.name.gojodev.picnym.ui.LoadingScreen
import ng.name.gojodev.picnym.ui.MainBottomBar
import ng.name.gojodev.picnym.ui.PicnymTopBar
import ng.name.gojodev.picnym.ui.PremiumBadge
import ng.name.gojodev.picnym.ui.ProfileAvatar
import ng.name.gojodev.picnym.ui.StatusPill
import ng.name.gojodev.picnym.util.shareText

@Composable
fun HomeScreen(
    api: PicnymApi,
    onAccount: () -> Unit,
    onDashboard: (String) -> Unit,
    onPublicInbox: (String) -> Unit,
    onThemeToggle: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var account by remember { mutableStateOf<AccountData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var createOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Inbox?>(null) }

    fun refresh() {
        scope.launch {
            loading = account == null
            error = null
            try { account = api.account() } catch (t: Throwable) { error = t.message }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            PicnymTopBar(actions = {
                IconButton(onClick = onThemeToggle) { Icon(Icons.Outlined.DarkMode, "Toggle theme") }
                IconButton(onClick = { refresh() }) { Icon(Icons.Outlined.Refresh, "Refresh") }
                IconButton(onClick = onAccount) { Icon(Icons.Outlined.Person, "Account") }
            })
        },
        bottomBar = { MainBottomBar("home", onHome = {}, onAccount = onAccount) },
        floatingActionButton = { FloatingActionButton(onClick = { createOpen = true }) { Text("+") } }
    ) { padding ->
        when {
            loading -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { LoadingScreen("Loading your PICNYM…") }
            error != null && account == null -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { ErrorCard(error ?: "Could not load account.", ::refresh) }
            else -> {
                val data = account ?: return@Scaffold
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    ProfileAvatar(data.profile, 64.dp)
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(data.profile.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                        }
                                        Text("@${data.profile.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (data.profile.premium) PremiumBadge()
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Stat("${data.stats.inboxes}", "Inboxes")
                                    Stat("${data.stats.totalMessages}", "Messages")
                                    Stat("${data.stats.friends}", "Friends")
                                }
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Your anonymous inboxes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                                Text("Native Android dashboard", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { createOpen = true }) { Text("Create") }
                        }
                    }
                    if (data.inboxes.isEmpty()) {
                        item { Card { Text("No inboxes yet. Create your first PICNYM link.", Modifier.padding(24.dp)) } }
                    } else {
                        items(data.inboxes, key = { it.id }) { inbox ->
                            Card {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("/${inbox.slug}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                            Text("${inbox.displayName} · ${inbox.messageCount} messages", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        StatusPill(if (inbox.settings.paused) "Paused" else if (inbox.settings.friendsOnly) "Friends only" else if (inbox.settings.registeredOnly) "Accounts only" else "Live")
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { onDashboard(inbox.slug) }, modifier = Modifier.weight(1f)) { Text("Open") }
                                        OutlinedButton(onClick = { onPublicInbox(inbox.slug) }) { Icon(Icons.Outlined.Send, null) }
                                        OutlinedButton(onClick = { shareText(context, "${BuildConfig.SITE_URL}/u/${inbox.slug}") }) { Icon(Icons.Outlined.Share, null) }
                                        OutlinedButton(onClick = { deleteTarget = inbox }) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.padding(32.dp)) }
                }
            }
        }
    }

    if (createOpen) CreateInboxDialog(
        defaultName = account?.profile?.displayName.orEmpty(),
        onDismiss = { createOpen = false },
        onCreate = { name, handle ->
            scope.launch {
                try {
                    val inbox = api.createInbox(name, handle)
                    createOpen = false
                    refresh()
                    onDashboard(inbox.slug)
                } catch (t: Throwable) { error = t.message }
            }
        }
    )

    deleteTarget?.let { inbox ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete /${inbox.slug}?") },
            text = { Text("This permanently deletes the inbox, its messages, polls and uploaded media.") },
            confirmButton = { TextButton(onClick = { scope.launch { runCatching { api.deleteInbox(inbox.slug) }; deleteTarget = null; refresh() } }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CreateInboxDialog(defaultName: String, onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember(defaultName) { mutableStateOf(defaultName) }
    var handle by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create anonymous inbox") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Display name") }, singleLine = true)
                OutlinedTextField(handle, { handle = it.lowercase().replace(" ", "-") }, label = { Text("Link name") }, prefix = { Text("/u/") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank() && handle.isNotBlank()) onCreate(name, handle) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
