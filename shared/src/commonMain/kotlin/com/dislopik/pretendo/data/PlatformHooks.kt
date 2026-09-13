package com.dislopik.pretendo.data

import androidx.compose.runtime.Composable

/**
 * Opens a URL in the phone's browser.
 *
 * Sign-in leans on this: the Pretendo account form is shown by the browser, never by the
 * app, so the app never sees the password and the forum's own two-factor and SSO steps
 * work exactly as they do on the web.
 */
expect fun openInBrowser(url: String)

/** Puts text on the system clipboard, used by "Copy link" and the manual sign-in fallback. */
expect fun copyToClipboard(text: String)

/** Hands a URL to the system share sheet. Falls back to the clipboard where there is none. */
expect fun shareText(text: String)

/** Holds the screen awake while composed. Wired to the "Keep screen on" accessibility option. */
@Composable
expect fun KeepScreenOn(enabled: Boolean)

/** Wall-clock time, used to say how long ago a post was written. */
expect fun currentTimeMillis(): Long
