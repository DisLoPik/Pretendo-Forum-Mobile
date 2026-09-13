package com.dislopik.pretendo.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.data.runCatchingUnlessCancelled
import com.dislopik.pretendo.data.DiscourseApi
import com.dislopik.pretendo.model.CategoryDto
import com.dislopik.pretendo.ui.ComposerRequest
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.categoryColor
import com.dislopik.pretendo.ui.components.CategoryChip
import com.dislopik.pretendo.ui.components.ForumTopBar
import kotlinx.coroutines.launch

/**
 * Writing anything: a new topic, a reply, an edit, or a private message.
 *
 * One screen covers all four because they differ only in which fields are shown, and a
 * single well-behaved editor is worth more on a phone than four half-finished ones.
 */
@Composable
fun ComposerScreen(
    request: ComposerRequest,
    onBack: () -> Unit,
    onPosted: (topicId: Long?, postNumber: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimens.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var recipients by remember {
        mutableStateOf((request as? ComposerRequest.NewMessage)?.to.orEmpty())
    }
    var body by remember {
        mutableStateOf(
            TextFieldValue(
                when (request) {
                    is ComposerRequest.Reply -> request.quoted.orEmpty()
                    is ComposerRequest.Edit -> request.existingRaw.orEmpty()
                    else -> ""
                }
            )
        )
    }
    var category by remember {
        mutableStateOf(
            (request as? ComposerRequest.NewTopic)?.categoryId?.let { Forum.site.byId(it) }
        )
    }
    var pickingCategory by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val needsTitle = request is ComposerRequest.NewTopic || request is ComposerRequest.NewMessage
    val needsCategory = request is ComposerRequest.NewTopic
    val needsRecipients = request is ComposerRequest.NewMessage

    val canSubmit = !submitting &&
        body.text.trim().length >= DiscourseApi.MIN_BODY_LENGTH &&
        (!needsTitle || title.trim().length >= DiscourseApi.MIN_TITLE_LENGTH) &&
        (!needsCategory || category != null) &&
        (!needsRecipients || recipients.isNotBlank())

    fun submit() {
        if (!canSubmit) return
        submitting = true
        error = null
        scope.launch {
            val outcome = runCatchingUnlessCancelled {
                when (request) {
                    is ComposerRequest.NewTopic -> {
                        val created = Forum.api.createTopic(
                            title = title,
                            raw = body.text,
                            categoryId = requireNotNull(category).id
                        )
                        created.topicId to created.postNumber
                    }

                    is ComposerRequest.Reply -> {
                        val created = if (request.isPrivateMessage) {
                            Forum.api.replyToPrivateMessage(request.topicId, body.text)
                        } else {
                            Forum.api.reply(request.topicId, body.text, request.replyToPostNumber)
                        }
                        created.topicId to created.postNumber
                    }

                    is ComposerRequest.NewMessage -> {
                        val created = Forum.api.sendPrivateMessage(
                            title = title,
                            raw = body.text,
                            recipients = recipients.split(',', ' ')
                                .map { it.trim().removePrefix("@") }
                                .filter { it.isNotEmpty() }
                        )
                        created.topicId to created.postNumber
                    }

                    is ComposerRequest.Edit -> {
                        val edited = Forum.api.editPost(request.postId, body.text)
                        edited.topicId to edited.postNumber
                    }
                }
            }
            submitting = false
            outcome
                .onSuccess { (topicId, postNumber) ->
                    Forum.session.invalidate()
                    onPosted(topicId.takeIf { it != 0L }, postNumber.takeIf { it != 0 })
                }
                .onFailure { error = it.message ?: "The forum would not accept that." }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ForumTopBar(
                title = titleFor(request),
                subtitle = subtitleFor(request),
                onBack = onBack,
                actions = {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp).padding(end = 4.dp)
                        )
                    } else {
                        TextButton(onClick = { submit() }, enabled = canSubmit) {
                            Text(if (request is ComposerRequest.Edit) "Save" else "Post")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(dimens.itemSpacing)
        ) {
            if (error != null) {
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (needsRecipients) {
                OutlinedTextField(
                    value = recipients,
                    onValueChange = { recipients = it },
                    label = { Text("To") },
                    placeholder = { Text("username, another-username") },
                    supportingText = { Text("Separate names with a comma.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (needsTitle) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (needsRecipients) "Subject" else "Title") },
                    supportingText = {
                        val remaining = DiscourseApi.MIN_TITLE_LENGTH - title.trim().length
                        Text(
                            if (remaining > 0) {
                                "$remaining more characters needed"
                            } else {
                                "${title.trim().length} characters"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (needsCategory) {
                CategoryField(
                    category = category,
                    onClick = { pickingCategory = true }
                )
            }

            FormattingBar(
                onWrap = { prefix, suffix -> body = body.wrapSelection(prefix, suffix) },
                onPrefixLine = { prefix -> body = body.prefixLine(prefix) }
            )

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Your post") },
                placeholder = { Text("Write what you want to say. Markdown works here.") },
                supportingText = {
                    val remaining = DiscourseApi.MIN_BODY_LENGTH - body.text.trim().length
                    Text(
                        if (remaining > 0) {
                            "$remaining more characters needed"
                        } else {
                            "${body.text.trim().length} characters"
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp)
            )

            Button(
                onClick = { submit() },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().heightIn(min = dimens.minTouchTarget)
            ) {
                Icon(PretendoIcons.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(if (request is ComposerRequest.Edit) "Save changes" else "Post")
            }

            Spacer(Modifier.size(dimens.sectionSpacing))
        }
    }

    if (pickingCategory) {
        CategoryPicker(
            onDismiss = { pickingCategory = false },
            onPick = {
                category = it
                pickingCategory = false
            }
        )
    }
}

@Composable
private fun CategoryField(category: CategoryDto?, onClick: () -> Unit) {
    val dimens = LocalDimens.current
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.minTouchTarget)
            .padding(vertical = 4.dp)
    ) {
        if (category != null) {
            CategoryChip(name = category.name, colorHex = category.color, onClick = onClick)
        } else {
            Text(
                text = "Choose a category",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(if (category == null) "Choose" else "Change") }
    }
}

@Composable
private fun CategoryPicker(onDismiss: () -> Unit, onPick: (CategoryDto) -> Unit) {
    val categories = Forum.site.postableCategories()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Where should this go?") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(categories, key = { it.id }) { category ->
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .padding(vertical = 2.dp)
                    ) {
                        TextButton(onClick = { onPick(category) }) {
                            CategoryChip(name = category.name, colorHex = category.color)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Markdown shortcuts.
 *
 * Typing `**bold**` on a phone keyboard means three keyboard switches per word, so the
 * app offers the handful of marks the forum actually uses as buttons.
 */
@Composable
private fun FormattingBar(
    onWrap: (String, String) -> Unit,
    onPrefixLine: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        AssistChip(onClick = { onWrap("**", "**") }, label = { Text("Bold") })
        AssistChip(onClick = { onWrap("_", "_") }, label = { Text("Italic") })
        AssistChip(onClick = { onWrap("`", "`") }, label = { Text("Code") })
        AssistChip(onClick = { onPrefixLine("> ") }, label = { Text("Quote") })
        AssistChip(onClick = { onPrefixLine("- ") }, label = { Text("List") })
        AssistChip(onClick = { onWrap("[", "](https://)") }, label = { Text("Link") })
        AssistChip(onClick = { onWrap("~~", "~~") }, label = { Text("Strike") })
    }
}

/** Wraps the selection, or drops the marks at the cursor ready to type between them. */
private fun TextFieldValue.wrapSelection(prefix: String, suffix: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val updated = buildString {
        append(text.substring(0, start))
        append(prefix)
        append(text.substring(start, end))
        append(suffix)
        append(text.substring(end))
    }
    val cursor = if (start == end) start + prefix.length else end + prefix.length + suffix.length
    return TextFieldValue(text = updated, selection = TextRange(cursor))
}

/** Puts a marker at the start of the line the cursor is on. */
private fun TextFieldValue.prefixLine(prefix: String): TextFieldValue {
    val lineStart = text.lastIndexOf('\n', (selection.min - 1).coerceAtLeast(0))
        .let { if (it < 0) 0 else it + 1 }
    val updated = text.substring(0, lineStart) + prefix + text.substring(lineStart)
    return TextFieldValue(
        text = updated,
        selection = TextRange(selection.min + prefix.length)
    )
}

private fun titleFor(request: ComposerRequest): String = when (request) {
    is ComposerRequest.NewTopic -> "New topic"
    is ComposerRequest.Reply -> "Reply"
    is ComposerRequest.Edit -> "Edit post"
    is ComposerRequest.NewMessage -> "New message"
}

private fun subtitleFor(request: ComposerRequest): String? = when (request) {
    is ComposerRequest.NewTopic -> request.categoryName
    is ComposerRequest.Reply -> request.replyToUsername?.let { "to $it" } ?: request.topicTitle
    is ComposerRequest.Edit -> request.topicTitle.takeIf { it.isNotBlank() }
    is ComposerRequest.NewMessage -> null
}
