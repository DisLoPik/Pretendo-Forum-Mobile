package com.dislopik.pretendo.data

import com.dislopik.pretendo.model.CreatePostResponse
import com.dislopik.pretendo.model.CurrentUserDto
import com.dislopik.pretendo.model.CurrentUserResponse
import com.dislopik.pretendo.model.ErrorResponse
import com.dislopik.pretendo.model.FlagReason
import com.dislopik.pretendo.model.NotificationsResponse
import com.dislopik.pretendo.model.PostActionType
import com.dislopik.pretendo.model.PostDto
import com.dislopik.pretendo.model.PostStreamDto
import com.dislopik.pretendo.model.SearchResponse
import com.dislopik.pretendo.model.SiteDto
import com.dislopik.pretendo.model.TopicDetailDto
import com.dislopik.pretendo.model.TopicListResponse
import com.dislopik.pretendo.model.TopPeriod
import com.dislopik.pretendo.model.TopicFilter
import com.dislopik.pretendo.model.UserActionsResponse
import com.dislopik.pretendo.model.UserDto
import com.dislopik.pretendo.model.UserResponse
import com.dislopik.pretendo.model.UserSummaryDto
import com.dislopik.pretendo.model.UserSummaryResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The stored session is missing, expired or refused; the app has to sign in again. */
class ForumAuthException(message: String = "Signed out") : Exception(message)

/** An expected, user-facing failure: a rate limit, a rejected post, a missing topic. */
class ForumException(message: String) : Exception(message)

/** Which kind of write is being sent. */
private enum class WriteMethod { Post, Put, Delete }

/**
 * Client for the Discourse instance at forum.pretendo.network.
 *
 * The app signs in the way a browser does, so it holds a session cookie rather than an API
 * key. That has one consequence worth stating plainly: Discourse protects session writes
 * with a CSRF token, so every post, like and delete carries one, and a rejected token is
 * refreshed and retried once before the failure is believed.
 *
 * Reads work signed out, which is why [ForumAuth] is consulted per request rather than
 * required up front.
 */
