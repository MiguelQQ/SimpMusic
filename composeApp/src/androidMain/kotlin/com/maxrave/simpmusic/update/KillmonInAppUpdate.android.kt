package com.maxrave.simpmusic.update

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.maxrave.simpmusic.BuildKonfig
import com.maxrave.simpmusic.ui.component.KillmonPrimaryButton
import com.maxrave.simpmusic.ui.theme.killmonGreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

private const val RELEASES_URL = "https://api.github.com/repos/MiguelQQ/SimpMusic/releases?per_page=100"

@Composable
actual fun KillmonInAppUpdate(active: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var checked by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    var offer by remember { mutableStateOf<KillmonUpdate?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var indeterminate by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloaded by remember { mutableStateOf<File?>(null) }
    var resumeAfterPermission by remember { mutableStateOf(false) }
    val downloadJob = remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(active) {
        if (!active || checked) return@LaunchedEffect
        checked = true
        offer =
            withContext(Dispatchers.IO) {
                try {
                    val body = fetchText(RELEASES_URL) ?: return@withContext null
                    selectKillmonUpdate(
                        releasesJson = body,
                        installedName = BuildKonfig.versionName,
                        installedCode = BuildKonfig.versionCode,
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
    }

    fun dismiss() {
        downloadJob.value?.cancel()
        downloading = false
        resumeAfterPermission = false
        dismissed = true
    }

    fun begin() {
        val current = offer ?: return
        if (dismissed || downloading) return
        if (!context.canInstallPackages()) {
            resumeAfterPermission = true
            context.openUnknownSourcesSettings()
            return
        }
        resumeAfterPermission = false
        val ready = downloaded
        if (ready != null && ready.isFile && ready.length() > 100_000L) {
            context.installApk(ready)
            return
        }
        downloading = true
        indeterminate = true
        progress = 0f
        downloadJob.value?.cancel()
        downloadJob.value =
            scope.launch {
                val file =
                    try {
                        downloadApk(current.downloadUrl, context.cacheDir) { fraction, knownTotal ->
                            indeterminate = !knownTotal
                            progress = fraction
                        }
                    } catch (cancelled: CancellationException) {
                        downloading = false
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                downloading = false
                if (file == null) return@launch
                downloaded = file
                if (context.canInstallPackages()) {
                    context.installApk(file)
                } else {
                    resumeAfterPermission = true
                    context.openUnknownSourcesSettings()
                }
            }
    }

    DisposableEffect(lifecycleOwner, resumeAfterPermission, downloading, downloaded) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && resumeAfterPermission && !downloading) {
                    if (context.canInstallPackages()) begin()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val current = offer
    if (current == null || dismissed) return
    AlertDialog(
        onDismissRequest = { dismiss() },
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text(
                text = "Nueva versión ${current.label} disponible",
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        },
        text =
            if (!downloading) {
                null
            } else {
                {
                    Column {
                        Text(
                            text =
                                if (indeterminate) {
                                    "Descargando…"
                                } else {
                                    "Descargando… ${progress.coerceIn(0f, 1f).times(100).roundToInt()}%"
                                },
                            color = Color(0xFFB3B3B3),
                        )
                        Spacer(Modifier.height(12.dp))
                        if (indeterminate) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = killmonGreen,
                                trackColor = Color(0xFF333333),
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = killmonGreen,
                                trackColor = Color(0xFF333333),
                            )
                        }
                    }
                }
            },
        confirmButton = {
            KillmonPrimaryButton(
                text = "Actualizar",
                loading = downloading,
                expanded = false,
                onClick = { begin() },
            )
        },
        dismissButton = {
            TextButton(onClick = { dismiss() }) {
                Text("Más tarde", color = Color.White)
            }
        },
    )
}

private fun android.content.Context.canInstallPackages(): Boolean =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        packageManager.canRequestPackageInstalls()
    } else {
        true
    }

private fun android.content.Context.openUnknownSourcesSettings() {
    val intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

private fun android.content.Context.installApk(file: File) {
    val uri = FileProvider.getUriForFile(this, "$packageName.FileProvider", file)
    val intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    runCatching {
        val matches = packageManager.queryIntentActivities(intent, 0)
        for (match in matches) {
            grantUriPermission(
                match.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        startActivity(intent)
    }
}

private fun fetchText(url: String): String? {
    val connection = open(url, readTimeoutMs = 15_000, accept = "application/vnd.github+json") ?: return null
    return try {
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private suspend fun downloadApk(
    url: String,
    cacheDir: File,
    onProgress: (fraction: Float, knownTotal: Boolean) -> Unit,
): File? =
    withContext(Dispatchers.IO) {
        val connection = open(url, readTimeoutMs = 60_000, accept = "*/*") ?: return@withContext null
        val partial = File(cacheDir, "KillmonMusic-update.apk.part")
        try {
            val total = connection.contentLengthLong
            partial.delete()
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var readTotal = 0L
                    var lastPercent = -1
                    while (true) {
                        ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        readTotal += count
                        if (total > 0L) {
                            val percent = ((readTotal * 100) / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                val fraction = (readTotal.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                withContext(Dispatchers.Main) { onProgress(fraction, true) }
                            }
                        }
                    }
                }
            }
            if (partial.length() < 100_000L || !partial.startsWithZip()) {
                partial.delete()
                return@withContext null
            }
            val dest = File(cacheDir, "KillmonMusic-update.apk")
            if (dest.exists()) dest.delete()
            if (!partial.renameTo(dest)) {
                partial.copyTo(dest, overwrite = true)
                partial.delete()
            }
            withContext(Dispatchers.Main) { onProgress(1f, true) }
            dest
        } catch (cancelled: CancellationException) {
            partial.delete()
            throw cancelled
        } catch (_: Exception) {
            partial.delete()
            null
        } finally {
            connection.disconnect()
        }
    }

private fun open(
    url: String,
    readTimeoutMs: Int,
    accept: String,
): HttpURLConnection? {
    val connection =
        try {
            URL(url).openConnection() as HttpURLConnection
        } catch (_: Exception) {
            return null
        }
    return try {
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 12_000
        connection.readTimeout = readTimeoutMs
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "KillmonMusic")
        connection.setRequestProperty("Accept", accept)
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            null
        } else {
            connection
        }
    } catch (_: Exception) {
        connection.disconnect()
        null
    }
}

private fun File.startsWithZip(): Boolean {
    if (length() < 4L) return false
    return inputStream().use { input ->
        val header = ByteArray(4)
        input.read(header) == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
    }
}
