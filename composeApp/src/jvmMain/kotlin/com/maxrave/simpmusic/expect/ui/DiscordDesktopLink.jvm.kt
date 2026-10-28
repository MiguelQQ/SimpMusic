package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxrave.common.DiscordDesktop
import com.maxrave.common.WindowsDiscordIpcSession
import com.maxrave.simpmusic.expect.copyToClipboard
import com.maxrave.simpmusic.expect.openUrl
import com.maxrave.simpmusic.ui.theme.typo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.copied_to_clipboard
import simpmusic.composeapp.generated.resources.discord_desktop_copy_snippet
import simpmusic.composeapp.generated.resources.discord_desktop_intro
import simpmusic.composeapp.generated.resources.discord_desktop_ipc_body
import simpmusic.composeapp.generated.resources.discord_desktop_ipc_button
import simpmusic.composeapp.generated.resources.discord_desktop_ipc_missing
import simpmusic.composeapp.generated.resources.discord_desktop_open_login
import simpmusic.composeapp.generated.resources.discord_desktop_snippet_local
import simpmusic.composeapp.generated.resources.discord_desktop_snippet_webpack
import simpmusic.composeapp.generated.resources.discord_desktop_token_body
import simpmusic.composeapp.generated.resources.discord_desktop_token_hint
import simpmusic.composeapp.generated.resources.discord_desktop_token_invalid
import simpmusic.composeapp.generated.resources.discord_desktop_token_save
import simpmusic.composeapp.generated.resources.discord_desktop_token_title

/**
 * localStorage still holds the token on discord.com/app as a JSON string (quotes included).
 * The iframe reads it from the app origin. Same scrape the Android WebView runs.
 */
private const val LOCAL_STORAGE_SNIPPET =
    "(function(){var i=document.createElement('iframe');document.body.appendChild(i);" +
        "alert(i.contentWindow.localStorage.token.slice(1,-1))})()"

/**
 * Discord sometimes leaves localStorage.token empty and keeps the token on the webpack module.
 * The module graph moves between builds; this is the fallback when the first snippet throws.
 */
private const val WEBPACK_SNIPPET =
    "(webpackChunkdiscord_app.push([[''],{},e=>{m=[];for(let c in e.c)m.push(e.c[c])}]),m)" +
        ".find(m=>m?.exports?.default?.getToken!==void 0).exports.default.getToken()"

@Composable
internal fun DiscordDesktopLink(
    modifier: Modifier,
    onLoginDone: (String) -> Unit,
    aboveContent: @Composable BoxScope.() -> Unit,
) {
    val isWindows = remember { WindowsDiscordIpcSession.isWindows() }
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val ipcMissing = stringResource(Res.string.discord_desktop_ipc_missing)
    val tokenInvalid = stringResource(Res.string.discord_desktop_token_invalid)
    val copied = stringResource(Res.string.copied_to_clipboard)

    Box(modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.discord_desktop_intro),
                    style = typo().bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )

                if (isWindows) {
                    Text(
                        text = stringResource(Res.string.discord_desktop_ipc_body),
                        style = typo().bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Button(
                        onClick = {
                            if (busy) return@Button
                            busy = true
                            status = null
                            scope.launch {
                                val linked =
                                    withContext(Dispatchers.IO) {
                                        WindowsDiscordIpcSession.probe(DiscordDesktop.APPLICATION_ID)
                                    }
                                busy = false
                                if (linked) {
                                    onLoginDone(DiscordDesktop.WINDOWS_IPC_TOKEN)
                                } else {
                                    statusIsError = true
                                    status = ipcMissing
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = CircleShape,
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.discord_desktop_ipc_button),
                                style = typo().labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                Text(
                    text = stringResource(Res.string.discord_desktop_token_title),
                    style = typo().titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.discord_desktop_token_body),
                    style = typo().bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { openUrl(DiscordDesktop.LOGIN_URL) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = CircleShape,
                ) {
                    Text(
                        text = stringResource(Res.string.discord_desktop_open_login),
                        style = typo().labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                SnippetBlock(
                    title = stringResource(Res.string.discord_desktop_snippet_local),
                    snippet = LOCAL_STORAGE_SNIPPET,
                    copyLabel = stringResource(Res.string.discord_desktop_copy_snippet),
                    onCopy = {
                        copyToClipboard("discord-token-snippet", LOCAL_STORAGE_SNIPPET)
                        statusIsError = false
                        status = copied
                    },
                )
                SnippetBlock(
                    title = stringResource(Res.string.discord_desktop_snippet_webpack),
                    snippet = WEBPACK_SNIPPET,
                    copyLabel = stringResource(Res.string.discord_desktop_copy_snippet),
                    onCopy = {
                        copyToClipboard("discord-token-snippet", WEBPACK_SNIPPET)
                        statusIsError = false
                        status = copied
                    },
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.discord_desktop_token_hint)) },
                )
                Button(
                    onClick = {
                        val cleaned = DiscordDesktop.normalizeUserToken(token)
                        if (cleaned == null) {
                            statusIsError = true
                            status = tokenInvalid
                        } else {
                            onLoginDone(cleaned)
                        }
                    },
                    enabled = !busy && token.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = CircleShape,
                ) {
                    Text(
                        text = stringResource(Res.string.discord_desktop_token_save),
                        style = typo().labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                if (status != null) {
                    Text(
                        text = status.orEmpty(),
                        style = typo().bodySmall,
                        color =
                            if (statusIsError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        aboveContent()
    }
}

@Composable
private fun SnippetBlock(
    title: String,
    snippet: String,
    copyLabel: String,
    onCopy: () -> Unit,
) {
    Text(
        text = title,
        style = typo().labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    SelectionContainer {
        Text(
            text = snippet,
            style = typo().bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    OutlinedButton(
        onClick = onCopy,
        modifier = Modifier.fillMaxWidth(),
        shape = CircleShape,
    ) {
        Text(
            text = copyLabel,
            style = typo().labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
