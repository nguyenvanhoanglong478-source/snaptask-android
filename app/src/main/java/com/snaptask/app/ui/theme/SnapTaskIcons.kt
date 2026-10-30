package com.snaptask.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Unified 24x24 vector icon set optically centered around (12, 12) for fixed-size controls.
object SnapTaskIcons {

    private fun createIcon(
        name: String,
        strokeWidth: Float = 2.0f,
        builder: PathBuilder.() -> Unit
    ): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            builder()
        }.build()
    }

    private fun PathBuilder.addRoundedRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
        moveTo(x + r, y)
        lineTo(x + w - r, y)
        arcTo(r, r, 0f, false, true, x + w, y + r)
        lineTo(x + w, y + h - r)
        arcTo(r, r, 0f, false, true, x + w - r, y + h)
        lineTo(x + r, y + h)
        arcTo(r, r, 0f, false, true, x, y + h - r)
        lineTo(x, y + r)
        arcTo(r, r, 0f, false, true, x + r, y)
        close()
    }

    private fun PathBuilder.addCircle(cx: Float, cy: Float, r: Float) {
        moveTo(cx, cy - r)
        arcTo(r, r, 0f, true, true, cx - 0.01f, cy - r)
        close()
    }

    val Today = createIcon("Today") {
        addRoundedRect(3.5f, 3.5f, 17f, 17f, 4.5f)
        moveTo(8.5f, 12.2f)
        lineTo(10.9f, 14.6f)
        lineTo(15.5f, 9.8f)
    }

    val Calendar = createIcon("Calendar") {
        addRoundedRect(3.5f, 4.5f, 17f, 16f, 4f)
        moveTo(16f, 3f)
        lineTo(16f, 6f)
        moveTo(8f, 3f)
        lineTo(8f, 6f)
        moveTo(3.5f, 9.5f)
        lineTo(20.5f, 9.5f)
    }

    val HeatMap = createIcon("HeatMap", strokeWidth = 1.8f) {
        val coords = listOf(3.5f, 9.7f, 15.9f)
        for (ry in coords) {
            for (rx in coords) {
                addRoundedRect(rx, ry, 4.6f, 4.6f, 1.2f)
            }
        }
    }

    val Board = createIcon("Board") {
        addRoundedRect(3.5f, 4f, 4.5f, 16f, 2f)
        addRoundedRect(9.75f, 4f, 4.5f, 11f, 2f)
        addRoundedRect(16f, 4f, 4.5f, 7f, 2f)
    }

    val Folder = createIcon("Folder") {
        moveTo(3.5f, 7.5f)
        arcTo(2f, 2f, 0f, false, true, 5.5f, 5.5f)
        lineTo(9.3f, 5.5f)
        lineTo(11.3f, 7.5f)
        lineTo(18.5f, 7.5f)
        arcTo(2f, 2f, 0f, false, true, 20.5f, 9.5f)
        lineTo(20.5f, 17.5f)
        arcTo(2f, 2f, 0f, false, true, 18.5f, 19.5f)
        lineTo(5.5f, 19.5f)
        arcTo(2f, 2f, 0f, false, true, 3.5f, 17.5f)
        close()
    }

    val Home = createIcon("Home") {
        moveTo(3.5f, 10.5f)
        lineTo(12f, 3.5f)
        lineTo(20.5f, 10.5f)
        lineTo(20.5f, 19.5f)
        arcTo(1.5f, 1.5f, 0f, false, true, 19f, 21f)
        lineTo(14.5f, 21f)
        lineTo(14.5f, 15f)
        lineTo(9.5f, 15f)
        lineTo(9.5f, 21f)
        lineTo(5f, 21f)
        arcTo(1.5f, 1.5f, 0f, false, true, 3.5f, 19.5f)
        close()
    }

    val Briefcase = createIcon("Briefcase") {
        addRoundedRect(3.5f, 7f, 17f, 13f, 2.2f)
        moveTo(8.5f, 7f)
        lineTo(8.5f, 5f)
        arcTo(2f, 2f, 0f, false, true, 10.5f, 3f)
        lineTo(13.5f, 3f)
        arcTo(2f, 2f, 0f, false, true, 15.5f, 5f)
        lineTo(15.5f, 7f)
        moveTo(3.5f, 12f)
        lineTo(20.5f, 12f)
    }

    val Heart = createIcon("Heart") {
        moveTo(12f, 20.2f)
        lineTo(4.8f, 13f)
        arcTo(4.6f, 4.6f, 0f, false, true, 11.4f, 6.6f)
        lineTo(12f, 7.3f)
        lineTo(12.6f, 6.6f)
        arcTo(4.6f, 4.6f, 0f, false, true, 19.2f, 13f)
        close()
    }

    val Wrench = createIcon("Wrench") {
        moveTo(14.7f, 6.3f)
        arcTo(4.5f, 4.5f, 0f, false, false, 8.8f, 12.2f)
        lineTo(4f, 17f)
        lineTo(7f, 20f)
        lineTo(11.8f, 15.2f)
        arcTo(4.5f, 4.5f, 0f, false, false, 17.7f, 9.3f)
        lineTo(15.1f, 11.9f)
        lineTo(12.1f, 8.9f)
        close()
    }

    val Cart = createIcon("Cart") {
        addCircle(9f, 20f, 1.5f)
        addCircle(17f, 20f, 1.5f)
        moveTo(3f, 4f)
        lineTo(5.2f, 4f)
        lineTo(7.6f, 15.2f)
        arcTo(2f, 2f, 0f, false, false, 9.6f, 16.8f)
        lineTo(17.4f, 16.8f)
        arcTo(2f, 2f, 0f, false, false, 19.4f, 15.2f)
        lineTo(21f, 7f)
        lineTo(6f, 7f)
    }

    val Leaf = createIcon("Leaf") {
        moveTo(5f, 19f)
        arcTo(13f, 13f, 0f, false, false, 19f, 5f)
        arcTo(13f, 13f, 0f, false, false, 5f, 19f)
        moveTo(5f, 19f)
        lineTo(12.5f, 11.5f)
    }

    val Book = createIcon("Book") {
        moveTo(4f, 19.5f)
        arcTo(2.5f, 2.5f, 0f, false, true, 6.5f, 17f)
        lineTo(20f, 17f)
        moveTo(4f, 19.5f)
        lineTo(4f, 5.5f)
        arcTo(2.5f, 2.5f, 0f, false, true, 6.5f, 3f)
        lineTo(20f, 3f)
        lineTo(20f, 20f)
        lineTo(6.5f, 20f)
        arcTo(2.5f, 2.5f, 0f, false, true, 4f, 19.5f)
    }

    val Bike = createIcon("Bike") {
        addCircle(5.5f, 17.5f, 3.3f)
        addCircle(18.5f, 17.5f, 3.3f)
        moveTo(15f, 6f)
        lineTo(12f, 6f)
        lineTo(9f, 17.5f)
        moveTo(5.5f, 17.5f)
        lineTo(11f, 11f)
        lineTo(16f, 11f)
        lineTo(18.5f, 17.5f)
    }

    val Clock = createIcon("Clock") {
        addCircle(12f, 12f, 8.8f)
        moveTo(12f, 7.2f)
        lineTo(12f, 12f)
        lineTo(15.2f, 14f)
    }

    val Repeat = createIcon("Repeat") {
        moveTo(17f, 2.5f)
        lineTo(20f, 5.5f)
        lineTo(17f, 8.5f)
        moveTo(4f, 11f)
        lineTo(4f, 9.5f)
        arcTo(4f, 4f, 0f, false, true, 8f, 5.5f)
        lineTo(20f, 5.5f)
        moveTo(7f, 21.5f)
        lineTo(4f, 18.5f)
        lineTo(7f, 15.5f)
        moveTo(20f, 13f)
        lineTo(20f, 14.5f)
        arcTo(4f, 4f, 0f, false, true, 16f, 18.5f)
        lineTo(4f, 18.5f)
    }

    val TextAa = createIcon("TextAa") {
        moveTo(3.8f, 18f)
        lineTo(8.3f, 6f)
        lineTo(12.8f, 18f)
        moveTo(5.5f, 14f)
        lineTo(11.1f, 14f)
        moveTo(15.2f, 12.2f)
        arcTo(2.4f, 2.4f, 0f, false, true, 20f, 12.6f)
        lineTo(20f, 18f)
        addRoundedRect(14.8f, 14.4f, 5.2f, 3.6f, 1.8f)
    }

    val Flame = createIcon("Flame") {
        moveTo(12f, 21.5f)
        arcTo(6.8f, 6.8f, 0f, false, false, 18.8f, 14.7f)
        lineTo(15.2f, 5f)
        lineTo(12.8f, 10.5f)
        lineTo(10.2f, 6.5f)
        lineTo(5.2f, 14.7f)
        arcTo(6.8f, 6.8f, 0f, false, false, 12f, 21.5f)
    }

    val ShareUp = createIcon("ShareUp") {
        moveTo(12f, 15f)
        lineTo(12f, 3.5f)
        moveTo(7.2f, 8.2f)
        lineTo(12f, 3.5f)
        lineTo(16.8f, 8.2f)
        moveTo(5f, 13.5f)
        lineTo(5f, 18.5f)
        arcTo(2f, 2f, 0f, false, false, 7f, 20.5f)
        lineTo(17f, 20.5f)
        arcTo(2f, 2f, 0f, false, false, 19f, 18.5f)
        lineTo(19f, 13.5f)
    }

    val Download = createIcon("Download") {
        moveTo(12f, 4f)
        lineTo(12f, 15.5f)
        moveTo(7.2f, 11f)
        lineTo(12f, 15.8f)
        lineTo(16.8f, 11f)
        moveTo(5f, 20f)
        lineTo(19f, 20f)
    }

    val EditPen = createIcon("EditPen") {
        moveTo(4f, 20f)
        lineTo(8.2f, 20f)
        lineTo(19f, 9.2f)
        lineTo(14.8f, 5f)
        lineTo(4f, 15.8f)
        close()
        moveTo(13.2f, 6.6f)
        lineTo(17.4f, 10.8f)
    }

    val Trash = createIcon("Trash") {
        moveTo(4f, 7f)
        lineTo(20f, 7f)
        moveTo(9f, 7f)
        lineTo(9f, 4f)
        lineTo(15f, 4f)
        lineTo(15f, 7f)
        moveTo(6.2f, 7f)
        lineTo(7.1f, 19.2f)
        arcTo(2f, 2f, 0f, false, false, 9.1f, 21f)
        lineTo(14.9f, 21f)
        arcTo(2f, 2f, 0f, false, false, 16.9f, 19.2f)
        lineTo(17.8f, 7f)
    }

    val CompareArrows = createIcon("CompareArrows") {
        moveTo(9f, 7f)
        lineTo(4f, 12f)
        lineTo(9f, 17f)
        moveTo(15f, 7f)
        lineTo(20f, 12f)
        lineTo(15f, 17f)
    }

    val Sun = createIcon("Sun") {
        addCircle(12f, 12f, 4f)
        moveTo(12f, 2.2f); lineTo(12f, 4.2f)
        moveTo(12f, 19.8f); lineTo(12f, 21.8f)
        moveTo(2.2f, 12f); lineTo(4.2f, 12f)
        moveTo(19.8f, 12f); lineTo(21.8f, 12f)
        moveTo(5.1f, 5.1f); lineTo(6.5f, 6.5f)
        moveTo(17.5f, 17.5f); lineTo(18.9f, 18.9f)
        moveTo(5.1f, 18.9f); lineTo(6.5f, 17.5f)
        moveTo(17.5f, 6.5f); lineTo(18.9f, 5.1f)
    }

    val Moon = createIcon("Moon") {
        moveTo(19.8f, 14.5f)
        arcTo(8f, 8f, 0f, false, true, 9.5f, 4.2f)
        arcTo(8f, 8f, 0f, true, false, 19.8f, 14.5f)
        close()
    }

    val MoreHorizontal = createIcon("MoreHorizontal") {
        addCircle(5.5f, 12f, 1.3f)
        addCircle(12f, 12f, 1.3f)
        addCircle(18.5f, 12f, 1.3f)
    }

    val ChevronLeft = createIcon("ChevronLeft") {
        moveTo(14.5f, 6f)
        lineTo(8.5f, 12f)
        lineTo(14.5f, 18f)
    }

    val ChevronRight = createIcon("ChevronRight") {
        moveTo(9.5f, 6f)
        lineTo(15.5f, 12f)
        lineTo(9.5f, 18f)
    }

    val ChevronUp = createIcon("ChevronUp") {
        moveTo(6f, 14.5f)
        lineTo(12f, 8.5f)
        lineTo(18f, 14.5f)
    }

    val ChevronDown = createIcon("ChevronDown") {
        moveTo(6f, 9.5f)
        lineTo(12f, 15.5f)
        lineTo(18f, 9.5f)
    }

    val Pin = createIcon("Pin") {
        moveTo(9f, 4f); lineTo(15f, 4f)
        moveTo(12f, 4f); lineTo(12f, 11f)
        moveTo(7f, 11f); lineTo(17f, 11f)
        moveTo(9f, 11f); lineTo(7f, 16f); lineTo(17f, 16f); lineTo(15f, 11f)
        moveTo(12f, 16f); lineTo(12f, 21f)
    }

    val FlashOn = createIcon("FlashOn") {
        moveTo(8.8f, 3.0f)
        lineTo(15.6f, 3.0f)
        lineTo(13.4f, 9.2f)
        lineTo(18.0f, 9.2f)
        lineTo(9.2f, 21.0f)
        lineTo(10.0f, 13.8f)
        lineTo(6.4f, 13.8f)
        close()
    }

    val FlashOff = createIcon("FlashOff") {
        moveTo(8.56f, 4.06f)
        lineTo(8.8f, 3.0f)
        lineTo(15.6f, 3.0f)
        lineTo(13.6f, 9.0f)
        lineTo(18.0f, 9.0f)
        lineTo(16.1f, 11.6f)
        moveTo(6.96f, 11.46f)
        lineTo(6.4f, 14.0f)
        lineTo(9.5f, 14.0f)
        lineTo(8.8f, 21.0f)
        lineTo(12.14f, 16.64f)
        moveTo(3.8f, 3.8f)
        lineTo(20.2f, 20.2f)
    }

    val FlashAuto = createIcon("FlashAuto") {
        moveTo(7.4f, 3.0f)
        lineTo(13.8f, 3.0f)
        lineTo(11.6f, 9.2f)
        lineTo(15.8f, 9.2f)
        lineTo(7.8f, 20.8f)
        lineTo(8.6f, 13.6f)
        lineTo(5.0f, 13.6f)
        close()
        moveTo(16.2f, 20.8f)
        lineTo(18.6f, 14.5f)
        lineTo(21.0f, 20.8f)
        moveTo(16.9f, 18.9f)
        lineTo(20.3f, 18.9f)
    }

    val CameraFlip = createIcon("CameraFlip") {
        moveTo(19.8f, 10.5f)
        arcTo(7.8f, 7.8f, 0f, false, false, 5.8f, 6.2f)
        lineTo(4f, 8f)
        moveTo(4f, 4f); lineTo(4f, 8f); lineTo(8f, 8f)
        moveTo(4.2f, 13.5f)
        arcTo(7.8f, 7.8f, 0f, false, false, 18.2f, 17.8f)
        lineTo(20f, 16f)
        moveTo(20f, 20f); lineTo(20f, 16f); lineTo(16f, 16f)
    }

    val AlarmClock = createIcon("AlarmClock") {
        addCircle(12f, 12.8f, 7.5f)
        moveTo(12f, 9f); lineTo(12f, 12.8f); lineTo(14.8f, 14.2f)
        moveTo(4.5f, 4.5f); lineTo(6.8f, 6.5f)
        moveTo(19.5f, 4.5f); lineTo(17.2f, 6.5f)
    }

    val Bell = createIcon("Bell") {
        moveTo(6f, 10f)
        arcTo(6f, 6f, 0f, false, true, 18f, 10f)
        lineTo(18f, 14.5f)
        lineTo(20f, 17f)
        lineTo(4f, 17f)
        lineTo(6f, 14.5f)
        close()
        moveTo(10f, 17.5f)
        arcTo(2.2f, 2.2f, 0f, false, false, 14f, 17.5f)
    }

    val Camera = createIcon("Camera") {
        moveTo(3.5f, 9f)
        arcTo(2f, 2f, 0f, false, true, 5.5f, 7f)
        lineTo(8f, 7f)
        lineTo(9.6f, 4.8f)
        lineTo(14.4f, 4.8f)
        lineTo(16f, 7f)
        lineTo(18.5f, 7f)
        arcTo(2f, 2f, 0f, false, true, 20.5f, 9f)
        lineTo(20.5f, 17.5f)
        arcTo(2f, 2f, 0f, false, true, 18.5f, 19.5f)
        lineTo(5.5f, 19.5f)
        arcTo(2f, 2f, 0f, false, true, 3.5f, 17.5f)
        close()
        addCircle(12f, 13.2f, 3.6f)
    }

    val CircleOutline = createIcon("CircleOutline") {
        addCircle(12f, 12f, 8.5f)
    }

    val FlagPriority = createIcon("FlagPriority") {
        moveTo(5.5f, 20.5f)
        lineTo(5.5f, 4f)
        lineTo(16.5f, 4f)
        lineTo(14.5f, 8f)
        lineTo(16.5f, 12f)
        lineTo(5.5f, 12f)
    }

    val Check = createIcon("Check", strokeWidth = 2.4f) {
        moveTo(5f, 12.5f)
        lineTo(9.6f, 17.1f)
        lineTo(19f, 7.5f)
    }

    val Close = createIcon("Close") {
        moveTo(6f, 6f); lineTo(18f, 18f)
        moveTo(18f, 6f); lineTo(6f, 18f)
    }

    val Retake = createIcon("Retake") {
        moveTo(3.8f, 12f)
        arcTo(8.2f, 8.2f, 0f, true, false, 6.4f, 6f)
        moveTo(3.8f, 4.2f); lineTo(3.8f, 8.8f); lineTo(8.4f, 8.8f)
    }

    val Plus = createIcon("Plus", strokeWidth = 2.2f) {
        moveTo(12f, 5f); lineTo(12f, 19f)
        moveTo(5f, 12f); lineTo(19f, 12f)
    }

    fun folderIconByKey(key: String): ImageVector = when (key) {
        "home", "i-home" -> Home
        "brief", "i-brief" -> Briefcase
        "heart", "i-heart" -> Heart
        "wrench", "i-wrench" -> Wrench
        "cart", "i-cart" -> Cart
        "leaf", "i-leaf" -> Leaf
        "book", "i-book" -> Book
        "bike", "i-bike" -> Bike
        else -> Folder
    }
}
