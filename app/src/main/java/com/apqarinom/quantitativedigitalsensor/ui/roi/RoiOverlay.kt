package com.apqarinom.quantitativedigitalsensor.ui.roi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Interactive Composable ROI (Region of Interest) Overlay.
 *
 * INTERACTIVE GESTURE & POSITIONING EXPLANATION:
 * -------------------------------------------------------------------------------------------------
 * 1. Interactive Gestures:
 *    Uses [detectTransformGestures] within [pointerInput] to capture pan (drag) and zoom (pinch) gestures.
 *    - Dragging translates the ROI box offset (`panOffset`) across the 2D viewport.
 *    - Pinching resizes the ROI box (`scaleFactor`) proportionally within safe boundary limits.
 *
 * 2. Boundary Clamping:
 *    The ROI box bounds are strictly clamped so the box never leaves the visible screen.
 *    The scale factor is constrained (0.4x to 1.8x base size) to prevent the ROI box
 *    from becoming too small to align or overflowing the display viewport.
 *
 * 3. Dynamic Masking & Overlay Alignment:
 *    - Scrim mask (darkened overlay with EvenOdd fill type) updates dynamically to cut out
 *      the active ROI rectangle.
 *    - Bounding borders, corner reticle brackets, center target crosshair, and instructional label
 *      dynamically scale and shift to continuously track the ROI position.
 * -------------------------------------------------------------------------------------------------
 */
@Composable
fun RoiOverlay(
    modifier: Modifier = Modifier,
    roiWidthFraction: Float = 0.70f,
    roiAspectRatio: Float = 1.0f, // 1:1 square by default for pH sensor film
    scrimColor: Color = Color.Black.copy(alpha = 0.55f),
    borderColor: Color = Color(0xFF00E676), // Vibrant indicator green
    cornerLengthDp: Dp = 24.dp,
    strokeWidthDp: Dp = 3.dp,
    onRoiBoundsChanged: (Rect) -> Unit = {}
) {
    // Interactive Gestures State
    var scaleFactor by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val density = LocalDensity.current

    // State holding computed ROI rect for label positioning & callbacks
    var currentRoiRect by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Apply zoom scale factor within safe limits (0.4f to 1.8f)
                    scaleFactor = (scaleFactor * zoom).coerceIn(0.4f, 1.8f)
                    // Apply pan drag offset
                    panOffset += pan
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate Base ROI dimensions
            val baseWidth = canvasWidth * roiWidthFraction.coerceIn(0.2f, 0.95f)

            // Apply interactive scale within safe bounds relative to canvas size
            val minDimension = minOf(canvasWidth, canvasHeight)
            val roiWidth = (baseWidth * scaleFactor).coerceIn(120f, minDimension * 0.92f)
            val roiHeight = roiWidth / roiAspectRatio

            // Calculate center point with pan offset clamped strictly within screen bounds
            val defaultCenterX = canvasWidth / 2f
            val defaultCenterY = canvasHeight / 2f

            val targetCenterX = defaultCenterX + panOffset.x
            val targetCenterY = defaultCenterY + panOffset.y

            val halfWidth = roiWidth / 2f
            val halfHeight = roiHeight / 2f

            val clampedCenterX = targetCenterX.coerceIn(halfWidth, canvasWidth - halfWidth)
            val clampedCenterY = targetCenterY.coerceIn(halfHeight, canvasHeight - halfHeight)

            val left = clampedCenterX - halfWidth
            val top = clampedCenterY - halfHeight
            val right = left + roiWidth
            val bottom = top + roiHeight

            val roiRect = Rect(left, top, right, bottom)
            currentRoiRect = roiRect
            onRoiBoundsChanged(roiRect)

            // 1. Draw semi-transparent dark mask with dynamic hole over ROI area
            val maskPath = Path().apply {
                fillType = PathFillType.EvenOdd
                // Outer canvas rectangle
                addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
                // Inner ROI cutout rectangle
                addRoundRect(
                    RoundRect(
                        rect = roiRect,
                        cornerRadius = CornerRadius(16f, 16f)
                    )
                )
            }
            drawPath(path = maskPath, color = scrimColor)

            // 2. Draw ROI Bounding Box Outline
            val strokeWidthPx = strokeWidthDp.toPx()
            drawRoundRect(
                color = borderColor.copy(alpha = 0.85f),
                topLeft = Offset(left, top),
                size = Size(roiWidth, roiHeight),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = strokeWidthPx)
            )

            // 3. Draw Corner Reticle Brackets (dynamically scaling corner length)
            val dynamicCornerLengthPx = (cornerLengthDp.toPx() * (roiWidth / baseWidth).coerceIn(0.7f, 1.4f))
            val bracketColor = Color.White

            // Top-Left Corner
            drawLine(bracketColor, Offset(left - 4f, top), Offset(left + dynamicCornerLengthPx, top), strokeWidthPx * 1.5f, StrokeCap.Round)
            drawLine(bracketColor, Offset(left, top - 4f), Offset(left, top + dynamicCornerLengthPx), strokeWidthPx * 1.5f, StrokeCap.Round)

            // Top-Right Corner
            drawLine(bracketColor, Offset(right + 4f, top), Offset(right - dynamicCornerLengthPx, top), strokeWidthPx * 1.5f, StrokeCap.Round)
            drawLine(bracketColor, Offset(right, top - 4f), Offset(right, top - dynamicCornerLengthPx), strokeWidthPx * 1.5f, StrokeCap.Round)

            // Bottom-Left Corner
            drawLine(bracketColor, Offset(left - 4f, bottom), Offset(left + dynamicCornerLengthPx, bottom), strokeWidthPx * 1.5f, StrokeCap.Round)
            drawLine(bracketColor, Offset(left, bottom + 4f), Offset(left, bottom + dynamicCornerLengthPx), strokeWidthPx * 1.5f, StrokeCap.Round)

            // Bottom-Right Corner
            drawLine(bracketColor, Offset(right + 4f, bottom), Offset(right - dynamicCornerLengthPx, bottom), strokeWidthPx * 1.5f, StrokeCap.Round)
            drawLine(bracketColor, Offset(right, bottom + 4f), Offset(right, bottom - dynamicCornerLengthPx), strokeWidthPx * 1.5f, StrokeCap.Round)

            // 4. Center Target Dot
            drawCircle(
                color = borderColor,
                radius = 4f,
                center = Offset(clampedCenterX, clampedCenterY)
            )
        }

        // 5. Instructional Label overlay dynamically following the ROI box top edge
        if (currentRoiRect != Rect.Zero) {
            val labelPaddingPx = with(density) { 36.dp.toPx() }
            val labelX = currentRoiRect.center.x
            val labelY = (currentRoiRect.top - labelPaddingPx).coerceAtLeast(with(density) { 80.dp.toPx() })

            Text(
                text = "Position Smart Film (Red Cabbage) Within ROI Box",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (labelX - with(density) { 150.dp.toPx() }).roundToInt(),
                            y = labelY.roundToInt()
                        )
                    }
                    .widthIn(max = 300.dp)
                    .padding(horizontal = 8.dp)
            )
        }
    }
}
