package ng.name.gojodev.picnym.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import ng.name.gojodev.picnym.data.AccountSettings
import ng.name.gojodev.picnym.data.AuthRepository
import ng.name.gojodev.picnym.data.FriendBuckets
import ng.name.gojodev.picnym.data.Inbox
import ng.name.gojodev.picnym.data.InboxSettings
import ng.name.gojodev.picnym.data.PicnymApi
import ng.name.gojodev.picnym.data.Profile
import ng.name.gojodev.picnym.data.SessionStore
import ng.name.gojodev.picnym.ui.ErrorCard
import ng.name.gojodev.picnym.ui.LoadingScreen
import ng.name.gojodev.picnym.ui.MainBottomBar
import ng.name.gojodev.picnym.ui.PicnymTopBar
import ng.name.gojodev.picnym.ui.PremiumBadge
import ng.name.gojodev.picnym.ui.ProfileAvatar
import ng.name.gojodev.picnym.ui.StatusPill
import ng.name.gojodev.picnym.ui.theme.AppThemeState
import ng.name.gojodev.picnym.ui.theme.ThemeMode
import ng.name.gojodev.picnym.util.copyUriToCache
import ng.name.gojodev.picnym.util.clearGoogleCredentialState
import ng.name.gojodev.picnym.util.openUrl

