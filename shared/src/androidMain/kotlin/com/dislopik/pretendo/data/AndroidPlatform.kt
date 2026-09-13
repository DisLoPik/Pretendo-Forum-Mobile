package com.dislopik.pretendo.data

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Holds the application context so the shared module can reach Android storage and system
 * services. [initPretendoAndroid] must be called once from the launcher activity.
 */
@SuppressLint("StaticFieldLeak")
internal object AndroidAppContext {
    private var context: Context? = null

    fun init(value: Context) {
        context = value.applicationContext
    }

    fun require(): Context = requireNotNull(context) {
        "initPretendoAndroid(context) must be called before using the shared module"
    }
}

fun initPretendoAndroid(context: Context) = AndroidAppContext.init(context)

actual class KeyValueStore {
    private val prefs
        get() = AndroidAppContext.require().getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun getString(key: String): String? = prefs.getString(key, null)

    actual fun putString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    private companion object {
        const val PREFS = "pretendo_forum"
    }
}

actual fun openInBrowser(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { AndroidAppContext.require().startActivity(intent) }
}

actual fun copyToClipboard(text: String) {
    val clipboard = AndroidAppContext.require()
        .getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Pretendo Forum", text))
}

actual fun shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { AndroidAppContext.require().startActivity(chooser) }
        .onFailure { copyToClipboard(text) }
}

@Composable
actual fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
