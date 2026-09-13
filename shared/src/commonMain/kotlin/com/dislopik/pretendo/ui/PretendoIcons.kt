package com.dislopik.pretendo.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The app's icon set, drawn in code.
 *
 * Compose Multiplatform does not ship Material's icon packs, and the standalone artifacts
 * are no longer maintained, so the two dozen glyphs this app needs are defined here. They
 * are stroked rather than filled, which keeps each one to a few readable path commands and
 * keeps the whole set visually consistent. `Icon` tints them, so the colour below is only
 * a placeholder.
 */
object PretendoIcons {


    val ArrowBack = stroked("ArrowBack") {
        moveTo(20f, 12f); lineTo(4f, 12f)
        moveTo(10f, 6f); lineTo(4f, 12f); lineTo(10f, 18f)
    }

    val ArrowForward = stroked("ArrowForward") {
        moveTo(4f, 12f); lineTo(20f, 12f)
        moveTo(14f, 6f); lineTo(20f, 12f); lineTo(14f, 18f)
    }

    val ChevronRight = stroked("ChevronRight") {
        moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f)
    }

    val ChevronDown = stroked("ChevronDown") {
        moveTo(5f, 9f); lineTo(12f, 16f); lineTo(19f, 9f)
    }

    val ChevronUp = stroked("ChevronUp") {
        moveTo(5f, 15f); lineTo(12f, 8f); lineTo(19f, 15f)
    }

    val Close = stroked("Close") {
        moveTo(6f, 6f); lineTo(18f, 18f)
        moveTo(18f, 6f); lineTo(6f, 18f)
    }

    val Check = stroked("Check") {
        moveTo(5f, 13f); lineTo(10f, 18f); lineTo(19f, 6f)
    }

    val MoreVert = filled("MoreVert") {
        dot(12f, 5f, 1.7f); dot(12f, 12f, 1.7f); dot(12f, 19f, 1.7f)
    }


    val Home = stroked("Home") {
        moveTo(3f, 11f); lineTo(12f, 3.5f); lineTo(21f, 11f)
        moveTo(5.5f, 10f); lineTo(5.5f, 20.5f); lineTo(18.5f, 20.5f); lineTo(18.5f, 10f)
    }

    val Categories = stroked("Categories") {
        moveTo(4f, 6f); lineTo(20f, 6f)
        moveTo(4f, 12f); lineTo(20f, 12f)
        moveTo(4f, 18f); lineTo(20f, 18f)
    }

    val Search = stroked("Search") {
        circle(11f, 11f, 6.5f)
        moveTo(15.8f, 15.8f); lineTo(21f, 21f)
    }

    val Bell = stroked("Bell") {
        moveTo(6f, 18f)
        lineTo(18f, 18f)
        moveTo(6f, 18f); lineTo(6f, 10.5f)
        arcTo(6f, 6f, 0f, false, true, 18f, 10.5f)
        lineTo(18f, 18f)
        moveTo(10f, 21f); lineTo(14f, 21f)
    }

    val Person = stroked("Person") {
        circle(12f, 8f, 3.6f)
        moveTo(4.8f, 20.5f)
        curveTo(4.8f, 16.4f, 8f, 14.2f, 12f, 14.2f)
        curveTo(16f, 14.2f, 19.2f, 16.4f, 19.2f, 20.5f)
    }

    val Mail = stroked("Mail") {
        roundedRect(3f, 5f, 21f, 19f, 2f)
        moveTo(3.5f, 6f); lineTo(12f, 13f); lineTo(20.5f, 6f)
    }


    val Add = stroked("Add") {
        moveTo(12f, 5f); lineTo(12f, 19f)
        moveTo(5f, 12f); lineTo(19f, 12f)
    }

    val Send = stroked("Send") {
        moveTo(21f, 3f); lineTo(3f, 11f); lineTo(10.5f, 13.5f); lineTo(21f, 3f)
        moveTo(21f, 3f); lineTo(13.5f, 21f); lineTo(10.5f, 13.5f)
    }

    val Reply = stroked("Reply") {
        moveTo(10f, 7f); lineTo(4f, 12f); lineTo(10f, 17f)
        moveTo(4f, 12f); lineTo(13f, 12f)
        arcTo(5.5f, 5.5f, 0f, false, true, 18.5f, 17.5f)
        lineTo(18.5f, 19.5f)
    }

    val Heart = stroked("Heart") {
        moveTo(12f, 20.5f)
        curveTo(12f, 20.5f, 3f, 15f, 3f, 8.8f)
        curveTo(3f, 5.9f, 5.2f, 4f, 7.6f, 4f)
        curveTo(9.5f, 4f, 11.2f, 5.2f, 12f, 6.8f)
        curveTo(12.8f, 5.2f, 14.5f, 4f, 16.4f, 4f)
        curveTo(18.8f, 4f, 21f, 5.9f, 21f, 8.8f)
        curveTo(21f, 15f, 12f, 20.5f, 12f, 20.5f)
        close()
    }

    val HeartFilled = filled("HeartFilled") {
        moveTo(12f, 20.5f)
        curveTo(12f, 20.5f, 3f, 15f, 3f, 8.8f)
        curveTo(3f, 5.9f, 5.2f, 4f, 7.6f, 4f)
        curveTo(9.5f, 4f, 11.2f, 5.2f, 12f, 6.8f)
        curveTo(12.8f, 5.2f, 14.5f, 4f, 16.4f, 4f)
        curveTo(18.8f, 4f, 21f, 5.9f, 21f, 8.8f)
        curveTo(21f, 15f, 12f, 20.5f, 12f, 20.5f)
        close()
    }

    val Bookmark = stroked("Bookmark") {
        moveTo(6f, 4.5f); lineTo(18f, 4.5f); lineTo(18f, 20.5f)
        lineTo(12f, 16.2f); lineTo(6f, 20.5f); close()
    }

    val BookmarkFilled = filled("BookmarkFilled") {
        moveTo(6f, 4.5f); lineTo(18f, 4.5f); lineTo(18f, 20.5f)
        lineTo(12f, 16.2f); lineTo(6f, 20.5f); close()
    }

    val Share = stroked("Share") {
        moveTo(12f, 3.5f); lineTo(12f, 15f)
        moveTo(8f, 7.5f); lineTo(12f, 3.5f); lineTo(16f, 7.5f)
        moveTo(5.5f, 13f); lineTo(5.5f, 20.5f); lineTo(18.5f, 20.5f); lineTo(18.5f, 13f)
    }

    val Edit = stroked("Edit") {
        moveTo(4f, 20f); lineTo(8f, 20f); lineTo(20f, 8f); lineTo(16f, 4f); lineTo(4f, 16f); close()
        moveTo(14.5f, 5.5f); lineTo(18.5f, 9.5f)
    }

    val Delete = stroked("Delete") {
        moveTo(4f, 7f); lineTo(20f, 7f)
        moveTo(6.5f, 7f); lineTo(6.5f, 20.5f); lineTo(17.5f, 20.5f); lineTo(17.5f, 7f)
        moveTo(9.5f, 4f); lineTo(14.5f, 4f)
    }

    val Flag = stroked("Flag") {
        moveTo(5f, 3f); lineTo(5f, 21f)
        moveTo(5f, 4.5f); lineTo(18f, 4.5f); lineTo(15.5f, 9f); lineTo(18f, 13.5f); lineTo(5f, 13.5f)
    }

    val Refresh = stroked("Refresh") {
        moveTo(20f, 12f)
        arcTo(8f, 8f, 0f, true, true, 17.2f, 6.1f)
        moveTo(20.5f, 3f); lineTo(20.5f, 7f); lineTo(16.5f, 7f)
    }

    val Logout = stroked("Logout") {
        moveTo(13f, 3.5f); lineTo(4.5f, 3.5f); lineTo(4.5f, 20.5f); lineTo(13f, 20.5f)
        moveTo(10f, 12f); lineTo(21f, 12f)
        moveTo(17f, 8f); lineTo(21f, 12f); lineTo(17f, 16f)
    }

    val Login = stroked("Login") {
        moveTo(11f, 3.5f); lineTo(19.5f, 3.5f); lineTo(19.5f, 20.5f); lineTo(11f, 20.5f)
        moveTo(3f, 12f); lineTo(14f, 12f)
        moveTo(10f, 8f); lineTo(14f, 12f); lineTo(10f, 16f)
    }


    val Comment = stroked("Comment") {
        moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 16f); lineTo(11f, 16f)
        lineTo(6.5f, 20f); lineTo(6.5f, 16f); lineTo(4f, 16f); close()
    }

    val Eye = stroked("Eye") {
        moveTo(2.5f, 12f)
        curveTo(5f, 7f, 8.5f, 5f, 12f, 5f)
        curveTo(15.5f, 5f, 19f, 7f, 21.5f, 12f)
        curveTo(19f, 17f, 15.5f, 19f, 12f, 19f)
        curveTo(8.5f, 19f, 5f, 17f, 2.5f, 12f)
        close()
        circle(12f, 12f, 3.2f)
    }

    val Lock = stroked("Lock") {
        roundedRect(5f, 10.5f, 19f, 20.5f, 2f)
        moveTo(8.2f, 10.5f); lineTo(8.2f, 7.5f)
        arcTo(3.8f, 3.8f, 0f, false, true, 15.8f, 7.5f)
        lineTo(15.8f, 10.5f)
    }

    val Pin = stroked("Pin") {
        moveTo(12f, 14.5f); lineTo(12f, 21f)
        moveTo(8f, 3.5f); lineTo(16f, 3.5f)
        moveTo(9.5f, 3.5f); lineTo(9f, 10f); lineTo(6.5f, 12f); lineTo(6.5f, 14.5f)
        lineTo(17.5f, 14.5f); lineTo(17.5f, 12f); lineTo(15f, 10f); lineTo(14.5f, 3.5f)
    }

    val Star = stroked("Star") {
        moveTo(12f, 3.5f); lineTo(14.6f, 9.2f); lineTo(20.8f, 10f)
        lineTo(16.2f, 14.2f); lineTo(17.4f, 20.4f); lineTo(12f, 17.4f)
        lineTo(6.6f, 20.4f); lineTo(7.8f, 14.2f); lineTo(3.2f, 10f)
        lineTo(9.4f, 9.2f); close()
    }

    val Flame = stroked("Flame") {
        moveTo(12f, 21f)
        curveTo(8f, 21f, 5.5f, 18.4f, 5.5f, 15f)
        curveTo(5.5f, 10.5f, 10.5f, 9f, 10f, 3f)
        curveTo(14.5f, 5.5f, 18.5f, 9.5f, 18.5f, 15f)
        curveTo(18.5f, 18.4f, 16f, 21f, 12f, 21f)
        close()
    }

    val Info = stroked("Info") {
        circle(12f, 12f, 8.5f)
        moveTo(12f, 11f); lineTo(12f, 16.5f)
        moveTo(12f, 7.6f); lineTo(12f, 8.2f)
    }

    val Warning = stroked("Warning") {
        moveTo(12f, 3.5f); lineTo(21.5f, 20f); lineTo(2.5f, 20f); close()
        moveTo(12f, 9.5f); lineTo(12f, 14.5f)
        moveTo(12f, 17f); lineTo(12f, 17.6f)
    }

    val Image = stroked("Image") {
        roundedRect(3.5f, 4.5f, 20.5f, 19.5f, 2f)
        moveTo(3.5f, 16f); lineTo(9f, 11f); lineTo(14f, 15.5f); lineTo(17f, 13f); lineTo(20.5f, 16.5f)
        circle(15.5f, 8.5f, 1.6f)
    }

    val Play = filled("Play") {
        moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close()
    }


    val Settings = stroked("Settings") {
        moveTo(4f, 7f); lineTo(20f, 7f)
        moveTo(4f, 12f); lineTo(20f, 12f)
        moveTo(4f, 17f); lineTo(20f, 17f)
        circle(9f, 7f, 2.2f)
        circle(15f, 12f, 2.2f)
        circle(8f, 17f, 2.2f)
    }

    /** The accessibility page's own glyph: a letter A with a bar. */
    val TextSize = stroked("TextSize") {
        moveTo(4f, 20f); lineTo(11f, 5f); lineTo(18f, 20f)
        moveTo(7f, 15f); lineTo(15f, 15f)
        moveTo(19f, 12f); lineTo(22f, 12f)
    }

    val Contrast = stroked("Contrast") {
        circle(12f, 12f, 8.5f)
        moveTo(12f, 3.5f)
        arcTo(8.5f, 8.5f, 0f, false, true, 12f, 20.5f)
        close()
    }

    val Palette = stroked("Palette") {
        circle(12f, 12f, 8.5f)
        dot(9f, 9f, 1.3f); dot(15f, 9f, 1.3f); dot(8.5f, 14.5f, 1.3f); dot(14.5f, 15f, 1.3f)
    }

    val Motion = stroked("Motion") {
        moveTo(3f, 12f); lineTo(9f, 12f)
        moveTo(12f, 6f); lineTo(21f, 6f)
        moveTo(12f, 18f); lineTo(21f, 18f)
        circle(11f, 12f, 2.4f)
    }


    private const val SIZE = 24f

    private fun stroked(name: String, build: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = SIZE,
            viewportHeight = SIZE
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = build
            )
        }.build()

    private fun filled(name: String, build: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = SIZE,
            viewportHeight = SIZE
        ).apply {
            path(fill = SolidColor(Color.Black), pathBuilder = build)
        }.build()
}

/** A full circle, as two half arcs, because the path DSL has no circle primitive. */
private fun PathBuilder.circle(cx: Float, cy: Float, radius: Float) {
    moveTo(cx - radius, cy)
    arcTo(radius, radius, 0f, false, true, cx + radius, cy)
    arcTo(radius, radius, 0f, false, true, cx - radius, cy)
    close()
}

private fun PathBuilder.dot(cx: Float, cy: Float, radius: Float) = circle(cx, cy, radius)

private fun PathBuilder.roundedRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float
) {
    moveTo(left + radius, top)
    lineTo(right - radius, top)
    arcTo(radius, radius, 0f, false, true, right, top + radius)
    lineTo(right, bottom - radius)
    arcTo(radius, radius, 0f, false, true, right - radius, bottom)
    lineTo(left + radius, bottom)
    arcTo(radius, radius, 0f, false, true, left, bottom - radius)
    lineTo(left, top + radius)
    arcTo(radius, radius, 0f, false, true, left + radius, top)
    close()
}
