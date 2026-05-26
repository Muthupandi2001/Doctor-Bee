package com.example.drbee.Helper.AppTheme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ── Spring mesh node — each point in the grid ────────────────────────────────
private data class MeshPoint(
    var x: Float, var y: Float,         // current position
    val rx: Float, val ry: Float,       // rest position
    var vx: Float = 0f, var vy: Float = 0f
)

// ── One surface — a displaced patch of liquid glass ──────────────────────────
private data class LiquidWave(
    val origin: Offset,
    val anim: Animatable<Float, AnimationVector1D>,
    val colorVariant: Int
)

// ── The real liquid glass indication node ────────────────────────────────────
private class LiquidGlassNode(
    private val interactionSource: InteractionSource,
    private val bounded: Boolean
) : Modifier.Node(), DrawModifierNode {

    private val waves = mutableStateListOf<LiquidWave>()

    // spring mesh — built lazily on first draw when size is known
    private var meshCols = 0
    private var meshRows = 0
    private var mesh: Array<Array<MeshPoint>>? = null
    private var lastW = 0f
    private var lastH = 0f
    private var physicsJob: Job? = null

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Press) {
                    spawnWave(interaction.pressPosition)
                    distortMesh(interaction.pressPosition, force = 14f)
                }
            }
        }
    }

    private fun buildMesh(w: Float, h: Float) {
        if (w == lastW && h == lastH && mesh != null) return
        lastW = w; lastH = h
        meshCols = 28; meshRows = 16
        mesh = Array(meshRows + 1) { r ->
            Array(meshCols + 1) { c ->
                val rx = (c.toFloat() / meshCols) * w
                val ry = (r.toFloat() / meshRows) * h
                MeshPoint(rx, ry, rx, ry)
            }
        }
        startPhysicsLoop()
    }

    private fun startPhysicsLoop() {
        physicsJob?.cancel()
        physicsJob = coroutineScope.launch {
            while (true) {
                kotlinx.coroutines.delay(16L)
                stepMeshPhysics()
                // invalidate draw
                waves.toList() // just read to trigger recomposition
            }
        }
    }

    private fun stepMeshPhysics() {
        val m = mesh ?: return
        val springK  = 0.022f
        val dampK    = 0.875f
        val tensionK = 0.10f
        val w = lastW; val h = lastH
        val dw = w / meshCols; val dh = h / meshRows

        for (r in 0..meshRows) {
            for (c in 0..meshCols) {
                val p = m[r][c]
                // restore spring
                p.vx += (p.rx - p.x) * springK
                p.vy += (p.ry - p.y) * springK
                // horizontal tension
                if (c > 0) {
                    val n = m[r][c - 1]
                    val dx = p.x - n.x - dw
                    p.vx -= dx * tensionK * 0.5f
                    n.vx += dx * tensionK * 0.5f
                }
                // vertical tension
                if (r > 0) {
                    val n = m[r - 1][c]
                    val dy = p.y - n.y - dh
                    p.vy -= dy * tensionK * 0.5f
                    n.vy += dy * tensionK * 0.5f
                }
                p.vx *= dampK; p.vy *= dampK
                p.x += p.vx;  p.y += p.vy
            }
        }
    }

    private fun distortMesh(touch: Offset, force: Float) {
        val m = mesh ?: return
        val radius = 140f
        for (r in 0..meshRows) {
            for (c in 0..meshCols) {
                val p = m[r][c]
                val dx = p.x - touch.x
                val dy = p.y - touch.y
                val d  = sqrt(dx * dx + dy * dy)
                if (d < radius) {
                    val f = force * (1f - d / radius)
                    val angle = kotlin.math.atan2(dy, dx)
                    p.vx += cos(angle) * f
                    p.vy += sin(angle) * f
                }
            }
        }
    }

    private fun spawnWave(offset: Offset) {
        val wave = LiquidWave(
            origin       = offset,
            anim         = Animatable(0f),
            colorVariant = Random.nextInt(3)
        )
        waves.add(wave)
        coroutineScope.launch {
            wave.anim.animateTo(
                targetValue   = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessLow
                )
            )
            waves.remove(wave)
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        buildMesh(size.width, size.height)
        drawLiquidMesh()
        drawWaves()
    }

    private fun ContentDrawScope.drawLiquidMesh() {
        val m = mesh ?: return
        for (r in 0 until meshRows) {
            for (c in 0 until meshCols) {
                val p00 = m[r][c]
                val p10 = m[r][c + 1]
                val p01 = m[r + 1][c]
                val p11 = m[r + 1][c + 1]

                val mx = (p00.x + p10.x + p01.x + p11.x) / 4f
                val my = (p00.y + p10.y + p01.y + p11.y) / 4f
                val rxc = (p00.rx + p10.rx + p01.rx + p11.rx) / 4f
                val ryc = (p00.ry + p10.ry + p01.ry + p11.ry) / 4f
                val distortion = sqrt((mx - rxc) * (mx - rxc) + (my - ryc) * (my - ryc))

                if (distortion < 0.6f) continue

                val alpha = (distortion / 20f).coerceIn(0f, 1f)

                // build quad path
                val path = Path().apply {
                    moveTo(p00.x, p00.y)
                    lineTo(p10.x, p10.y)
                    lineTo(p11.x, p11.y)
                    lineTo(p01.x, p01.y)
                    close()
                }

                // refraction fill — chromatic iridescence like real glass
                val angle = kotlin.math.atan2(my - ryc, mx - rxc).toFloat()
                val r1 = (200 + 55 * cos(angle)).toInt().coerceIn(0, 255)
                val g1 = (220 + 35 * sin(angle)).toInt().coerceIn(0, 255)

                drawPath(
                    path  = path,
                    brush = Brush.linearGradient(
                        colors     = listOf(
                            Color(r1, g1, 255, (alpha * 0.18f * 255).toInt()),
                            Color(255, 255, 255, (alpha * 0.07f * 255).toInt()),
                            Color(r1, g1, 255, (alpha * 0.04f * 255).toInt())
                        ),
                        start = Offset(p00.x, p00.y),
                        end   = Offset(p11.x, p11.y)
                    )
                )

                // specular edge lines
                drawPath(
                    path  = path,
                    color = Color.White.copy(alpha = alpha * 0.20f),
                    style = Stroke(width = 0.4.dp.toPx())
                )

                // caustic sparkle at high-distortion points
                if (distortion > 5f) {
                    val causticAlpha = ((distortion - 5f) / 14f).coerceIn(0f, 0.65f)
                    val hue = (180f + distortion * 7f) % 360f
                    drawCircle(
                        color  = hslToColor(hue, 1f, 0.9f, causticAlpha * 0.30f),
                        radius = (distortion * 0.5f).coerceAtMost(5.dp.toPx()),
                        center = Offset(mx, my)
                    )
                }
            }
        }

        // top-edge specular shimmer — the hallmark iOS glass highlight
        val topPath = Path().apply {
            val row0 = m[0]; val row1 = m[1]
            moveTo(row0[0].x, row0[0].y)
            for (c in 1..meshCols) lineTo(row0[c].x, row0[c].y)
            for (c in meshCols downTo 0) lineTo(row1[c].x, row1[c].y)
            close()
        }
        drawPath(
            path  = topPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.05f)
                ),
                start = Offset(0f, 0f),
                end   = Offset(size.width, 0f)
            )
        )
    }

    private fun ContentDrawScope.drawWaves() {
        val snapshot = waves.toList()
        for (wave in snapshot) {
            val t      = wave.anim.value
            val eased  = 1f - (1f - t) * (1f - t)
            val maxR   = if (bounded)
                (size.minDimension * 0.68f).coerceAtLeast(70.dp.toPx())
            else
                (size.maxDimension).coerceAtLeast(220.dp.toPx())
            val r     = eased * maxR
            val alpha = (1f - t) * 0.55f
            val o     = wave.origin

            val (primary, secondary) = when (wave.colorVariant) {
                0    -> Color(0xFFFFB428) to Color(0xFFFF7A00)
                1    -> Color(0xFF00E5CC) to Color(0xFF00AAFF)
                else -> Color(0xFFCC88FF) to Color(0xFF7744EE)
            }

            // ── thin expanding ring — like water surface wave ─────────────
            if (r > 1f) {
                drawCircle(
                    color  = primary.copy(alpha = alpha * 0.9f),
                    radius = r,
                    center = o,
                    style  = Stroke(width = 1.dp.toPx())
                )
            }
            // ── 2nd ring — lags behind = wave train
            val r2 = (r - 24.dp.toPx()).coerceAtLeast(0f)
            if (r2 > 1f) {
                drawCircle(
                    color  = secondary.copy(alpha = alpha * 0.55f),
                    radius = r2,
                    center = o,
                    style  = Stroke(width = 0.7.dp.toPx())
                )
            }
            // ── 3rd micro ring
            val r3 = (r - 44.dp.toPx()).coerceAtLeast(0f)
            if (r3 > 1f) {
                drawCircle(
                    color  = Color.White.copy(alpha = alpha * 0.28f),
                    radius = r3,
                    center = o,
                    style  = Stroke(width = 0.5.dp.toPx())
                )
            }
            // ── refraction lens fill (glow inside the wave front)
            if (r > 1f) {
                drawCircle(
                    brush  = Brush.radialGradient(
                        colors  = listOf(
                            primary.copy(alpha = alpha * 0.12f),
                            secondary.copy(alpha = alpha * 0.05f),
                            Color.Transparent
                        ),
                        center = o,
                        radius = r
                    ),
                    radius = r,
                    center = o
                )
            }
            // ── specular arc — top-right — iOS caustic glint
            val arcR = r * 0.82f
            if (arcR > 4.dp.toPx()) {
                drawArc(
                    color      = Color.White.copy(alpha = alpha * 0.45f),
                    startAngle = -68f,
                    sweepAngle = 52f,
                    useCenter  = false,
                    topLeft    = Offset(o.x - arcR, o.y - arcR),
                    size       = Size(arcR * 2f, arcR * 2f),
                    style      = Stroke(width = 1.dp.toPx())
                )
            }
            // ── impact sparkle at origin
            val sparkR = 3.dp.toPx() * (1f - t * 0.8f)
            if (sparkR > 0.5f) {
                drawCircle(
                    color  = Color.White.copy(alpha = alpha * 0.9f),
                    radius = sparkR,
                    center = o
                )
            }
        }
    }
}

