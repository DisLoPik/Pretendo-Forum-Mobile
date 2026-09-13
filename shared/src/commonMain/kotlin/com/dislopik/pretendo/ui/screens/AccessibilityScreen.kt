package com.dislopik.pretendo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dislopik.pretendo.data.AccentColor
import com.dislopik.pretendo.data.AccessibilitySettings
import com.dislopik.pretendo.data.FontChoice
import com.dislopik.pretendo.data.Forum
import com.dislopik.pretendo.data.LetterSpacing
import com.dislopik.pretendo.data.LineSpacing
import com.dislopik.pretendo.data.PostLength
import com.dislopik.pretendo.data.ThemeMode
import com.dislopik.pretendo.data.UiDensity
import com.dislopik.pretendo.ui.LocalDimens
import com.dislopik.pretendo.ui.LocalSettings
import com.dislopik.pretendo.ui.PretendoIcons
import com.dislopik.pretendo.ui.components.ForumTopBar

/**
 * The Accessibility page.
 *
 * Everything here is stored on the phone, because a Discourse account has nowhere to keep
 * it. That is not only a limitation: the phone that needs larger text is often not the
 * only place the same account gets used.
 *
 * Every control changes the app the moment it is touched, and the sample at the top is
 * drawn with the real theme, so the effect of a change is visible without leaving the page.
 */
