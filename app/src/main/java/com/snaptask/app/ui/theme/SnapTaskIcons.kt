package com.snaptask.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

object SnapTaskIcons {

    private fun createIcon(name: String, builder: PathBuilder.() -> Unit): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = builder
        ).build()
    }

    // 1. Search (Magnifying Glass)
    val Search = createIcon("Search") {
        // Lens circle
        moveTo(10.5f, 4f)
        arcTo(6.5f, 6.5f, 0f, true, true, 10.49f, 4f)
        close()
        // Handle
        moveTo(15.2f, 15.2f)
        lineTo(20f, 20f)
    }

    // 2. Bento Grid (4 rounded squares)
    val GridBento = createIcon("GridBento") {
        // Top-Left
        moveTo(4f, 4f)
        lineTo(9f, 4f)
        lineTo(9f, 9f)
        lineTo(4f, 9f)
        close()
        // Top-Right
        moveTo(15f, 4f)
        lineTo(20f, 4f)
        lineTo(20f, 9f)
        lineTo(15f, 9f)
        close()
        // Bottom-Left
        moveTo(4f, 15f)
        lineTo(9f, 15f)
        lineTo(9f, 20f)
        lineTo(4f, 20f)
        close()
        // Bottom-Right
        moveTo(15f, 15f)
        lineTo(20f, 15f)
        lineTo(20f, 20f)
        lineTo(15f, 20f)
        close()
    }

    // 3. List Feed (2 horizontal cards)
    val ListFeed = createIcon("ListFeed") {
        // Card 1
        moveTo(4f, 5f)
        lineTo(20f, 5f)
        lineTo(20f, 10f)
        lineTo(4f, 10f)
        close()
        // Card 2
        moveTo(4f, 14f)
        lineTo(20f, 14f)
        lineTo(20f, 19f)
        lineTo(4f, 19f)
        close()
    }

    // 4. Pin (Pushpin)
    val Pin = createIcon("Pin") {
        moveTo(9f, 4f)
        lineTo(15f, 4f)
        moveTo(12f, 4f)
        lineTo(12f, 11f)
        moveTo(7f, 11f)
        lineTo(17f, 11f)
        moveTo(9f, 11f)
        lineTo(7f, 16f)
        lineTo(17f, 16f)
        lineTo(15f, 11f)
        moveTo(12f, 16f)
        lineTo(12f, 21f)
    }

    // 5. Flash On (Lightning bolt)
    val FlashOn = createIcon("FlashOn") {
        moveTo(13f, 2f)
        lineTo(6f, 13f)
        lineTo(12f, 13f)
        lineTo(11f, 22f)
        lineTo(18f, 11f)
        lineTo(12f, 11f)
        close()
    }

    // 6. Flash Off (Lightning bolt with slash)
    val FlashOff = createIcon("FlashOff") {
        moveTo(13f, 2f)
        lineTo(9.5f, 7.5f)
        moveTo(8f, 10f)
        lineTo(6f, 13f)
        lineTo(12f, 13f)
        lineTo(11.5f, 17f)
        moveTo(13.5f, 13.5f)
        lineTo(18f, 11f)
        lineTo(12f, 11f)
        lineTo(12.5f, 7f)
        moveTo(3f, 3f)
        lineTo(21f, 21f)
    }

    // 7. Flash Auto (Bolt + 'A')
    val FlashAuto = createIcon("FlashAuto") {
        moveTo(11f, 2f)
        lineTo(5f, 12f)
        lineTo(10f, 12f)
        lineTo(9f, 21f)
        lineTo(15f, 11f)
        lineTo(10f, 11f)
        close()
        // Letter A at top-right
        moveTo(17f, 7f)
        lineTo(19f, 2f)
        lineTo(21f, 7f)
        moveTo(17.5f, 5.5f)
        lineTo(20.5f, 5.5f)
    }

    // 8. Camera Flip (Switch Camera)
    val CameraFlip = createIcon("CameraFlip") {
        // Upper arc & arrow
        moveTo(20f, 10f)
        arcTo(8f, 8f, 0f, false, false, 6.5f, 5.5f)
        lineTo(4f, 6f)
        moveTo(4f, 2f)
        lineTo(4f, 6f)
        lineTo(8f, 6f)

        // Lower arc & arrow
        moveTo(4f, 14f)
        arcTo(8f, 8f, 0f, false, false, 17.5f, 18.5f)
        lineTo(20f, 18f)
        moveTo(20f, 22f)
        lineTo(20f, 18f)
        lineTo(16f, 18f)
    }

    // 9. Alarm Clock
    val AlarmClock = createIcon("AlarmClock") {
        // Clock face
        moveTo(12f, 5f)
        arcTo(8f, 8f, 0f, true, true, 11.99f, 5f)
        close()
        // Hands
        moveTo(12f, 9f)
        lineTo(12f, 13f)
        lineTo(15f, 13f)
        // Bells
        moveTo(4.5f, 4.5f)
        lineTo(6.5f, 6.5f)
        moveTo(19.5f, 4.5f)
        lineTo(17.5f, 6.5f)
        // Feet
        moveTo(6f, 19.5f)
        lineTo(4.5f, 21f)
        moveTo(18f, 19.5f)
        lineTo(19.5f, 21f)
    }

    // 10. Folder
    val Folder = createIcon("Folder") {
        moveTo(3f, 7f)
        lineTo(9f, 7f)
        lineTo(11f, 9f)
        lineTo(20f, 9f)
        lineTo(20f, 18f)
        lineTo(3f, 18f)
        close()
    }

    // 11. Flag Priority
    val FlagPriority = createIcon("FlagPriority") {
        moveTo(5f, 21f)
        lineTo(5f, 3f)
        moveTo(5f, 4f)
        lineTo(17f, 4f)
        lineTo(14.5f, 8.5f)
        lineTo(17f, 13f)
        lineTo(5f, 13f)
    }

    // 12. Check (Mark)
    val Check = createIcon("Check") {
        moveTo(4f, 12.5f)
        lineTo(9.5f, 18f)
        lineTo(20f, 6.5f)
    }

    // 13. Close (Cross X)
    val Close = createIcon("Close") {
        moveTo(5f, 5f)
        lineTo(19f, 19f)
        moveTo(19f, 5f)
        lineTo(5f, 19f)
    }

    // 14. Retake (Rotate reload)
    val Retake = createIcon("Retake") {
        moveTo(3f, 12f)
        arcTo(9f, 9f, 0f, true, false, 6f, 5.5f)
        moveTo(3f, 4f)
        lineTo(3f, 9f)
        lineTo(8f, 9f)
    }

    // 15. Plus (Add)
    val Plus = createIcon("Plus") {
        moveTo(12f, 5f)
        lineTo(12f, 19f)
        moveTo(5f, 12f)
        lineTo(19f, 12f)
    }

    // 16. Sort (Filter lines)
    val Sort = createIcon("Sort") {
        moveTo(4f, 6f)
        lineTo(20f, 6f)
        moveTo(4f, 12f)
        lineTo(15f, 12f)
        moveTo(4f, 18f)
        lineTo(10f, 18f)
    }
}
