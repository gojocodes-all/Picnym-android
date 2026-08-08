package ng.name.gojodev.picnym.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ng.name.gojodev.picnym.BuildConfig
import ng.name.gojodev.picnym.data.AuthRepository
import ng.name.gojodev.picnym.ui.PicnymMark
import ng.name.gojodev.picnym.util.openUrl

@Composable
fun AuthScreen(auth: AuthRepository, onSignedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var signUp by remember { mutableStateOf(true) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PicnymMark(size = 58.dp)
        Spacer(Modifier.height(18.dp))
        Text("PICNYM", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Text("Say it anonymously. Share more than text.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { signUp = true; error = null; message = null }, modifier = Modifier.weight(1f), enabled = !busy) { Text("Create account") }
            OutlinedButton(onClick = { signUp = false; error = null; message = null }, modifier = Modifier.weight(1f), enabled = !busy) { Text("Sign in") }
        }
        Spacer(Modifier.height(14.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (signUp) "Create your PICNYM account" else "Welcome back", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (signUp) {
                    OutlinedTextField(displayName, { displayName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
                }
                OutlinedTextField(
                    email, { email = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    password, { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                Button(
                    onClick = {
                        if (email.isBlank() || password.length < 8 || (signUp && displayName.isBlank())) {
                            error = "Enter valid details. Passwords need at least 8 characters."
                            return@Button
                        }
                        busy = true; error = null; message = null
                        scope.launch {
                            try {
                                val result = if (signUp) auth.signUp(displayName, email, password) else auth.signIn(email, password)
                                message = result.message
                                if (result.signedIn) onSignedIn()
                            } catch (t: Throwable) {
                                error = t.message ?: "Authentication failed."
                            } finally { busy = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), enabled = !busy
                ) { Text(if (busy) "Please wait…" else if (signUp) "Create account" else "Sign in") }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Native Android client · PICNYM", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { openUrl(context, BuildConfig.SITE_URL + "/privacy") }) { Text("Privacy") }
            OutlinedButton(onClick = { openUrl(context, BuildConfig.SITE_URL + "/terms") }) { Text("Terms") }
            OutlinedButton(onClick = { openUrl(context, BuildConfig.SITE_URL + "/safety") }) { Text("Safety") }
        }
    }
}
