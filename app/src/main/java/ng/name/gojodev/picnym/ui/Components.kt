package ng.name.gojodev.picnym.ui

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ng.name.gojodev.picnym.data.Poll
import ng.name.gojodev.picnym.data.Profile

@Composable
fun PicnymMark(modifier: Modifier = Modifier, size: Dp = 42.dp) {
    Row(modifier = modifier.size(size).clip(RoundedCornerShape(3.dp))) {
        Box(Modifier.weight(1f).height(size).background(Color(0xFF12264B)))
        Box(Modifier.weight(1f).height(size).background(Color(0xFFB9C9B2)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PicnymTopBar(title: String = "PICNYM", onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PicnymMark(size = 30.dp)
                Text(title, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
        },
        actions = { actions() }
    )
}

@Composable
fun ProfileAvatar(profile: Profile?, size: Dp = 58.dp) {
    val url = profile?.avatarUrl
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = "${profile.displayName} profile picture",
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Text((profile?.displayName ?: "P").take(1).uppercase(), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondary)
        }
    }
}

@Composable
fun LoadingScreen(label: String = "Loading PICNYM…") {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator()
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error)
            if (onRetry != null) Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun PremiumBadge() {
    AssistChip(onClick = {}, label = { Text("PICNYM PREMIUM", fontWeight = FontWeight.Black) })
}

@Composable
fun StatusPill(text: String) {
    AssistChip(onClick = {}, label = { Text(text, maxLines = 1) })
}

@Composable
fun AudioPlayerButton(url: String) {
    val player = remember(url) { MediaPlayer() }
    var prepared by remember(url) { mutableStateOf(false) }
    var playing by remember(url) { mutableStateOf(false) }
    var failed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        runCatching {
            withContext(Dispatchers.IO) {
                player.setDataSource(url)
                player.prepare()
            }
            prepared = true
            player.setOnCompletionListener { playing = false }
        }.onFailure { failed = true }
    }
    DisposableEffect(player) { onDispose { runCatching { player.release() } } }

    Button(
        enabled = prepared && !failed,
        onClick = {
            if (player.isPlaying) { player.pause(); playing = false }
            else { player.start(); playing = true }
        }
    ) {
        Icon(if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text(if (failed) "Audio unavailable" else if (playing) "Pause voice note" else "Play voice note")
    }
}

@Composable
fun PollSummary(poll: Poll) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(poll.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        poll.options.forEach { option ->
            val fraction = if (poll.totalVotes > 0) option.votes.toFloat() / poll.totalVotes else 0f
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(option.text)
                    Text("${(fraction * 100).toInt()}%", fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            }
        }
        Text("${poll.totalVotes} votes", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MainBottomBar(current: String, onHome: () -> Unit, onAccount: () -> Unit) {
    NavigationBar {
        NavigationBarItem(selected = current == "home", onClick = onHome, icon = { Icon(Icons.Outlined.Home, null) }, label = { Text("Home") })
        NavigationBarItem(selected = current == "account", onClick = onAccount, icon = { Icon(Icons.Outlined.AccountCircle, null) }, label = { Text("Account") })
    }
}
