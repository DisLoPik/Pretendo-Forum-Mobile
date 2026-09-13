package com.dislopik.pretendo.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Signing in with a Pretendo Network username and password.
 *
 * The forum has no account system of its own: it delegates to Pretendo Network through
 * DiscourseConnect, and its user API keys are switched off. So the app walks the same path
 * a browser walks, and ends up holding the same session cookie a browser would:
 *
 *  1. the username and password go to Pretendo's account API, which answers with tokens;
 *  2. the forum is asked to start a sign-on, and points at Pretendo to vouch for the user;
 *  3. Pretendo, given the token from step 1, signs a payload and points back at the forum;
 *  4. the forum reads that payload and issues its `_t` session cookie.
 *
 * From then on the app is an ordinary signed-in session, which is why writes also need a
 * CSRF token; see [DiscourseApi].
 */
class ForumAuth(
    private val store: KeyValueStore = KeyValueStore(),
    private val client: HttpClient = Forum.httpClient
) {
    /** The forum session cookie. Its presence is what "signed in" means. */
    var sessionCookie: String? = store.getString(KEY_SESSION)
        private set

    /**
     * Discourse's own session cookie. It carries the CSRF identity, and the forum rotates
     * it, so it is tracked alongside the login cookie rather than assumed constant.
     */
    var forumSessionCookie: String? = store.getString(KEY_FORUM_SESSION)
        private set

    /** Pretendo's account token, kept so a lapsed forum session can be renewed silently. */
    private var accessToken: String? = store.getString(KEY_ACCESS_TOKEN)
    private var refreshToken: String? = store.getString(KEY_REFRESH_TOKEN)

    val isSignedIn: Boolean get() = sessionCookie != null

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * The handshake client.
     *
     * It stops at every redirect instead of chasing it, because the hops cross between the
     * forum and the account site and each needs different cookies attached. The shared
     * client goes on following redirects normally for everything else.
     */
    private val chain: HttpClient = client.config { followRedirects = false }


    /**
     * Signs in from scratch. Throws [ForumAuthException] with a message worth showing to
     * the person who typed the password.
     */
    suspend fun signIn(username: String, password: String) {
        if (username.isBlank() || password.isEmpty()) {
            throw ForumAuthException("Enter your username and password.")
        }
        val tokens = requestAccountTokens(username.trim(), password)
        accessToken = tokens.first
        refreshToken = tokens.second
        store.putString(KEY_ACCESS_TOKEN, tokens.first)
        store.putString(KEY_REFRESH_TOKEN, tokens.second)

        establishForumSession()
    }

    /**
     * Trades a Pretendo username and password for account tokens.
     *
     * The account API answers failures with a machine-readable `code`, which is worth
     * translating: "Could not find user" and "Incorrect password" are the two a person
     * actually needs to tell apart.
     */
    private suspend fun requestAccountTokens(username: String, password: String): Pair<String, String> {
        val response = runCatchingUnlessCancelled {
            client.post("$ACCOUNT_URL/api/auth/login") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                header(HttpHeaders.UserAgent, DiscourseApi.USER_AGENT)
                setBody(
                    buildJsonObject {
                        put("username", username)
                        put("password", password)
                    }.toString()
                )
            }
        }.getOrElse {
            throw ForumAuthException("Could not reach Pretendo Network. Check your connection.")
        }

        val body = runCatchingUnlessCancelled { response.bodyAsText() }.getOrDefault("")
        val parsed = runCatchingUnlessCancelled { json.parseToJsonElement(body).jsonObject }.getOrNull()

        if (!response.status.isSuccess()) {
            throw ForumAuthException(accountErrorMessage(parsed, response.status.value))
        }

        val access = parsed?.string("accessToken")
            ?: throw ForumAuthException("Pretendo Network did not return a sign-in token.")
        val refresh = parsed.string("refreshToken").orEmpty()
        return access to refresh
    }

    private fun accountErrorMessage(parsed: JsonObject?, code: Int): String {
        val data = parsed?.get("data")?.let { runCatchingUnlessCancelled { it.jsonObject }.getOrNull() }
        return when (data?.string("code")) {
            "INVALID_USERNAME" -> "There is no account with that username."
            "INVALID_PASSWORD" -> "That password is not right."
            "UNDER_THIRTEEN" -> "That account cannot be used here."
            "INVALID_CAPTCHA" ->
                "Pretendo Network asked for a captcha. Sign in on the website once, then try again."
            else -> data?.string("message")
                ?: parsed?.string("message")
                ?: "Could not sign in ($code)."
        }
    }

    /**
     * Walks the sign-on handshake and keeps the forum cookie that falls out of it.
     *
     * The hops cross between the forum and the account site: the forum leg has to carry
     * the session cookie holding the sign-on nonce, and the Pretendo leg the account
     * token. The number of hops is not fixed, so this follows whatever it is given until
     * the forum hands over a login cookie or stops redirecting.
     */
    private suspend fun establishForumSession() {
        var url = "${Forum.BASE_URL}/session/sso"

        repeat(MAX_HANDSHAKE_HOPS) {
            val onForum = url.startsWith(Forum.BASE_URL)
            val response = chain.get(url) {
                browserHeaders()
                if (onForum) {
                    forumCookies()?.let { header(HttpHeaders.Cookie, it) }
                } else {
                    header(HttpHeaders.Cookie, "access_token=$accessToken")
                    // Harmless if the account site only reads the cookie, and correct if
                    // it prefers a bearer token.
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }

            if (onForum) {
                rememberForumSession(response)
                // The forum decides which hop issues the login cookie, so every one of
                // its answers is checked rather than only the last.
                response.cookie(COOKIE_SESSION)?.let { session ->
                    sessionCookie = session
                    store.putString(KEY_SESSION, session)
                    return
                }
            }

            val next = response.location()
                ?: throw ForumAuthException(handshakeFailure(url, response.status.value))

            url = resolveAgainst(next, url)

            // Being sent to a login page means the account token was not accepted.
            if (url.startsWith("$ACCOUNT_URL/account/login")) {
                clearTokens()
                throw ForumAuthException(
                    "Pretendo Network did not accept the sign-in. Please try again."
                )
            }
        }

        throw ForumAuthException("The sign-in went round in circles. Please try again.")
    }

    /** Names the hop that gave up, because otherwise every break reads the same. */
    private fun handshakeFailure(url: String, status: Int): String = when {
        url.endsWith("/session/sso") ->
            "The forum did not start the sign-in handshake ($status)."
        url.startsWith(ACCOUNT_URL) ->
            "Pretendo Network did not confirm the sign-in ($status)."
        else -> "The forum did not complete the sign-in ($status)."
    }

    /**
     * Silently rebuilds a forum session that has lapsed, using the stored account token
     * and, failing that, the refresh token. Returns true when the session is usable again.
     */
    suspend fun renewSession(): Boolean {
        if (accessToken == null && refreshToken.isNullOrEmpty()) return false

        if (runCatchingUnlessCancelled { establishForumSession() }.isSuccess) return true

        // The account token may itself have expired; a refresh is the last thing to try.
        val refreshed = runCatchingUnlessCancelled { refreshAccountToken() }.getOrDefault(false)
        if (!refreshed) return false
        return runCatchingUnlessCancelled { establishForumSession() }.isSuccess
    }

    private suspend fun refreshAccountToken(): Boolean {
        val token = refreshToken?.takeIf { it.isNotEmpty() } ?: return false
        val response = client.post("$ACCOUNT_URL/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.UserAgent, DiscourseApi.USER_AGENT)
            setBody(buildJsonObject { put("token", token) }.toString())
        }
        if (!response.status.isSuccess()) return false

        val parsed = runCatchingUnlessCancelled {
            json.parseToJsonElement(response.bodyAsText()).jsonObject
        }.getOrNull() ?: return false

        val access = parsed.string("accessToken") ?: return false
        accessToken = access
        store.putString(KEY_ACCESS_TOKEN, access)
        parsed.string("refreshToken")?.let {
            refreshToken = it
            store.putString(KEY_REFRESH_TOKEN, it)
        }
        return true
    }


    fun signOut() {
        sessionCookie = null
        forumSessionCookie = null
        store.putString(KEY_SESSION, null)
        store.putString(KEY_FORUM_SESSION, null)
        clearTokens()
    }

    private fun clearTokens() {
        accessToken = null
        refreshToken = null
        store.putString(KEY_ACCESS_TOKEN, null)
        store.putString(KEY_REFRESH_TOKEN, null)
    }

    /** Drops only the forum session, so a renewal can be attempted with the tokens kept. */
    fun invalidateSession() {
        sessionCookie = null
        store.putString(KEY_SESSION, null)
    }


    /** The Cookie header value for forum requests, or null while signed out. */
    fun forumCookies(): String? {
        val parts = buildList {
            sessionCookie?.let { add("$COOKIE_SESSION=$it") }
            forumSessionCookie?.let { add("$COOKIE_FORUM_SESSION=$it") }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }

    /** Discourse rotates its session cookie, so every response is checked for a new one. */
    fun rememberForumSession(response: HttpResponse) {
        val updated = response.cookie(COOKIE_FORUM_SESSION) ?: return
        if (updated != forumSessionCookie) {
            forumSessionCookie = updated
            store.putString(KEY_FORUM_SESSION, updated)
        }
    }

    private fun HttpResponse.cookie(name: String): String? =
        headers.getAll(HttpHeaders.SetCookie)
            ?.firstOrNull { it.startsWith("$name=") }
            ?.substringAfter("$name=")
            ?.substringBefore(';')
            ?.takeIf { it.isNotBlank() && it != "deleted" }

    private fun HttpResponse.location(): String? =
        headers[HttpHeaders.Location]?.takeIf { it.isNotBlank() }

    private fun io.ktor.client.request.HttpRequestBuilder.browserHeaders() {
        header(HttpHeaders.UserAgent, DiscourseApi.USER_AGENT)
        header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
    }

    /** Resolves a Location header against the address that returned it. */
    private fun resolveAgainst(location: String, current: String): String = when {
        location.startsWith("http") -> location
        location.startsWith("/") -> originOf(current) + location
        else -> originOf(current) + "/" + location
    }

    private fun originOf(url: String): String {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd < 0) return Forum.BASE_URL
        val pathStart = url.indexOf('/', schemeEnd + 3)
        return if (pathStart < 0) url else url.substring(0, pathStart)
    }

    private fun JsonObject.string(key: String): String? =
        runCatchingUnlessCancelled { this[key]?.jsonPrimitive?.content }.getOrNull()?.takeIf { it.isNotBlank() }

    companion object {
        const val ACCOUNT_URL = "https://pretendo.network"

        /** Discourse's login cookie, and the session cookie that carries CSRF identity. */
        private const val COOKIE_SESSION = "_t"
        private const val COOKIE_FORUM_SESSION = "_forum_session"

        private const val KEY_SESSION = "auth.forum_session"
        private const val KEY_FORUM_SESSION = "auth.discourse_session"
        private const val KEY_ACCESS_TOKEN = "auth.pn_access_token"
        private const val KEY_REFRESH_TOKEN = "auth.pn_refresh_token"

        /** Enough for the real chain several times over, and short of an endless loop. */
        private const val MAX_HANDSHAKE_HOPS = 8
    }
}
