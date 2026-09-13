package com.dislopik.pretendo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire types for the Discourse JSON API that forum.pretendo.network runs on.
 *
 * Every field is optional or defaulted. Discourse omits keys that do not apply to the
 * reader (a signed-out visitor gets no `bookmarked`, no `can_edit`), and plugins such as
 * discourse-reactions add keys a stock instance never sends.
 */


@Serializable
data class SiteDto(
    val categories: List<CategoryDto> = emptyList(),
    @SerialName("uncategorized_category_id") val uncategorizedCategoryId: Int? = null
)

@Serializable
data class CategoryDto(
    val id: Int,
    val name: String = "",
    val slug: String = "",
    val color: String = "0088CC",
    @SerialName("text_color") val textColor: String = "FFFFFF",
    val description: String? = null,
    @SerialName("description_excerpt") val descriptionExcerpt: String? = null,
    @SerialName("topic_count") val topicCount: Int = 0,
    @SerialName("post_count") val postCount: Int = 0,
    val position: Int = 0,
    @SerialName("parent_category_id") val parentCategoryId: Int? = null,
    @SerialName("read_restricted") val readRestricted: Boolean = false,
    @SerialName("has_children") val hasChildren: Boolean = false,
    @SerialName("uploaded_logo") val uploadedLogo: UploadDto? = null
)

@Serializable
data class UploadDto(val url: String? = null)


@Serializable
data class TopicListResponse(
    @SerialName("topic_list") val topicList: TopicListDto? = null,
    val users: List<UserBriefDto> = emptyList()
)

@Serializable
data class TopicListDto(
    @SerialName("can_create_topic") val canCreateTopic: Boolean = false,
    @SerialName("more_topics_url") val moreTopicsUrl: String? = null,
    @SerialName("per_page") val perPage: Int = 30,
    val topics: List<TopicDto> = emptyList()
)

@Serializable
data class TopicDto(
    val id: Long,
    val title: String = "",
    @SerialName("fancy_title") val fancyTitle: String? = null,
    val slug: String = "",
    @SerialName("posts_count") val postsCount: Int = 0,
    @SerialName("reply_count") val replyCount: Int = 0,
    @SerialName("highest_post_number") val highestPostNumber: Int = 0,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_posted_at") val lastPostedAt: String? = null,
    @SerialName("bumped_at") val bumpedAt: String? = null,
    val excerpt: String? = null,
    val visible: Boolean = true,
    val closed: Boolean = false,
    val archived: Boolean = false,
    val pinned: Boolean = false,
    @SerialName("pinned_globally") val pinnedGlobally: Boolean = false,
    val bookmarked: Boolean? = null,
    val liked: Boolean? = null,
    val unseen: Boolean = false,
    @Serializable(with = TagNamesSerializer::class)
    val tags: List<String> = emptyList(),
    val views: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("has_accepted_answer") val hasAcceptedAnswer: Boolean = false,
    @SerialName("last_poster_username") val lastPosterUsername: String? = null,
    @SerialName("category_id") val categoryId: Int? = null,
    val archetype: String = "regular",
    val posters: List<PosterDto> = emptyList(),
    /** Only the private-message lists carry this. */
    val participants: List<UserBriefDto> = emptyList(),
    @SerialName("unread_posts") val unreadPosts: Int? = null,
    @SerialName("new_posts") val newPosts: Int? = null,
    @SerialName("last_read_post_number") val lastReadPostNumber: Int? = null
)

@Serializable
data class PosterDto(
    val description: String? = null,
    @SerialName("user_id") val userId: Long? = null,
    val extras: String? = null
)

@Serializable
data class UserBriefDto(
    val id: Long = 0,
    val username: String = "",
    val name: String? = null,
    @SerialName("avatar_template") val avatarTemplate: String? = null,
    @SerialName("primary_group_name") val primaryGroupName: String? = null,
    @SerialName("flair_name") val flairName: String? = null,
    @SerialName("flair_url") val flairUrl: String? = null,
    val moderator: Boolean = false,
    val admin: Boolean = false,
    @SerialName("trust_level") val trustLevel: Int = 0
)