@Composable
fun AccountScreen(
    api: PicnymApi,
    auth: AuthRepository,
    store: SessionStore,
    onHome: () -> Unit,
    onDashboard: (String) -> Unit,
    onProfile: (String) -> Unit,
    onSignedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf<AccountData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Profile", "Inboxes", "Friends", "Settings", "Billing")

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
        topBar = { PicnymTopBar("Account", onBack = onHome) },
        bottomBar = { MainBottomBar("account", onHome = onHome, onAccount = {}) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> Column(Modifier.padding(24.dp)) { LoadingScreen("Loading account center…") }
                error != null && account == null -> Column(Modifier.padding(24.dp)) { ErrorCard(error ?: "Could not load account.", ::refresh) }
                else -> {
                    val data = account ?: return@Column
                    Card(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            ProfileAvatar(data.profile, 70.dp)
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(data.profile.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                    if (data.profile.premium) PremiumBadge()
                                }
                                Text("@${data.profile.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${data.stats.inboxes} inboxes · ${data.stats.totalMessages} messages · ${data.stats.friends} friends")
                            }
                        }
                    }
                    ScrollableTabRow(selectedTabIndex = tab) {
                        tabs.forEachIndexed { index, label -> Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) }) }
                    }
                    when (tab) {
                        0 -> ProfileTab(data, api, onProfile, ::refresh)
                        1 -> InboxesTab(data, api, onDashboard, ::refresh)
                        2 -> FriendsTab(api, onProfile)
                        3 -> SettingsTab(data, api, auth, store, onSignedOut, ::refresh)
                        else -> BillingTab(data)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTab(data: AccountData, api: PicnymApi, onProfile: (String) -> Unit, refresh: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var name by remember(data.profile.displayName) { mutableStateOf(data.profile.displayName) }
    var username by remember(data.profile.username) { mutableStateOf(data.profile.username) }
    var bio by remember(data.profile.bio) { mutableStateOf(data.profile.bio) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            try {
                val (file, mime) = copyUriToCache(context, uri, "avatar")
                api.uploadAvatar(file, mime)
                refresh()
            } catch (t: Throwable) { error = t.message }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Profile picture", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileAvatar(data.profile, 86.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { picker.launch("image/*") }) { Text("Choose picture") }
                            if (data.profile.avatarUrl != null) OutlinedButton(onClick = { scope.launch { runCatching { api.removeAvatar() }; refresh() } }) { Text("Remove") }
                        }
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Profile details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Display name") })
                    OutlinedTextField(username, { username = it.lowercase().replace("@", "").replace(" ", "_") }, modifier = Modifier.fillMaxWidth(), label = { Text("Username") }, prefix = { Text("@") })
                    OutlinedTextField(bio, { bio = it.take(240) }, modifier = Modifier.fillMaxWidth(), label = { Text("Bio") }, minLines = 3)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = { scope.launch { try { api.updateProfile(name, username, bio); error = null; refresh() } catch (t: Throwable) { error = t.message } } }) { Text("Save profile") }
                    OutlinedButton(onClick = { onProfile(data.profile.username) }) { Text("View public profile") }
                }
            }
        }
    }
}

@Composable
private fun InboxesTab(data: AccountData, api: PicnymApi, onDashboard: (String) -> Unit, refresh: () -> Unit) {
    val scope = rememberCoroutineScope()
    var settingsTarget by remember { mutableStateOf<Inbox?>(null) }
    var deleteTarget by remember { mutableStateOf<Inbox?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Manage inboxes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
        items(data.inboxes, key = { it.id }) { inbox ->
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("/${inbox.slug}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text("${inbox.messageCount} messages", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill(if (inbox.settings.paused) "Paused" else "Live")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onDashboard(inbox.slug) }) { Text("Open") }
                        OutlinedButton(onClick = { settingsTarget = inbox }) { Text("Settings") }
                        OutlinedButton(onClick = { deleteTarget = inbox }) { Text("Delete") }
                    }
                }
            }
        }
    }
    settingsTarget?.let { inbox -> InboxSettingsDialog(inbox, api, { settingsTarget = null }, { settingsTarget = null; refresh() }) }
    deleteTarget?.let { inbox ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null }, title = { Text("Delete /${inbox.slug}?") },
            text = { Text("Messages, polls and uploaded media in this inbox will be permanently deleted.") },
            confirmButton = { Button(onClick = { scope.launch { runCatching { api.deleteInbox(inbox.slug) }; deleteTarget = null; refresh() } }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun InboxSettingsDialog(inbox: Inbox, api: PicnymApi, dismiss: () -> Unit, saved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(inbox.displayName) }
    var slug by remember { mutableStateOf(inbox.slug) }
    var settings by remember { mutableStateOf(inbox.settings) }
    var hidden by remember { mutableStateOf(inbox.settings.hiddenWords.joinToString("\n")) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Inbox settings") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Display name") })
                OutlinedTextField(slug, { slug = it.lowercase().replace(" ", "-") }, label = { Text("Link name") })
                SettingSwitch("Pause link", settings.paused) { settings = settings.copy(paused = it) }
                SettingSwitch("Registered users only", settings.registeredOnly) { settings = settings.copy(registeredOnly = it) }
                SettingSwitch("Friends only", settings.friendsOnly) { settings = settings.copy(friendsOnly = it) }
                SettingSwitch("Allow images", settings.allowImages) { settings = settings.copy(allowImages = it) }
                SettingSwitch("Allow voice notes", settings.allowVoice) { settings = settings.copy(allowVoice = it) }
                SettingSwitch("Allow polls", settings.allowPolls) { settings = settings.copy(allowPolls = it) }
                OutlinedTextField(hidden, { hidden = it }, label = { Text("Hidden words, one per line") }, minLines = 3)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { Button(onClick = { scope.launch {
            try {
                val renamed = api.updateInbox(inbox.slug, slug, name)
                api.updateInboxSettings(renamed.slug, settings.copy(hiddenWords = hidden.lines().map { it.trim() }.filter { it.isNotBlank() }))
                saved()
            } catch (t: Throwable) { error = t.message }
        } }) { Text("Save") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FriendsTab(api: PicnymApi, onProfile: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var friends by remember { mutableStateOf<FriendBuckets?>(null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() { scope.launch { try { friends = api.friends(); error = null } catch (t: Throwable) { error = t.message } } }
    LaunchedEffect(Unit) { load() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Friends", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it.removePrefix("@") }, modifier = Modifier.weight(1f), label = { Text("Search @username") }, singleLine = true)
                Button(onClick = { scope.launch { try { results = api.searchUsers(query) } catch (t: Throwable) { error = t.message } } }) { Text("Search") }
            }
        }
        if (results.isNotEmpty()) {
            item { Text("Search results", fontWeight = FontWeight.Bold) }
            items(results, key = { it.userId }) { profile -> FriendRow(profile, "Add", onAction = { scope.launch { runCatching { api.requestFriend(profile.username) }; results = results.filterNot { it.userId == profile.userId }; load() } }, onOpen = { onProfile(profile.username) }) }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        friends?.incoming?.takeIf { it.isNotEmpty() }?.let { list ->
            item { Text("Requests", fontWeight = FontWeight.Bold) }
            items(list, key = { it.id }) { entry -> FriendRow(entry.profile, "Accept", onAction = { scope.launch { runCatching { api.acceptFriend(entry.profile.userId) }; load() } }, onOpen = { onProfile(entry.profile.username) }) }
        }
        friends?.outgoing?.takeIf { it.isNotEmpty() }?.let { list ->
            item { Text("Sent requests", fontWeight = FontWeight.Bold) }
            items(list, key = { it.id }) { entry -> FriendRow(entry.profile, "Cancel", onAction = { scope.launch { runCatching { api.removeFriend(entry.profile.userId) }; load() } }, onOpen = { onProfile(entry.profile.username) }) }
        }
        item { Text("Your friends", fontWeight = FontWeight.Bold) }
        val accepted = friends?.accepted.orEmpty()
        if (accepted.isEmpty()) item { Card { Text("No friends yet.", Modifier.padding(20.dp)) } }
        items(accepted, key = { it.id }) { entry -> FriendRow(entry.profile, "Remove", onAction = { scope.launch { runCatching { api.removeFriend(entry.profile.userId) }; load() } }, onOpen = { onProfile(entry.profile.username) }) }
    }
}

@Composable
private fun FriendRow(profile: Profile, action: String, onAction: () -> Unit, onOpen: () -> Unit) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileAvatar(profile, 46.dp)
            Column(Modifier.weight(1f)) { Text(profile.displayName, fontWeight = FontWeight.Bold); TextButton(onClick = onOpen) { Text("@${profile.username}") } }
            OutlinedButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun SettingsTab(
    data: AccountData,
    api: PicnymApi,
    auth: AuthRepository,
    store: SessionStore,
    onSignedOut: () -> Unit,
    refresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var settings by remember(data.settings) { mutableStateOf(data.settings) }
    var deleteOpen by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Help & safety", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Review how reporting, blocking, hidden words and privacy work.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { openUrl(context, BuildConfig.SITE_URL + "/safety") }) { Text("Safety") }
                        OutlinedButton(onClick = { openUrl(context, BuildConfig.SITE_URL + "/privacy") }) { Text("Privacy") }
                        OutlinedButton(onClick = { openUrl(context, BuildConfig.SITE_URL + "/terms") }) { Text("Terms") }
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(selected = settings.theme == mode.key, onClick = {
                                settings = settings.copy(theme = mode.key)
                                AppThemeState.mode = mode
                                scope.launch { store.saveTheme(mode.key); runCatching { api.updateAccountSettings(settings) }; refresh() }
                            }, label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) })
                        }
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Privacy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    SettingSwitch("Discoverable profile", settings.discoverable) { settings = settings.copy(discoverable = it) }
                    SettingSwitch("Allow friend requests", settings.allowFriendRequests) { settings = settings.copy(allowFriendRequests = it) }
                    SettingSwitch("Show activity status", settings.showActivity) { settings = settings.copy(showActivity = it) }
                    Button(onClick = { scope.launch { try { settings = api.updateAccountSettings(settings); refresh() } catch (_: Throwable) {} } }) { Text("Save privacy settings") }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Account controls", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(data.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { scope.launch { auth.signOut(); runCatching { clearGoogleCredentialState(context) }; onSignedOut() } }) { Text("Sign out") }
                    HorizontalDivider()
                    OutlinedButton(onClick = { deleteOpen = true }) { Text("Delete PICNYM account") }
                }
            }
        }
    }
    if (deleteOpen) DeleteAccountDialog(
        dismiss = { deleteOpen = false },
        confirm = { scope.launch { try { api.deleteAccount(); runCatching { clearGoogleCredentialState(context) }; onSignedOut() } catch (_: Throwable) {} } }
    )
}

@Composable
private fun DeleteAccountDialog(dismiss: () -> Unit, confirm: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = dismiss, title = { Text("Delete your account?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("This permanently deletes your profile, every owned inbox, its messages and media. Type DELETE to continue.")
            OutlinedTextField(text, { text = it }, label = { Text("DELETE") })
        } },
        confirmButton = { Button(enabled = text == "DELETE", onClick = confirm) { Text("Delete forever") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } }
    )
}

@Composable
private fun BillingTab(data: AccountData) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Billing", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Card {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PICNYM Free", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("₦0", style = MaterialTheme.typography.headlineMedium)
                Text("Up to 5 inboxes · text · images · voice notes · polls · profiles · friends · safety controls")
                if (data.billing.plan == "free") StatusPill("Current plan")
            }
        }
        Card {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumBadge()
                Text("₦${data.billing.premiumPriceNgn} / month", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Up to 30 inboxes · 30-day non-identifying analytics · premium badge · advanced exports and creator tools")
                Text("Premium never reveals hidden sender identity, exact location, IP address or device fingerprint.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(enabled = data.billing.paymentsEnabled, onClick = { openUrl(context, BuildConfig.SITE_URL + "/features") }) {
                    Text(if (data.billing.paymentsEnabled) "Manage subscription" else "Payments coming soon")
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, changed: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = changed)
    }
}
