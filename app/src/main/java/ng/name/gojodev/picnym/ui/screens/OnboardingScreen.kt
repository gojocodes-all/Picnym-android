package ng.name.gojodev.picnym.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ng.name.gojodev.picnym.ui.PicnymMark
import ng.name.gojodev.picnym.ui.theme.PicnymPalette

private data class IntroPage(
    val eyebrow: String,
    val title: String,
    val description: String,
    val color: Color,
    val icons: List<ImageVector>
)

private val introPages = listOf(
    IntroPage(
        eyebrow = "ONE LINK · FOUR FORMATS",
        title = "Say more than text.",
        description = "Open one shareable inbox for messages, photos, voice notes and quick polls.",
        color = PicnymPalette.Indigo,
        icons = listOf(Icons.Outlined.ChatBubbleOutline, Icons.Outlined.Image, Icons.Outlined.GraphicEq, Icons.Outlined.Poll)
    ),
    IntroPage(
        eyebrow = "CONTROL BEFORE CURIOSITY",
        title = "Your inbox. Your rules.",
        description = "Pause your link, hide words, limit who can send, report messages and block unwanted senders.",
        color = PicnymPalette.Coral,
        icons = listOf(Icons.Outlined.Security, Icons.Outlined.Tune, Icons.Outlined.Block)
    ),
    IntroPage(
        eyebrow = "PRIVATE FIRST",
        title = "Share only what you choose.",
        description = "Replies stay private until you deliberately publish or turn them into a share-ready PICNYM card.",
        color = PicnymPalette.Mint,
        icons = listOf(Icons.Outlined.ChatBubbleOutline, Icons.Outlined.Share)
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState(pageCount = { introPages.size })
    val scope = rememberCoroutineScope()
    val lastPage = pager.currentPage == introPages.lastIndex

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(top = 18.dp, bottom = 22.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PicnymMark(size = 36.dp)
                Text("PICNYM", modifier = Modifier.padding(start = 10.dp).weight(1f), fontWeight = FontWeight.Black)
                TextButton(onClick = onFinish) { Text("Skip") }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 22.dp),
                pageSpacing = 14.dp
            ) { index ->
                val page = introPages[index]
                val scale by animateFloatAsState(if (pager.currentPage == index) 1f else 0.94f, label = "introCardScale")
                Column(
                    modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }.padding(vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IntroArtwork(page, index)
                    Spacer(Modifier.height(30.dp))
                    Text(page.eyebrow, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    Text(page.title, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    introPages.indices.forEach { index ->
                        Box(
                            Modifier
                                .size(if (pager.currentPage == index) 22.dp else 7.dp, 7.dp)
                                .clip(CircleShape)
                                .background(if (pager.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        )
                    }
                }
                Button(
                    onClick = {
                        if (lastPage) onFinish()
                        else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    },
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                ) { Text(if (lastPage) "Create or sign in" else "Next") }
            }
            Text(
                "PICNYM is intended for adults 18+.",
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IntroArtwork(page: IntroPage, index: Int) {
    Box(
        modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(30.dp)).background(page.color),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(210.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)))
        if (index == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                page.icons.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { icon -> IntroIcon(icon, dark = true) }
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    page.icons.forEach { icon ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IntroIcon(icon, dark = false)
                            Box(Modifier.size(if (index == 1) 108.dp else 132.dp, 8.dp).clip(CircleShape).background(Color(0xFFE7E8F1)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroIcon(icon: ImageVector, dark: Boolean) {
    Box(
        Modifier.size(if (dark) 72.dp else 42.dp).clip(RoundedCornerShape(if (dark) 22.dp else 13.dp))
            .background(if (dark) Color.White.copy(alpha = 0.16f) else Color(0xFFEEF0FF)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (dark) Color.White else PicnymPalette.Indigo, modifier = Modifier.size(if (dark) 30.dp else 22.dp))
    }
}