@Serializable
data class TopicDetailDto(
    val id: Long,
    val title: String = "",
    @SerialName("fancy_title") val fancyTitle: String? = null,
    val slug: String = "",
    @SerialName("posts_count") val postsCount: Int = 0,
    @SerialName("highest_post_number") val highestPostNumber: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("category_id") val categoryId: Int? = null,
    val archetype: String = "regular",
    val views: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("participant_count") val participantCount: Int = 0,
    val closed: Boolean = false,
    val archived: Boolean = false,
    val pinned: Boolean = false,
    val bookmarked: Boolean? = null,
    @Serializable(with = TagNamesSerializer::class)
    val tags: List<String> = emptyList(),
    @SerialName("chunk_size") val chunkSize: Int = 20,
    @SerialName("has_accepted_answer") val hasAcceptedAnswer: Boolean = false,
    @SerialName("post_stream") val postStream: PostStreamDto = PostStreamDto(),
    val details: TopicDetailsDto? = null,
    @SerialName("suggested_topics") val suggestedTopics: List<TopicDto> = emptyList()
)

@Serializable
data class PostStreamDto(
    val posts: List<PostDto> = emptyList(),
    val stream: List<Long> = emptyList()
)

@Serializable
data class TopicDetailsDto(
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("can_create_post") val canCreatePost: Boolean = false,
    @SerialName("notification_level") val notificationLevel: Int? = null,
    @SerialName("created_by") val createdBy: UserBriefDto? = null,
    val participants: List<UserBriefDto> = emptyList(),
    @SerialName("allowed_users") val allowedUsers: List<UserBriefDto> = emptyList()
)

@Serializable
data class PostDto(
    val id: Long,
    val username: String = "",
    val name: String? = null,
    @SerialName("avatar_template") val avatarTemplate: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val cooked: String = "",
    val raw: String? = null,
    @SerialName("post_number") val postNumber: Int = 0,
    @SerialName("post_type") val postType: Int = 1,
    @SerialName("reply_count") val replyCount: Int = 0,
    @SerialName("reply_to_post_number") val replyToPostNumber: Int? = null,
    @SerialName("topic_id") val topicId: Long = 0,
    @SerialName("topic_slug") val topicSlug: String? = null,
    @SerialName("user_id") val userId: Long = 0,
    @SerialName("user_title") val userTitle: String? = null,
    @SerialName("primary_group_name") val primaryGroupName: String? = null,
    @SerialName("flair_url") val flairUrl: String? = null,
    @SerialName("trust_level") val trustLevel: Int = 0,
    val moderator: Boolean = false,
    val admin: Boolean = false,
    val staff: Boolean = false,
    val hidden: Boolean = false,
    val wiki: Boolean = false,
    val yours: Boolean = false,
    val version: Int = 1,
    @SerialName("edit_reason") val editReason: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("user_deleted") val userDeleted: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
    val bookmarked: Boolean = false,
    @SerialName("accepted_answer") val acceptedAnswer: Boolean = false,
    @SerialName("actions_summary") val actionsSummary: List<ActionSummaryDto> = emptyList(),
    /** discourse-reactions, which this forum runs alongside plain likes. */
    val reactions: List<ReactionDto> = emptyList(),
    @SerialName("current_user_reaction") val currentUserReaction: CurrentReactionDto? = null,
    @SerialName("reaction_users_count") val reactionUsersCount: Int = 0
)

@Serializable
data class ActionSummaryDto(
    val id: Int,
    val count: Int = 0,
    val acted: Boolean = false,
    @SerialName("can_act") val canAct: Boolean = false,
    @SerialName("can_undo") val canUndo: Boolean = false
)

@Serializable
data class ReactionDto(val id: String = "", val type: String = "emoji", val count: Int = 0)

@Serializable
data class CurrentReactionDto(val id: String = "", @SerialName("can_undo") val canUndo: Boolean = false)


@Serializable
data class UserResponse(val user: UserDto? = null)

@Serializable
data class UserDto(
    val id: Long = 0,
    val username: String = "",
    val name: String? = null,
    @SerialName("avatar_template") val avatarTemplate: String? = null,
    val title: String? = null,
    @SerialName("bio_cooked") val bioCooked: String? = null,
    @SerialName("bio_excerpt") val bioExcerpt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_posted_at") val lastPostedAt: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("trust_level") val trustLevel: Int = 0,
    val moderator: Boolean = false,
    val admin: Boolean = false,
    @SerialName("badge_count") val badgeCount: Int = 0,
    @SerialName("profile_view_count") val profileViewCount: Int = 0,
    @SerialName("time_read") val timeRead: Long = 0,
    @SerialName("primary_group_name") val primaryGroupName: String? = null,
    @SerialName("flair_url") val flairUrl: String? = null,
    @SerialName("profile_background_upload_url") val profileBackgroundUrl: String? = null,
    @SerialName("can_send_private_message_to_user") val canSendPm: Boolean = false,
    val groups: List<GroupMembershipDto> = emptyList()
)

