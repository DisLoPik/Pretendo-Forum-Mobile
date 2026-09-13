package com.dislopik.pretendo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.data.runCatchingUnlessCancelled
import com.dislopik.pretendo.data.ForumAuth
import com.dislopik.pretendo.data.openInBrowser
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.components.ForumTopBar
import kotlinx.coroutines.launch

/**
 * Signing in with a Pretendo Network username and password.
 *
 * The forum itself has no separate account, so these are the same details used on
 * pretendo.network. They are sent to Pretendo's account service and exchanged for a forum
 * session; the password is not stored, only the session that comes back.
 */
@Composable
fun SignInScreen(
    onBack: (() -> Unit)?,
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimens.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canSubmit = !busy && username.isNotBlank() && password.isNotEmpty()

    fun submit() {
        if (!canSubmit) return
        busy = true
        error = null
        scope.launch {
            runCatchingUnlessCancelled { Forum.auth.signIn(username, password) }
                .onSuccess {
                    // Nothing keeps the password after this point.
                    password = ""
                    Forum.session.refresh()
                    Forum.session.invalidate()
                    busy = false
                    onSignedIn()
                }
                .onFailure {
                    busy = false
                    error = it.message ?: "Could not sign in."
                }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { ForumTopBar(title = "Sign in", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(dimens.itemSpacing)
        ) {
            Text(
                text = "Sign in",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Use your Pretendo Network account, the same one as on " +
                    "pretendo.network. The forum does not have a separate login.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (error != null) {
                ErrorNotice(error!!)
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                enabled = !busy,
                visualTransformation = if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = PretendoIcons.Eye,
                            contentDescription = if (showPassword) {
                                "Hide the password"
                            } else {
                                "Show the password"
                            },
                            tint = if (showPassword) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { submit() },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouchTarget)
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.size(10.dp))
                    Text("Signing in")
                } else {
                    Icon(
                        imageVector = PretendoIcons.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Sign in")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { openInBrowser("${ForumAuth.ACCOUNT_URL}/account/forgot-password") }
                ) {
                    Text("Forgot password")
                }
                TextButton(
                    onClick = { openInBrowser("${ForumAuth.ACCOUNT_URL}/account/register") }
                ) {
                    Text("Create an account")
                }
            }

            Spacer(Modifier.size(dimens.sectionSpacing))

            Text(
                text = "You can read the whole forum without signing in. An account is only " +
                    "needed to post, reply, like and send messages.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Your password is sent to Pretendo Network to sign in and is not kept " +
                    "on this phone. Signing out removes the session straight away.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.size(dimens.sectionSpacing))
        }
    }
}

@Composable
private fun ErrorNotice(message: String) {
    val dimens = LocalDimens.current
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(dimens.corner),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(dimens.cardPadding)) {
            Icon(
                imageVector = PretendoIcons.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
