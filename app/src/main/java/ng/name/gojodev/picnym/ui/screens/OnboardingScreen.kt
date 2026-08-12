package ng.name.gojodev.picnym.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val panelColor: Color,
    val panelText: Color,
    val rows: List<String>
)

private val introPages = listOf(
    IntroPage(
        eyebrow = "ONE LINK / FOUR FORMATS",
        title = "Ask for honesty. Not just text.",
        description = "Open one shareable inbox for anonymous text, supported photos, voice notes and quick polls.",
        panelColor = PicnymPalette.Blue,
        panelText = Color.White,
        rows = listOf("Text", "Photo", "Voice", "Poll")
    ),
    IntroPage(
        eyebrow = "CONTROL BEFORE CURIOSITY",
        title = "Your inbox. Your rules.",
        description = "Pause your link, hide words, limit who can send, report messages and block unwanted senders.",
        panelColor = PicnymPalette.Orange,
        panelText = PicnymPalette.Ink,
        rows = listOf("Pause", "Filter", "Restrict", "Block")
    ),
    IntroPage(
        eyebrow = "PRIVATE BY DEFAULT",
        title = "You decide what happens next.",
        description = "Messages stay private until you deliberately reply, publish or turn an answer into a share card.",
        panelColor = PicnymPalette.Ink,
        panelText = PicnymPalette.Paper,
        rows = listOf("Keep private", "Reply", "Publish", "Remove")
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
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text("PICNYM", fontWeight = FontWeight.Black)
                    Text("PRIVATE INBOX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onFinish) { Text("Skip") }
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 22.dp),
                pageSpacing = 14.dp
            ) { index ->
                val page = introPages[index]
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    IntroLedger(page)
                    Spacer(Modifier.height(30.dp))
                    Text(page.eyebrow, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(10.dp))
                    Text(page.title, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    introPages.indices.forEach { index ->
                        Box(
                            Modifier
                                .width(if (pager.currentPage == index) 24.dp else 8.dp)
                                .height(4.dp)
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
private fun IntroLedger(page: IntroPage) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = page.panelColor,
        contentColor = page.panelText,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("PICNYM / MESSAGE DESK", style = MaterialTheme.typography.labelSmall, color = page.panelText.copy(alpha = .74f))
            Spacer(Modifier.height(28.dp))
            page.rows.forEachIndexed { index, label ->
                HorizontalDivider(color = page.panelText.copy(alpha = .46f))
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("0${index + 1}", modifier = Modifier.width(42.dp), style = MaterialTheme.typography.labelMedium)
                    Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
            }
            HorizontalDivider(color = page.panelText.copy(alpha = .46f))
        }
    }
}
