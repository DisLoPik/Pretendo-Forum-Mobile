package com.dislopik.pretendo.model

/** The topic lists the app offers, matching Discourse's own top-level routes. */
enum class TopicFilter(val path: String, val label: String, val requiresAuth: Boolean) {
    Latest("latest", "Latest", false),
    Hot("hot", "Hot", false),
    Top("top", "Top", false),
    New("new", "New", true),
    Unread("unread", "Unread", true),
    Bookmarks("bookmarks", "Bookmarks", true)
}

/** Periods accepted by /top.json. */
enum class TopPeriod(val id: String, val label: String) {
    Day("daily", "Today"),
    Week("weekly", "This week"),
    Month("monthly", "This month"),
    Quarter("quarterly", "This quarter"),
    Year("yearly", "This year"),
    All("all", "All time")
}

/** The tabs on a user's page. */
enum class ProfileTab(val label: String) {
    Summary("Summary"),
    Activity("Activity"),
    Topics("Topics"),
    Replies("Replies"),
    Likes("Likes")
}

/**
 * `user_actions.json` filter ids. Discourse numbers these in
 * `UserAction::TYPES`; only the ones the profile tabs need are listed.
 */
object UserActionType {
    const val LIKE = 1
    const val WAS_LIKED = 2
    const val REPLY = 5
    const val RESPONSE = 6
    const val MENTION = 7
    const val NEW_TOPIC = 4
    const val BOOKMARK = 3

    /**
     * The filter string a profile tab sends.
     *
     * Only some of these ids are queryable. forum.pretendo.network answers 404 rather than
     * an empty list for `was_liked`, `response` and `mention`, which are tied to what the
     * reader is allowed to see, so the tabs ask only for the types that are always served.
     */
    fun filterFor(tab: ProfileTab): String? = when (tab) {
        ProfileTab.Topics -> NEW_TOPIC.toString()
        ProfileTab.Replies -> REPLY.toString()
        ProfileTab.Likes -> LIKE.toString()
        ProfileTab.Activity -> "$NEW_TOPIC,$REPLY"
        ProfileTab.Summary -> null
    }
}

/**
 * Discourse notification type ids, from `site.json`'s `notification_types`. Only the ones
 * the app renders differently are named; anything else falls back to a generic row.
 */
object NotificationType {
    const val MENTIONED = 1
    const val REPLIED = 2
    const val QUOTED = 3
    const val EDITED = 4
    const val LIKED = 5
    const val PRIVATE_MESSAGE = 6
    const val INVITED_TO_PRIVATE_MESSAGE = 7
    const val INVITEE_ACCEPTED = 8
    const val POSTED = 9
    const val MOVED_POST = 10
    const val LINKED = 11
    const val GRANTED_BADGE = 12
    const val INVITED_TO_TOPIC = 13
    const val CUSTOM = 14
    const val GROUP_MENTIONED = 15
    const val GROUP_MESSAGE_SUMMARY = 16
    const val WATCHING_FIRST_POST = 17
    const val TOPIC_REMINDER = 18
    const val LIKED_CONSOLIDATED = 19
    const val POST_APPROVED = 20
    const val BOOKMARK_REMINDER = 24
    const val REACTION = 25
}

/** Post action ids from `site.json`'s `post_action_types`. */
object PostActionType {
    const val LIKE = 2
    const val OFF_TOPIC = 3
    const val INAPPROPRIATE = 4
    const val SPAM = 8
    const val NOTIFY_USER = 6
    const val NOTIFY_MODERATORS = 7
    const val ILLEGAL = 10
}

/** The reasons the app offers when flagging a post, in the order Discourse lists them. */
enum class FlagReason(val actionId: Int, val label: String, val description: String) {
    OffTopic(
        PostActionType.OFF_TOPIC,
        "Off-Topic",
        "This post is not relevant to the current discussion."
    ),
    Inappropriate(
        PostActionType.INAPPROPRIATE,
        "Inappropriate",
        "This post contains content a reasonable person would consider offensive or abusive."
    ),
    Spam(
        PostActionType.SPAM,
        "Spam",
        "This post is an advertisement, or vandalism. It is not useful or relevant to this topic."
    ),
    Illegal(
        PostActionType.ILLEGAL,
        "Illegal",
        "This post requires staff attention for a legal reason."
    ),
    Other(
        PostActionType.NOTIFY_MODERATORS,
        "Something Else",
        "This post requires staff attention for another reason."
    )
}

/** Whether a topic is a normal thread or a private message. */
val TopicDto.isPrivateMessage: Boolean get() = archetype == "private_message"

val TopicDetailDto.isPrivateMessage: Boolean get() = archetype == "private_message"

/** Discourse marks small actions (closed, renamed, ...) with a post type other than 1. */
val PostDto.isSmallAction: Boolean get() = postType == 3 || postType == 4

/** How many likes a post has, read out of its action summary. */
val PostDto.likeCount: Int
    get() = actionsSummary.firstOrNull { it.id == PostActionType.LIKE }?.count ?: 0

val PostDto.likedByMe: Boolean
    get() = actionsSummary.firstOrNull { it.id == PostActionType.LIKE }?.acted == true

val PostDto.canLike: Boolean
    get() = actionsSummary.firstOrNull { it.id == PostActionType.LIKE }?.canAct == true

val PostDto.canUnlike: Boolean
    get() = actionsSummary.firstOrNull { it.id == PostActionType.LIKE }?.canUndo == true

/** A staff badge to show next to a username, or null for a regular member. */
fun staffLabel(admin: Boolean, moderator: Boolean): String? = when {
    admin -> "admin"
    moderator -> "mod"
    else -> null
}