@Composable
fun AccessibilityScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimens.current
    val settings = LocalSettings.current
    var confirmingReset by remember { mutableStateOf(false) }

    fun update(transform: (AccessibilitySettings) -> AccessibilitySettings) {
        Forum.settings.update(transform)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ForumTopBar(
                title = "Accessibility",
                subtitle = "Saved on this phone",
                onBack = onBack,
                actions = {
                    TextButton(onClick = { confirmingReset = true }) { Text("Reset") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PreviewCard()

            SettingsSection("Text size") {
                TextScaleControl(
                    value = settings.effectiveTextScale,
                    onChange = { value -> update { it.copy(textScale = value) } }
                )
            }

            SettingsSection("Typeface") {
                ChoiceRow(
                    options = FontChoice.entries,
                    selected = settings.fontChoice,
                    label = { it.label },
                    onSelect = { choice -> update { it.copy(fontChoice = choice) } }
                )
                Description(settings.fontChoice.description)

                SwitchRow(
                    title = "Bold text",
                    description = "Thickens every letter in the app, not only headings.",
                    checked = settings.boldText,
                    onCheckedChange = { on -> update { it.copy(boldText = on) } }
                )
            }

            SettingsSection("Spacing between lines") {
                ChoiceRow(
                    options = LineSpacing.entries,
                    selected = settings.lineSpacing,
                    label = { it.label },
                    onSelect = { choice -> update { it.copy(lineSpacing = choice) } }
                )
                Description("More space between lines makes it easier not to lose your place.")
            }

            SettingsSection("Spacing between letters") {
                ChoiceRow(
                    options = LetterSpacing.entries,
                    selected = settings.letterSpacing,
                    label = { it.label },
                    onSelect = { choice -> update { it.copy(letterSpacing = choice) } }
                )
                Description("Wider letters can make crowded words easier to tell apart.")
            }

            SettingsSection("Colours") {
                ChoiceRow(
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    label = { it.label },
                    onSelect = { choice -> update { it.copy(themeMode = choice) } }
                )
                Description(settings.themeMode.description)

                Spacer(Modifier.size(dimens.itemSpacing))
                Text(
                    text = "Highlight colour",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = dimens.screenPadding)
                )
                Spacer(Modifier.size(dimens.itemSpacing / 2))
                AccentRow(
                    selected = settings.accentColor,
                    dark = settings.themeMode != ThemeMode.Light,
                    onSelect = { choice -> update { it.copy(accentColor = choice) } }
                )

                SwitchRow(
                    title = "Stronger contrast",
                    description = "Pushes text and background further apart, and darkens " +
                        "the quiet grey text the forum uses for dates and counts.",
                    checked = settings.highContrast,
                    onCheckedChange = { on -> update { it.copy(highContrast = on) } }
                )
            }

            SettingsSection("Spacing and targets") {
                ChoiceRow(
                    options = UiDensity.entries,
                    selected = settings.density,
                    label = { it.label },
                    onSelect = { choice -> update { it.copy(density = choice) } }
                )
                Description("How much room sits around and between things.")

                SwitchRow(
                    title = "Bigger tap targets",
                    description = "Makes buttons and rows taller so they are easier to hit.",
                    checked = settings.largeTapTargets,
                    onCheckedChange = { on -> update { it.copy(largeTapTargets = on) } }
                )
            }

            SettingsSection("Post length") {
                ChoiceRow(
                    options = PostLength.entries,
                    selected = settings.postLength,
                    label = { it.label },
                    onSelect = { choice -> update { it.copy(postLength = choice) } }
                )
                Description(
                    "Collapsing long posts puts a \"Show more\" button under them, so a " +
                        "single long post cannot swallow the whole thread."
                )
            }

            SettingsSection("Pictures and motion") {
                SwitchRow(
                    title = "Show pictures",
                    description = "Turning this off hides images, emoji and avatars until " +
                        "you tap them. Useful on a slow connection or when images are a distraction.",
                    checked = settings.showImages,
                    onCheckedChange = { on -> update { it.copy(showImages = on) } }
                )
                SwitchRow(
                    title = "Show avatars",
                    description = "Replaces profile pictures with a plain initial.",
                    checked = settings.showAvatars,
                    onCheckedChange = { on -> update { it.copy(showAvatars = on) } }
                )
                SwitchRow(
                    title = "Reduce motion",
                    description = "Turns off fades and sliding animations.",
                    checked = settings.reduceMotion,
                    onCheckedChange = { on -> update { it.copy(reduceMotion = on) } }
                )
            }

            SettingsSection("Reading") {
                SwitchRow(
                    title = "Underline links",
                    description = "Marks links with a line as well as a colour, so they do " +
                        "not rely on colour alone.",
                    checked = settings.underlineLinks,
                    onCheckedChange = { on -> update { it.copy(underlineLinks = on) } }
                )
                SwitchRow(
                    title = "Always show full dates",
                    description = "Shows \"4 Mar 2026 at 14:30\" instead of \"3d\".",
                    checked = settings.absoluteDates,
                    onCheckedChange = { on -> update { it.copy(absoluteDates = on) } }
                )
                SwitchRow(
                    title = "Open quotes automatically",
                    description = "Quoted posts start expanded instead of folded away.",
                    checked = settings.showQuotesExpanded,
                    onCheckedChange = { on -> update { it.copy(showQuotesExpanded = on) } }
                )
                SwitchRow(
                    title = "Keep the screen on",
                    description = "Stops the screen dimming while you are reading a topic.",
                    checked = settings.keepScreenOn,
                    onCheckedChange = { on -> update { it.copy(keepScreenOn = on) } }
                )
            }

            Spacer(Modifier.size(dimens.sectionSpacing * 2))
        }
    }

    if (confirmingReset) {
        AlertDialog(
            onDismissRequest = { confirmingReset = false },
            title = { Text("Put everything back?") },
            text = { Text("Every option on this page returns to how it started.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingReset = false
                    Forum.settings.resetToDefaults()
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingReset = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * A sample of the app's own text, drawn with the live theme.
 *
 * Choosing a text size from a number is guesswork; choosing it from a paragraph that looks
 * like the thing you will actually read is not.
 */
@Composable
private fun PreviewCard() {
    val dimens = LocalDimens.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(dimens.corner),
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.screenPadding)
    ) {
        Column(modifier = Modifier.padding(dimens.cardPadding)) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(dimens.itemSpacing / 2))
            Text(
                text = "Wii U servers back online",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = "Everything should be working again. Thank you for your patience " +
                    "while we sorted it out, and let us know in the replies if anything " +
                    "still looks wrong on your console.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.size(dimens.itemSpacing / 2))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Announcements",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "12 replies · 2h",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TextScaleControl(value: Float, onChange: (Float) -> Unit) {
    val dimens = LocalDimens.current
    val percent = (value * 100).toInt()

    Column(modifier = Modifier.padding(horizontal = dimens.screenPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    onChange((value - STEP).coerceAtLeast(AccessibilitySettings.MIN_TEXT_SCALE))
                },
                modifier = Modifier.size(dimens.minTouchTarget)
            ) {
                Text("A", style = MaterialTheme.typography.bodySmall)
            }
            Slider(
                value = value,
                onValueChange = { onChange(roundToStep(it)) },
                valueRange = AccessibilitySettings.MIN_TEXT_SCALE..AccessibilitySettings.MAX_TEXT_SCALE,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Text size, $percent percent" }
            )
            IconButton(
                onClick = {
                    onChange((value + STEP).coerceAtMost(AccessibilitySettings.MAX_TEXT_SCALE))
                },
                modifier = Modifier.size(dimens.minTouchTarget)
            ) {
                Text("A", style = MaterialTheme.typography.headlineSmall)
            }
        }
        Text(
            text = "$percent% of the normal size",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private const val STEP = 0.05f

private fun roundToStep(value: Float): Float =
    ((value / STEP).toInt() * STEP).coerceIn(
        AccessibilitySettings.MIN_TEXT_SCALE,
        AccessibilitySettings.MAX_TEXT_SCALE
    )

@Composable
private fun AccentRow(
    selected: AccentColor,
    dark: Boolean,
    onSelect: (AccentColor) -> Unit
) {
    val dimens = LocalDimens.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = dimens.screenPadding)
    ) {
        AccentColor.entries.forEach { accent ->
            val color = Color(0xFF000000L or if (dark) accent.onDark else accent.onLight)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(dimens.minTouchTarget)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelect(accent) }
                    .semantics { contentDescription = accent.label }
            ) {
                if (accent == selected) {
                    Icon(
                        imageVector = PretendoIcons.Check,
                        contentDescription = null,
                        tint = if (dark) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val dimens = LocalDimens.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                start = dimens.screenPadding,
                end = dimens.screenPadding,
                top = dimens.sectionSpacing,
                bottom = dimens.itemSpacing
            )
        )
        content()
        Spacer(Modifier.size(dimens.itemSpacing))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    val dimens = LocalDimens.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = dimens.screenPadding)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                modifier = Modifier.heightIn(min = dimens.minTouchTarget - 8.dp)
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val dimens = LocalDimens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .heightIn(min = dimens.minTouchTarget)
            .padding(horizontal = dimens.screenPadding, vertical = dimens.itemSpacing)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.size(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(dimens.itemSpacing))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun Description(text: String) {
    val dimens = LocalDimens.current
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = dimens.screenPadding,
            end = dimens.screenPadding,
            top = dimens.itemSpacing / 2
        )
    )
}