@Serializable
data class GroupMembershipDto(
    val id: Int = 0,
    val name: String = "",
    @SerialName("full_name") val fullName: String? = null,
    val title: String? = null
)

@Serializable
data class UserSummaryResponse(@SerialName("user_summary") val userSummary: UserSummaryDto? = null)

@Serializable
data class UserSummaryDto(
    @SerialName("likes_given") val likesGiven: Int = 0,
    @SerialName("likes_received") val likesReceived: Int = 0,
    @SerialName("topics_entered") val topicsEntered: Int = 0,
    @SerialName("posts_read_count") val postsReadCount: Int = 0,
    @SerialName("days_visited") val daysVisited: Int = 0,
    @SerialName("topic_count") val topicCount: Int = 0,
    @SerialName("post_count") val postCount: Int = 0,
    @SerialName("time_read") val timeRead: Long = 0,
    @SerialName("solved_count") val solvedCount: Int = 0
)

@Serializable
data class CurrentUserResponse(@SerialName("current_user") val currentUser: CurrentUserDto? = null)

@Serializable
data class CurrentUserDto(
    val id: Long = 0,
    val username: String = "",
    val name: String? = null,
    @SerialName("avatar_template") val avatarTemplate: String? = null,
    val moderator: Boolean = false,
    val admin: Boolean = false,
    @SerialName("trust_level") val trustLevel: Int = 0,
    @SerialName("unread_notifications") val unreadNotifications: Int = 0,
    @SerialName("unread_private_messages") val unreadPrivateMessages: Int = 0,
    @SerialName("unread_high_priority_notifications") val unreadHighPriority: Int = 0,
    @SerialName("can_create_topic") val canCreateTopic: Boolean = false,
    @SerialName("can_send_private_messages") val canSendPrivateMessages: Boolean = false
)


@Serializable
data class UserActionsResponse(@SerialName("user_actions") val userActions: List<UserActionDto> = emptyList())

@Serializable
data class UserActionDto(
    @SerialName("action_type") val actionType: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    val excerpt: String? = null,
    @SerialName("topic_id") val topicId: Long = 0,
    @SerialName("post_number") val postNumber: Int = 1,
    val title: String = "",
    val slug: String = "",
    @SerialName("category_id") val categoryId: Int? = null,
    val username: String = "",
    @SerialName("avatar_template") val avatarTemplate: String? = null,
    @SerialName("acting_username") val actingUsername: String? = null
)

@Serializable
data class NotificationsResponse(
    val notifications: List<NotificationDto> = emptyList(),
    @SerialName("total_rows_notifications") val totalRows: Int = 0
)

@Serializable
data class NotificationDto(
    val id: Long = 0,
    @SerialName("notification_type") val notificationType: Int = 0,
    val read: Boolean = false,
    @SerialName("high_priority") val highPriority: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("post_number") val postNumber: Int? = null,
    @SerialName("topic_id") val topicId: Long? = null,
    val slug: String? = null,
    @SerialName("fancy_title") val fancyTitle: String? = null,
    val data: NotificationDataDto = NotificationDataDto()
)

@Serializable
data class NotificationDataDto(
    @SerialName("topic_title") val topicTitle: String? = null,
    @SerialName("original_username") val originalUsername: String? = null,
    @SerialName("display_username") val displayUsername: String? = null,
    @SerialName("badge_name") val badgeName: String? = null,
    val message: String? = null,
    val count: Int? = null,
    val username: String? = null,
    @SerialName("group_name") val groupName: String? = null
)

@Serializable
data class SearchResponse(
    val topics: List<TopicDto> = emptyList(),
    val posts: List<SearchPostDto> = emptyList(),
    val users: List<UserBriefDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList()
)

@Serializable
data class SearchPostDto(
    val id: Long = 0,
    val username: String = "",
    @SerialName("avatar_template") val avatarTemplate: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("post_number") val postNumber: Int = 1,
    @SerialName("topic_id") val topicId: Long = 0,
    val blurb: String = ""
)


@Serializable
data class CreatePostResponse(
    val id: Long = 0,
    @SerialName("topic_id") val topicId: Long = 0,
    @SerialName("post_number") val postNumber: Int = 0,
    @SerialName("topic_slug") val topicSlug: String? = null,
    val errors: List<String> = emptyList()
)

@Serializable
data class ErrorResponse(
    val errors: List<String> = emptyList(),
    @SerialName("error_type") val errorType: String? = null,
    val extras: ErrorExtrasDto? = null
)

@Serializable
data class ErrorExtrasDto(@SerialName("wait_seconds") val waitSeconds: Int? = null)
