package ng.name.gojodev.picnym.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ng.name.gojodev.picnym.BuildConfig
import ng.name.gojodev.picnym.data.AuthRepository
import ng.name.gojodev.picnym.ui.PicnymMark
import ng.name.gojodev.picnym.util.openUrl
import ng.name.gojodev.picnym.util.requestGoogleIdToken

@Composable
fun AuthScreen(auth: AuthRepository, onSignedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var signUp by remember { mutableStateOf(true) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var eligible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun changeMode(create: Boolean) {
        signUp = create
        error = null
        message = null
    }

    fun runGoogleSignIn() {
        if (signUp && !eligible) {
            error = "Confirm that you are 18+ and accept the Terms first."
            return
        }
        busy = true; error = null; message = null
        scope.launch {
            try {
                val idToken = requestGoogleIdToken(context)
                val result = auth.signInWithGoogleIdToken(idToken)
                message = result.message
                if (result.signedIn) onSignedIn()
            } catch (t: Throwable) {
                error = t.message ?: "Google sign-in was not completed."
            } finally { busy = false }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().widthIn(max = 560.dp), verticalAlignment = Alignment.CenterVertically) {
            PicnymMark(size = 44.dp)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("PICNYM", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Text("PRIVATE INBOX", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            }
            Text("18+", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(34.dp))
        Text(
            if (signUp) "Make your PICNYM." else "Good to see you again.",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        Text(
            if (signUp) "Keep every link, reply and safety control under one account." else "Your links and private inboxes are right where you left them.",
            modifier = Modifier.padding(top = 10.dp, start = 14.dp, end = 14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(26.dp))
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = signUp, onClick = { changeMode(true) }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.extraSmall, label = { Text("Create account") }, enabled = !busy)
                    FilterChip(selected = !signUp, onClick = { changeMode(false) }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.extraSmall, label = { Text("Sign in") }, enabled = !busy)
                }

                OutlinedButton(onClick = ::runGoogleSignIn, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                    Box(Modifier.background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.extraSmall).padding(horizontal = 7.dp, vertical = 3.dp)) {
                        Text("G", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Black)
                    }
                    Text("Continue with Google", modifier = Modifier.padding(start = 10.dp))
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(Modifier.weight(1f))
                    Text("or use email", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.weight(1f))
                }

                AnimatedContent(
                    targetState = signUp,
                    transitionSpec = {
                        (fadeIn() + slideInHorizontally { it / 6 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 6 })
                    },
                    label = "authMode"
                ) { create ->
                    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        if (create) {
                            OutlinedTextField(displayName, { displayName = it.take(60) }, modifier = Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
                        }
                        OutlinedTextField(
                            email, { email = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        OutlinedTextField(
                            password, { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            supportingText = if (create) ({ Text("Use at least 8 characters.") }) else null
                        )
                        if (create) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                Checkbox(checked = eligible, onCheckedChange = { eligible = it }, enabled = !busy)
                                Text(
                                    "I confirm I am 18 or older and accept the Terms and Privacy Policy.",
                                    modifier = Modifier.padding(top = 11.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = {
                                if (email.isBlank() || password.length < 8 || (create && displayName.isBlank())) {
                                    error = "Enter valid details. Passwords need at least 8 characters."
                                    return@Button
                                }
                                if (create && !eligible) {
                                    error = "Confirm that you are 18+ and accept the Terms first."
                                    return@Button
                                }
                                busy = true; error = null; message = null
                                scope.launch {
                                    try {
                                        val result = if (create) auth.signUp(displayName, email, password) else auth.signIn(email, password)
                                        message = result.message
                                        if (result.signedIn) onSignedIn()
                                    } catch (t: Throwable) {
                                        error = t.message ?: "Authentication failed."
                                    } finally { busy = false }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(), enabled = !busy
                        ) { Text(if (busy) "Please wait…" else if (create) "Create account" else "Sign in") }
                    }
                }

                AnimatedVisibility(error != null || message != null) {
                    Text(error ?: message.orEmpty(), color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Anonymous to recipients. Account-protected for you.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TextButton(onClick = { openUrl(context, BuildConfig.SITE_URL + "/privacy") }) { Text("Privacy") }
            TextButton(onClick = { openUrl(context, BuildConfig.SITE_URL + "/terms") }) { Text("Terms") }
            TextButton(onClick = { openUrl(context, BuildConfig.SITE_URL + "/safety") }) { Text("Safety") }
        }
    }
}
