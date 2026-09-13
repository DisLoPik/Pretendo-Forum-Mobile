package com.dislopik.pretendo.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * How the app reads the forum's JSON.
 *
 * Named rather than inlined into the client so that tests can decode captured responses
 * exactly the way the app does; a test that was lenient in different places would prove
 * nothing.
 */
val forumJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

/**
 * The one HTTP client the app uses.
 *
 * Discourse answers with JSON throughout, and sends far more fields than any one screen
 * needs, so unknown keys are ignored rather than treated as a parse failure.
 */
fun createForumHttpClient(): HttpClient = HttpClient {
    expectSuccess = false
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 30_000
    }
    install(ContentNegotiation) {
        json(forumJson)
    }
}

/**
 * Process-wide wiring. There is one forum, one key and one settings file, so a service
 * locator keeps the screens free of dependency plumbing.
 */
object Forum {
    const val BASE_URL = "https://forum.pretendo.network"

    val httpClient: HttpClient by lazy { createForumHttpClient() }
    val auth: ForumAuth by lazy { ForumAuth() }
    val api: DiscourseApi by lazy { DiscourseApi(httpClient, auth) }
    val settings: SettingsController by lazy { SettingsController() }
    val site: SiteCache by lazy { SiteCache(api) }
    val session: Session by lazy { Session(api, auth) }

    /**
     * Turns a forum-relative path into something loadable. Discourse hands back paths like
     * `/uploads/...` and avatar templates like `/user_avatar/.../{size}/1_2.png`.
     */
    fun absoluteUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return when {
            path.startsWith("http://") || path.startsWith("https://") -> path
            path.startsWith("//") -> "https:$path"
            path.startsWith("/") -> BASE_URL + path
            else -> "$BASE_URL/$path"
        }
    }

    /** Fills in the `{size}` placeholder Discourse leaves in every avatar template. */
    fun avatarUrl(template: String?, size: Int = 96): String? =
        absoluteUrl(template?.replace("{size}", size.toString()))

    /** The web address of a topic, used by Share and Copy link. */
    fun topicUrl(topicId: Long, slug: String?, postNumber: Int? = null): String {
        val base = if (slug.isNullOrBlank()) "$BASE_URL/t/$topicId" else "$BASE_URL/t/$slug/$topicId"
        return if (postNumber != null && postNumber > 1) "$base/$postNumber" else base
    }

    fun userUrl(username: String): String = "$BASE_URL/u/$username"
}