class DiscourseApi(
    private val client: HttpClient,
    private val auth: ForumAuth
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Cached because Discourse issues one per session, not one per request. */
    private var csrfToken: String? = null


    suspend fun site(): SiteDto = getJson("/site.json")

    suspend fun currentUser(): CurrentUserDto? {
        if (!auth.isSignedIn) return null
        return getJson<CurrentUserResponse>("/session/current.json").currentUser
    }

    /** One page of a top-level list. Discourse pages from 0. */
    suspend fun topics(
        filter: TopicFilter,
        page: Int = 0,
        period: TopPeriod = TopPeriod.Week
    ): TopicListResponse {
        if (filter.requiresAuth && !auth.isSignedIn) throw ForumAuthException()
        val path = when (filter) {
            TopicFilter.Top -> "/top.json?period=${period.id}&page=$page"
            TopicFilter.Bookmarks -> {
                val username = requireUsername()
                "/u/${username.encodeURLParameter()}/bookmarks.json?page=$page"
            }
            else -> "/${filter.path}.json?page=$page"
        }
        return getJson(path)
    }

    suspend fun categoryTopics(slug: String, categoryId: Int, page: Int = 0): TopicListResponse =
        getJson("/c/${slug.encodeURLParameter()}/$categoryId.json?page=$page")

    suspend fun tagTopics(tag: String, page: Int = 0): TopicListResponse =
        getJson("/tag/${tag.encodeURLParameter()}.json?page=$page")

    /**
     * A topic with its first chunk of posts. Discourse returns roughly 20 at a time and
     * lists every post id in `post_stream.stream`, which is how [postsByIds] pages on.
     */
    suspend fun topic(topicId: Long, postNumber: Int? = null): TopicDetailDto {
        val suffix = if (postNumber != null && postNumber > 1) "/$postNumber" else ""
        return getJson("/t/$topicId$suffix.json?track_visit=${auth.isSignedIn}&forceLoad=true")
    }

    /** Fetches specific posts from a topic's stream, for scrolling past the first chunk. */
    suspend fun postsByIds(topicId: Long, ids: List<Long>): List<PostDto> {
        if (ids.isEmpty()) return emptyList()
        val query = ids.joinToString("&") { "post_ids[]=$it" }
        return getJson<PostStreamDto>("/t/$topicId/posts.json?$query").posts
    }

    suspend fun user(username: String): UserDto =
        getJson<UserResponse>("/u/${username.encodeURLParameter()}.json").user
            ?: throw ForumException("That profile is not available.")

    suspend fun userSummary(username: String): UserSummaryDto =
        getJson<UserSummaryResponse>("/u/${username.encodeURLParameter()}/summary.json").userSummary
            ?: UserSummaryDto()

    suspend fun userActions(username: String, filter: String, offset: Int = 0): UserActionsResponse =
        getJson(
            "/user_actions.json?username=${username.encodeURLParameter()}" +
                "&filter=$filter&offset=$offset&no_results_help_key=user_activity"
        )

    suspend fun notifications(offset: Int = 0): NotificationsResponse {
        requireSignedIn()
        return getJson("/notifications.json?offset=$offset&limit=30")
    }

    suspend fun search(query: String, page: Int = 1): SearchResponse =
        getJson("/search.json?q=${query.encodeURLParameter()}&page=$page")

    /** The private-message inbox. [sent] switches to the messages the user wrote. */
    suspend fun privateMessages(sent: Boolean = false, page: Int = 0): TopicListResponse {
        val username = requireUsername()
        val box = if (sent) "private-messages-sent" else "private-messages"
        return getJson("/topics/$box/${username.encodeURLParameter()}.json?page=$page")
    }


    /** Starts a new topic. Returns the created post, which carries the new topic's id. */
    suspend fun createTopic(
        title: String,
        raw: String,
        categoryId: Int,
        tags: List<String> = emptyList()
    ): CreatePostResponse {
        validateTopic(title, raw)
        return submitPost {
            append("title", title.trim())
            append("raw", raw.trim())
            append("category", categoryId.toString())
            append("archetype", "regular")
            tags.forEach { append("tags[]", it) }
        }
    }

    /** Replies to a topic, optionally addressed to a particular post. */
    suspend fun reply(
        topicId: Long,
        raw: String,
        replyToPostNumber: Int? = null
    ): CreatePostResponse {
        validateBody(raw)
        return submitPost {
            append("raw", raw.trim())
            append("topic_id", topicId.toString())
            append("archetype", "regular")
            if (replyToPostNumber != null) {
                append("reply_to_post_number", replyToPostNumber.toString())
            }
        }
    }

    /** Opens a private message to one or more usernames. */
    suspend fun sendPrivateMessage(
        title: String,
        raw: String,
        recipients: List<String>
    ): CreatePostResponse {
        if (recipients.isEmpty()) throw ForumException("Choose someone to send this to.")
        validateTopic(title, raw)
        return submitPost {
            append("title", title.trim())
            append("raw", raw.trim())
            append("archetype", "private_message")
            append("target_recipients", recipients.joinToString(","))
        }
    }

    /** Adds a reply to an existing private message thread. */
    suspend fun replyToPrivateMessage(topicId: Long, raw: String): CreatePostResponse {
        validateBody(raw)
        return submitPost {
            append("raw", raw.trim())
            append("topic_id", topicId.toString())
            append("archetype", "private_message")
        }
    }

    suspend fun editPost(postId: Long, raw: String, editReason: String? = null): PostDto {
        validateBody(raw)
        val response = write(WriteMethod.Put, "/posts/$postId.json") {
            append("post[raw]", raw.trim())
            if (!editReason.isNullOrBlank()) append("post[edit_reason]", editReason.trim())
        }
        // Discourse answers an edit with { post: {...} }.
        val text = response.bodyAsText()
        return runCatchingUnlessCancelled {
            json.decodeFromString<Map<String, PostDto>>(text)["post"]
        }.getOrNull() ?: runCatchingUnlessCancelled { json.decodeFromString<PostDto>(text) }.getOrNull()
            ?: throw ForumException("The post was saved but could not be read back.")
    }

    suspend fun deletePost(postId: Long) {
        write(WriteMethod.Delete, "/posts/$postId.json")
    }

    suspend fun like(postId: Long) {
        write(WriteMethod.Post, "/post_actions.json") {
            append("id", postId.toString())
            append("post_action_type_id", PostActionType.LIKE.toString())
            append("flag_topic", "false")
        }
    }

    suspend fun unlike(postId: Long) {
        write(
            WriteMethod.Delete,
            "/post_actions/$postId.json?post_action_type_id=${PostActionType.LIKE}"
        )
    }

    suspend fun flagPost(postId: Long, reason: FlagReason, message: String) {
        write(WriteMethod.Post, "/post_actions.json") {
            append("id", postId.toString())
            append("post_action_type_id", reason.actionId.toString())
            append("flag_topic", "false")
            if (message.isNotBlank()) append("message", message.trim())
        }
    }

    /** Toggles a topic bookmark. Returns true when the topic ends up bookmarked. */
    suspend fun setTopicBookmarked(topicId: Long, bookmarked: Boolean): Boolean {
        val method = if (bookmarked) WriteMethod.Post else WriteMethod.Delete
        write(method, "/t/$topicId/bookmark.json")
        return bookmarked
    }

    /**
     * Sets how closely the user follows a topic: 0 muted, 1 regular, 2 tracking,
     * 3 watching. Discourse's own numbering.
     */
    suspend fun setNotificationLevel(topicId: Long, level: Int) {
        write(WriteMethod.Post, "/t/$topicId/notifications.json") {
            append("notification_level", level.toString())
        }
    }

    suspend fun markNotificationsRead() {
        write(WriteMethod.Put, "/notifications/mark-read.json")
    }


    /**
     * A read, retried once if a signed-in session has lapsed.
     *
     * Sessions here are ordinary browser sessions, so they expire on the forum's schedule
     * rather than the app's. Rebuilding one from the stored account token is invisible to
     * the reader, which is better than throwing them back to a login screen mid-scroll.
     */
    private suspend inline fun <reified T> getJson(path: String): T {
        var response = client.get(absolute(path)) { apiHeaders() }
        auth.rememberForumSession(response)

        if (response.status.value.isAuthFailure() && auth.isSignedIn) {
            auth.invalidateSession()
            if (auth.renewSession()) {
                csrfToken = null
                response = client.get(absolute(path)) { apiHeaders() }
                auth.rememberForumSession(response)
            }
        }

        checkResponse(response)
        return runCatchingUnlessCancelled { response.body<T>() }.getOrElse { failure ->
            // Naming the field turns "this app could not read it" into something fixable.
            val detail = failure.message?.lineSequence()?.firstOrNull()?.take(200)
            throw ForumException(
                "The forum sent something this app could not read" +
                    (if (detail.isNullOrBlank()) "." else ": $detail")
            )
        }
    }

    private suspend fun submitPost(build: ParametersBuilder.() -> Unit): CreatePostResponse {
        val response = write(WriteMethod.Post, "/posts.json", build)
        val created = runCatchingUnlessCancelled { response.body<CreatePostResponse>() }.getOrNull()
            ?: throw ForumException("The forum did not confirm your post.")
        if (created.errors.isNotEmpty()) throw ForumException(created.errors.joinToString("\n"))
        return created
    }

    /**
     * Sends a write with a CSRF token attached.
     *
     * Discourse answers a stale token with the same 403 it uses for a lapsed session, so a
     * rejection is met by fetching a fresh token, renewing the session if that is what
     * actually went wrong, and trying exactly once more.
     */
    private suspend fun write(
        method: WriteMethod,
        path: String,
        build: (ParametersBuilder.() -> Unit)? = null
    ): HttpResponse {
        requireSignedIn()

        var response = send(method, path, build, ensureCsrfToken())

        if (response.status.value == 403) {
            // A fresh token is the cheap explanation, so try that before concluding that
            // the session itself is gone. Retrying is safe: a 403 means nothing was written.
            csrfToken = null
            response = send(method, path, build, ensureCsrfToken())

            if (response.status.value == 403) {
                auth.invalidateSession()
                if (auth.renewSession()) {
                    csrfToken = null
                    response = send(method, path, build, ensureCsrfToken())
                }
            }
        }

        checkResponse(response)
        return response
    }

    private suspend fun send(
        method: WriteMethod,
        path: String,
        build: (ParametersBuilder.() -> Unit)?,
        csrf: String
    ): HttpResponse {
        val url = absolute(path)
        val configure: HttpRequestBuilder.() -> Unit = {
            apiHeaders()
            header(CSRF_HEADER, csrf)
            if (build != null) setBody(FormDataContent(Parameters.build(build)))
        }
        val response = when (method) {
            WriteMethod.Post -> client.post(url, configure)
            WriteMethod.Put -> client.put(url, configure)
            WriteMethod.Delete -> client.delete(url, configure)
        }
        auth.rememberForumSession(response)
        return response
    }

    /** Discourse hands out a CSRF token tied to the session cookie. */
    private suspend fun ensureCsrfToken(): String {
        csrfToken?.let { return it }
        val response = client.get(absolute("/session/csrf.json")) { apiHeaders() }
        auth.rememberForumSession(response)
        val token = runCatchingUnlessCancelled {
            json.parseToJsonElement(response.bodyAsText())
                .jsonObject["csrf"]
                ?.jsonPrimitive
                ?.content
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: throw ForumException("The forum would not issue a security token.")
        csrfToken = token
        return token
    }

    private fun requireSignedIn() {
        if (!auth.isSignedIn) throw ForumAuthException("Sign in to do that.")
    }

    private fun requireUsername(): String =
        cachedUsername ?: throw ForumAuthException("Sign in to do that.")

    /** Set once the session is known, because several endpoints are keyed by username. */
    var cachedUsername: String? = null

    private fun absolute(path: String): String = when {
        path.startsWith("http") -> path
        path.startsWith("/") -> Forum.BASE_URL + path
        else -> "${Forum.BASE_URL}/$path"
    }

    private fun HttpRequestBuilder.apiHeaders() {
        header(HttpHeaders.UserAgent, USER_AGENT)
        header(HttpHeaders.Accept, "application/json, text/javascript, */*; q=0.01")
        // Discourse treats requests carrying these as its own front end, which is what
        // this app is standing in for.
        header("X-Requested-With", "XMLHttpRequest")
        header("Discourse-Present", "true")
        auth.forumCookies()?.let { header(HttpHeaders.Cookie, it) }
    }

    private fun Int.isAuthFailure(): Boolean = this == 401 || this == 403

    /**
     * Turns a failed response into the clearest message available. Discourse is good about
     * explaining itself in `errors`, so those are preferred over anything invented here.
     */
    private suspend fun checkResponse(response: HttpResponse) {
        if (response.status.isSuccess()) return

        val code = response.status.value
        val body = runCatchingUnlessCancelled { response.bodyAsText() }.getOrDefault("")
        val parsed = runCatchingUnlessCancelled { json.decodeFromString<ErrorResponse>(body) }.getOrNull()
        val message = parsed?.errors?.filter { it.isNotBlank() }?.joinToString("\n")

        when (code) {
            401, 403 -> {
                // Having tried a renewal already, a refusal here means the session is
                // genuinely gone rather than merely stale.
                if (auth.isSignedIn) {
                    auth.invalidateSession()
                    throw ForumAuthException(
                        message ?: "Your forum session ended. Please sign in again."
                    )
                }
                throw ForumException(message ?: "You do not have access to that.")
            }
            404 -> throw ForumException(message ?: "That is not on the forum any more.")
            422 -> throw ForumException(message ?: "The forum would not accept that.")
            429 -> {
                val wait = parsed?.extras?.waitSeconds
                throw ForumException(
                    message ?: if (wait != null) {
                        "You are doing that too fast. Try again in $wait seconds."
                    } else {
                        "You are doing that too fast. Try again in a moment."
                    }
                )
            }
            in 500..599 -> throw ForumException("The forum is having trouble right now ($code).")
            else -> throw ForumException(message ?: "The forum returned $code.")
        }
    }

    private fun validateTopic(title: String, raw: String) {
        if (title.trim().length < MIN_TITLE_LENGTH) {
            throw ForumException("Titles need at least $MIN_TITLE_LENGTH characters.")
        }
        validateBody(raw)
    }

    private fun validateBody(raw: String) {
        if (raw.trim().length < MIN_BODY_LENGTH) {
            throw ForumException("Posts need at least $MIN_BODY_LENGTH characters.")
        }
    }

    companion object {
        const val MIN_TITLE_LENGTH = 15
        const val MIN_BODY_LENGTH = 20

        private const val CSRF_HEADER = "X-CSRF-Token"

        /** Cloudflare fronts the forum and is unfriendly to clients with no user agent. */
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/126.0.6478.71 Mobile Safari/537.36"
    }
}
