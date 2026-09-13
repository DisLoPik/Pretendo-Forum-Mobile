package com.dislopik.pretendo.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dislopik.pretendo.model.CategoryDto
import com.dislopik.pretendo.model.CurrentUserDto

/**
 * The forum's category list.
 *
 * Topic lists only carry a `category_id`, so every list needs this to show a name and a
 * colour. It changes about once a year, so it is fetched once per launch and kept.
 */
class SiteCache(private val api: DiscourseApi) {

    var categories: List<CategoryDto> by mutableStateOf(emptyList())
        private set

    private var loaded = false

    suspend fun ensureLoaded() {
        if (loaded) return
        val fetched = runCatchingUnlessCancelled { api.site().categories }.getOrNull() ?: return
        categories = fetched
        loaded = true
    }

    suspend fun reload() {
        loaded = false
        ensureLoaded()
    }

    fun byId(id: Int?): CategoryDto? {
        if (id == null) return null
        return categories.firstOrNull { it.id == id }
    }

    /** Top-level categories, each paired with the subcategories filed under it. */
    fun grouped(): List<Pair<CategoryDto, List<CategoryDto>>> {
        val parents = categories.filter { it.parentCategoryId == null }.sortedBy { it.position }
        return parents.map { parent ->
            parent to categories
                .filter { it.parentCategoryId == parent.id }
                .sortedBy { it.position }
        }
    }

    /** Where a new topic can be filed. Discourse refuses posts to some of these. */
    fun postableCategories(): List<CategoryDto> =
        categories.filterNot { it.readRestricted }.sortedBy { it.position }
}

/** Who is signed in, and what is waiting for them. */
class Session(private val api: DiscourseApi, private val auth: ForumAuth) {

    var currentUser: CurrentUserDto? by mutableStateOf(null)
        private set

    /** Bumped whenever a write succeeds, so open screens know to re-read. */
    var refreshKey: Int by mutableStateOf(0)
        private set

    val isSignedIn: Boolean get() = auth.isSignedIn

    val username: String? get() = currentUser?.username

    suspend fun refresh() {
        if (!auth.isSignedIn) {
            currentUser = null
            api.cachedUsername = null
            return
        }
        // A failed lookup means the forum was unreachable, not that the reader was
        // signed out, so whatever is already known about them is kept.
        val user = runCatchingUnlessCancelled { api.currentUser() }.getOrNull() ?: return
        currentUser = user
        api.cachedUsername = user.username
    }

    fun signOut() {
        auth.signOut()
        currentUser = null
        api.cachedUsername = null
        invalidate()
    }

    fun invalidate() {
        refreshKey++
    }
}
