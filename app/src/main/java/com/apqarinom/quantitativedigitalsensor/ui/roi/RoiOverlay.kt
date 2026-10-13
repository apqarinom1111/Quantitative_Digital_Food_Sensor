package com.apqarinom.quantitativedigitalsensor.ui.roi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Composable ROI (Region of Interest) Overlay.
 *
 * POSITIONING OVER CAMERA STREAM EXPLANATION:
 * -------------------------------------------------------------------------------------------------
 * 1. Coordinates Alignment:
 *    The Jetpack Compose Canvas occupies the exact same viewport bounds (fillMaxSize) as the
 *    CameraX PreviewView underneath. Because both viewports share the exact screen aspect ratio
 *    and coordinate origin (0,0 at top-left), any coordinate (x,y) on this Canvas maps directly
 *    to the corresponding pixel location on the rendered camera stream.
 *
 * 2. Centering Logic:
 *    - canvasWidth = size.width, canvasHeight = size.height
 *    - roiWidth = size.width * widthFraction (e.g., 70% of screen width)
 *    - roiHeight = roiWidth / aspectRatio (e.g., 1.0 for square, 1.33 for rectangle)
 *    - left = (canvasWidth - roiWidth) / 2
 *    - top = (canvasHeight - roiHeight) / 2
 *
 * 3. Scrim Masking (Dimming):
 *    We construct a Path composed of two rectangles: the full screen outer bounds and the center
 *    ROI cutout rectangle. Using [PathFillType.EvenOdd], the center ROI area remains completely
 *    unshaded (clear view of the camera feed), while the surrounding screen is darkened with a
 *    semi-transparent overlay (scrim).
 *
 * 4. Alignment Guidelines & Corner Reticles:
 *    Over top of the cutout, high-contrast bounding borders, target crosshairs, and corner brackets
 *    are drawn to guide the operator when positioning the smart film pH indicator.
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
    strokeWidthDp: Dp = 3.dp
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate ROI dimensions centered on the canvas
            val roiWidth = canvasWidth * roiWidthFraction.coerceIn(0.2f, 0.95f)
            val roiHeight = roiWidth / roiAspectRatio
            val left = (canvasWidth - roiWidth) / 2f
            val top = (canvasHeight - roiHeight) / 2f
            val right = left + roiWidth
            val bottom = top + roiHeight

            val roiRect = Rect(left, top, right, bottom)

            // 1. Draw semi-transparent dark mask with hole over ROI area using EvenOdd fill type
            val maskPath = Path().apply {
                fillType = PathFillType.EvenOdd
                // Outer full canvas rectangle
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
                color = borderColor.copy(alpha = 0.8f),
                topLeft = Offset(left, top),
                size = Size(roiWidth, roiHeight),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = strokeWidthPx)
            )

            // 3. Draw Corner Reticle Brackets for precise placement
            val cornerLengthPx = cornerLengthDp.toPx()
            val bracketColor = Color.White

            // Top-Left Corner
            drawLine(bracketColor, Offset(left - 4f, top), Offset(left + cornerLengthPx, top), strokeWidthPx * 1.5f, StrokeCap.Round)
            drawLine(bracketColor, Offset(left, top - 4f), Offset(left, top + cornerLengthPx), strokeWidthPx * 1.5f, StrokeCap.Round)

            // Top-Right Corner
            drawLine(bracketColor, Offset(right + 4f, top), Offset(right - cornerLengthPx, top), strokeWidthPx * 1.5f, StrokeCap.Round)
            drawLine(bracketColor, Offset(right, top - 4f), Offset(right, top + cornerLengthPx), strokeWidthPx * 1.5f, StrokeCap.Round)

            // Bottom-Left Corner
            drawLine(bracketColor, Offset(left - 4f, bottom), Offset(left + cornerLengthPx, bottom), strokeWidthPx * 1.5f, StrokeCap.Round)
            drawLine(bracketColor, Offset(left, bottom + 4f), Offset(left, bottom + cornerLengthPx), strokeWidthPx * 1.5f, StrokeCap.Round)

            // Bottom-Right Corner
            drawLine(bracketColor, Offset(right + 4f, bottom), Offset(right - cornerLengthPx, bottom), strokeWidthPx * 1.5f, StrokeCap.Round)
            drawLine(bracketColor, Offset(right, bottom + 4f), Offset(right, bottom - cornerLengthPx), strokeWidthPx * 1.5f, StrokeCap.Round)

            // 4. Center Crosshair / Target Dot
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f
            drawCircle(
                color = borderColor,
                radius = 4f,
                center = Offset(centerX, centerY)
            )
        }

        // Instructional Label overlay above ROI box
        Text(
            text = "Position Smart Film (Red Cabbage) Within ROI Box",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 220.dp, start = 32.dp, end = 32.dp)
        )
    }
}
