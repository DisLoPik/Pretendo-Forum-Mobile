package com.dislopik.pretendo.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.Foundation.NSURL
import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController
import platform.UIKit.UIPasteboard

actual class KeyValueStore {
    private val defaults get() = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String): String? = defaults.stringForKey(PREFIX + key)

    actual fun putString(key: String, value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(PREFIX + key)
        } else {
            defaults.setObject(value, PREFIX + key)
        }
    }

    private companion object {
        const val PREFIX = "pretendo_forum."
    }
}

actual fun openInBrowser(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any?>(), null)
}

actual fun copyToClipboard(text: String) {
    UIPasteboard.generalPasteboard.string = text
}

actual fun shareText(text: String) {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController
    if (root == null) {
        copyToClipboard(text)
        return
    }
    val controller = UIActivityViewController(listOf(text), null)
    // On iPad the sheet is a popover and needs an anchor; the root view is a safe default.
    controller.popoverPresentationController?.sourceView = root.view
    root.presentViewController(controller, animated = true, completion = null)
}

@Composable
actual fun KeepScreenOn(enabled: Boolean) {
    DisposableEffect(enabled) {
        UIApplication.sharedApplication.idleTimerDisabled = enabled
        onDispose { UIApplication.sharedApplication.idleTimerDisabled = false }
    }
}

actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000.0).toLong()