// ── HSL helper — no java.awt needed ──────────────────────────────────────────
private fun hslToColor(h: Float, s: Float, l: Float, a: Float): Color {
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60  -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else    -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m, a)
}

// ── Factories ─────────────────────────────────────────────────────────────────
object LiquidGlassIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        LiquidGlassNode(interactionSource, bounded = true)
    override fun equals(other: Any?) = other === this
    override fun hashCode()          = 2001
}

object LiquidGlassIndicationUnbounded : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        LiquidGlassNode(interactionSource, bounded = false)
    override fun equals(other: Any?) = other === this
    override fun hashCode()          = 2002
}

// ── Modifiers ─────────────────────────────────────────────────────────────────
fun Modifier.liquidClickable(
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
    onClick: () -> Unit
): Modifier = this
    .indication(interactionSource, LiquidGlassIndication)
    .clickable(
        interactionSource = interactionSource,
        indication        = null,
        onClick           = onClick
    )

fun Modifier.liquidBackground(
    interactionSource: MutableInteractionSource
): Modifier = this
    .indication(interactionSource, LiquidGlassIndicationUnbounded)
    .pointerInput(interactionSource) {
        detectTapGestures { offset ->
            interactionSource.tryEmit(PressInteraction.Press(offset))
        }
    }

// Drag variant — for scroll surfaces that need continuous distortion
fun Modifier.liquidDraggable(
    interactionSource: MutableInteractionSource
): Modifier = this
    .indication(interactionSource, LiquidGlassIndicationUnbounded)
    .pointerInput(interactionSource) {
        detectDragGestures(
            onDragStart = { offset ->
                interactionSource.tryEmit(PressInteraction.Press(offset))
            },
            onDrag = { change, _ ->
                interactionSource.tryEmit(PressInteraction.Press(change.position))
            }
        )
    }