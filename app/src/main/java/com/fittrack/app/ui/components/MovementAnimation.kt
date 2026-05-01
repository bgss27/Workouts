package com.fittrack.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fittrack.app.domain.model.MovementPattern

@Composable
fun MovementAnimation(
    pattern: MovementPattern,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "movement")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    val accentColor = MaterialTheme.colorScheme.primary
    val bodyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val weightColor = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            when (pattern) {
                MovementPattern.HORIZONTAL_PUSH -> drawHorizontalPush(phase, bodyColor, weightColor, accentColor)
                MovementPattern.HORIZONTAL_PULL -> drawHorizontalPull(phase, bodyColor, weightColor, accentColor)
                MovementPattern.VERTICAL_PUSH -> drawVerticalPush(phase, bodyColor, weightColor, accentColor)
                MovementPattern.VERTICAL_PULL -> drawVerticalPull(phase, bodyColor, weightColor, accentColor)
                MovementPattern.SQUAT -> drawSquat(phase, bodyColor, weightColor, accentColor)
                MovementPattern.HIP_HINGE -> drawHipHinge(phase, bodyColor, weightColor, accentColor)
                MovementPattern.CURL -> drawCurl(phase, bodyColor, weightColor, accentColor)
                MovementPattern.EXTENSION -> drawExtension(phase, bodyColor, weightColor, accentColor)
                MovementPattern.LATERAL_RAISE -> drawLateralRaise(phase, bodyColor, weightColor, accentColor)
                MovementPattern.ISOLATION -> drawIsolation(phase, bodyColor, weightColor, accentColor)
                MovementPattern.STATIC_HOLD -> drawStaticHold(phase, bodyColor, weightColor, accentColor)
                MovementPattern.CARRY -> drawCarry(phase, bodyColor, weightColor, accentColor)
            }
        }
        Text(
            text = pattern.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

private fun DrawScope.drawStickPerson(
    cx: Float, baseY: Float, bodyColor: Color, strokeWidth: Float,
    torsoAngle: Float = 0f, armAngle: Float = 0f, legBend: Float = 0f
) {
    val headR = size.width * 0.04f
    val torsoLen = size.height * 0.3f
    val limbLen = size.height * 0.25f

    // Head
    val headY = baseY - torsoLen - headR * 2
    drawCircle(bodyColor, headR, Offset(cx, headY), style = Stroke(strokeWidth))

    // Torso
    val torsoTopY = headY + headR
    val torsoBottomY = torsoTopY + torsoLen
    val torsoOffsetX = torsoAngle * size.width * 0.1f
    drawLine(bodyColor, Offset(cx, torsoTopY), Offset(cx + torsoOffsetX, torsoBottomY), strokeWidth, StrokeCap.Round)

    // Arms
    val shoulderY = torsoTopY + torsoLen * 0.15f
    val armEndY = shoulderY + limbLen * (1 - armAngle * 0.7f)
    val armEndX1 = cx - limbLen * 0.6f * (1 - armAngle * 0.5f)
    val armEndX2 = cx + limbLen * 0.6f * (1 - armAngle * 0.5f)
    drawLine(bodyColor, Offset(cx, shoulderY), Offset(armEndX1, armEndY), strokeWidth, StrokeCap.Round)
    drawLine(bodyColor, Offset(cx, shoulderY), Offset(armEndX2, armEndY), strokeWidth, StrokeCap.Round)

    // Legs
    val hipY = torsoBottomY
    val kneeSpread = limbLen * 0.3f
    val kneeBendY = hipY + limbLen * (1 - legBend * 0.4f)
    drawLine(bodyColor, Offset(cx + torsoOffsetX, hipY), Offset(cx - kneeSpread + torsoOffsetX, kneeBendY), strokeWidth, StrokeCap.Round)
    drawLine(bodyColor, Offset(cx + torsoOffsetX, hipY), Offset(cx + kneeSpread + torsoOffsetX, kneeBendY), strokeWidth, StrokeCap.Round)
}

private fun DrawScope.drawWeight(x: Float, y: Float, width: Float, color: Color, strokeWidth: Float) {
    drawLine(color, Offset(x - width / 2, y), Offset(x + width / 2, y), strokeWidth * 2, StrokeCap.Round)
}

private fun DrawScope.drawHorizontalPush(phase: Float, body: Color, weight: Color, accent: Color) {
    val cx = size.width * 0.4f
    val baseY = size.height * 0.85f
    val sw = 4f
    drawStickPerson(cx, baseY, body, sw, torsoAngle = 0f, armAngle = phase, legBend = 0f)
    val weightY = size.height * 0.25f + (1 - phase) * size.height * 0.15f
    drawWeight(cx, weightY, size.width * 0.3f, weight, sw)
    // Arrow
    val arrowY = size.height * 0.15f
    drawLine(accent, Offset(cx, arrowY + 10), Offset(cx, arrowY - 10), sw, StrokeCap.Round)
    drawLine(accent, Offset(cx - 8, arrowY - 2), Offset(cx, arrowY - 10), sw, StrokeCap.Round)
    drawLine(accent, Offset(cx + 8, arrowY - 2), Offset(cx, arrowY - 10), sw, StrokeCap.Round)
}

private fun DrawScope.drawHorizontalPull(phase: Float, body: Color, weight: Color, accent: Color) {
    val cx = size.width * 0.5f
    val baseY = size.height * 0.85f
    val sw = 4f
    drawStickPerson(cx, baseY, body, sw, torsoAngle = 0.2f, armAngle = phase, legBend = 0.1f)
    val weightY = size.height * 0.7f - phase * size.height * 0.15f
    drawWeight(cx, weightY, size.width * 0.3f, weight, sw)
    drawLine(accent, Offset(cx, weightY + 15), Offset(cx, weightY - 5), sw, StrokeCap.Round)
}

private fun DrawScope.drawVerticalPush(phase: Float, body: Color, weight: Color, accent: Color) {
    val cx = size.width * 0.5f
    val baseY = size.height * 0.9f
    val sw = 4f
    drawStickPerson(cx, baseY, body, sw, armAngle = phase, legBend = 0f)
    val weightY = size.height * 0.35f - phase * size.height * 0.2f
    drawWeight(cx, weightY, size.width * 0.35f, weight, sw)
    drawLine(accent, Offset(cx, weightY + 10), Offset(cx, weightY - 10), sw, StrokeCap.Round)
}

private fun DrawScope.drawVerticalPull(phase: Float, body: Color, weight: Color, accent: Color) {
    val cx = size.width * 0.5f
    val sw = 4f
    val bodyY = size.height * 0.9f - phase * size.height * 0.2f
    drawStickPerson(cx, bodyY, body, sw, armAngle = phase)
    drawWeight(cx, size.height * 0.05f, size.width * 0.5f, weight, sw * 1.5f)
    drawLine(accent, Offset(cx, bodyY - size.height * 0.4f), Offset(cx, size.height * 0.05f), sw * 0.5f, StrokeCap.Round)
}

private fun DrawScope.drawSquat(phase: Float, body: Color, weight: Color, accent: Color) {
    val cx = size.width * 0.5f
    val baseY = size.height * 0.9f + phase * size.height * 0.05f
    val sw = 4f
    drawStickPerson(cx, baseY, body, sw, legBend = phase)
    val weightY = baseY - size.height * 0.55f + phase * size.height * 0.15f
    drawWeight(cx, weightY, size.width * 0.35f, weight, sw)
}

private fun DrawScope.drawHipHinge(phase: Float, body: Color, weight: Color, accent: Color) {
    val cx = size.width * 0.5f
    val baseY = size.height * 0.9f
    val sw = 4f
    drawStickPerson(cx, baseY, body, sw, torsoAngle = phase * 0.5f, legBend = phase * 0.1f)
    val weightY = size.height * 0.5f + phase * size.height * 0.2f
    drawWeight(cx + phase * size.width * 0.05f, weightY, size.width * 0.3f, weight, sw)
}

private fun DrawScope.drawCurl(phase: Float, body: Color, weight: Color, accent: Color) {
    val cx = size.width * 0.5f
    val baseY = size.height * 0.9f
    val sw = 4f
    drawStickPerson(cx, baseY, body, sw, armAngle = phase)
    val weightX = cx + size.width * 0.15f
    val weightY = size.height * 0.65f - phase * size.height * 0.25f
    drawCircle(weight, 8f, Offset(weightX, weightY))
    drawCircle(weight, 8f, Offset(cx - size.width * 0.15f, weightY))
}

private fun DrawScope.drawExtension(phase: Float, body: Color, weight: Color, accent: Color) {
    val cx = size.width * 0.5f
    val baseY = size.height * 0.9f
    val sw = 4f
    drawStickPerson(cx, baseY, body, sw, armAngle = 1f - phase)
    val weightY = size.height * 0.25f + phase * size.height * 0.2f
    drawWeight(cx, weightY, size.width * 0.15f, weight, sw)
}

private fun DrawScope.drawLateralRaise(phase: Float, body: Color, weight: Color, accent: Color) {
    val cx = size.width * 0.5f
    val baseY = size.height * 0.9f
    val sw = 4f
    drawStickPerson(cx, baseY, body, sw)
    val armSpread = phase * size.width * 0.3f
    val armY = size.height * 0.4f - phase * size.height * 0.1f
    drawCircle(weight, 6f, Offset(cx - armSpread - size.width * 0.1f, armY))
    drawCircle(weight, 6f, Offset(cx + armSpread + size.width * 0.1f, armY))
    drawLine(body, Offset(cx, size.height * 0.35f), Offset(cx - armSpread - size.width * 0.1f, armY), sw, StrokeCap.Round)
    drawLine(body, Offset(cx, size.height * 0.35f), Offset(cx + armSpread + size.width * 0.1f, armY), sw, StrokeCap.Round)
}

private fun DrawScope.drawIsolation(phase: Float, body: Color, weight: Color, accent: Color) {
    drawCurl(phase, body, weight, accent)
}

private fun DrawScope.drawStaticHold(phase: Float, body: Color, weight: Color, accent: Color) {
    val cx = size.width * 0.5f
    val sw = 4f
    val pulseAlpha = 0.4f + phase * 0.3f
    // Plank position
    val y = size.height * 0.6f
    drawLine(body, Offset(cx - size.width * 0.25f, y), Offset(cx + size.width * 0.25f, y), sw * 1.5f, StrokeCap.Round)
    drawCircle(body, size.width * 0.03f, Offset(cx - size.width * 0.25f, y - size.width * 0.04f), style = Stroke(sw))
    // Pulse ring
    drawCircle(accent.copy(alpha = pulseAlpha), size.width * 0.08f + phase * size.width * 0.03f, Offset(cx, y), style = Stroke(sw * 0.5f))
}

private fun DrawScope.drawCarry(phase: Float, body: Color, weight: Color, accent: Color) {
    val walkOffset = phase * size.width * 0.1f
    val cx = size.width * 0.4f + walkOffset
    val baseY = size.height * 0.9f
    val sw = 4f
    drawStickPerson(cx, baseY, body, sw, legBend = phase * 0.15f)
    drawCircle(weight, 10f, Offset(cx - size.width * 0.15f, size.height * 0.65f))
    drawCircle(weight, 10f, Offset(cx + size.width * 0.15f, size.height * 0.65f))
}
