package br.com.clockscreen

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import java.util.Calendar
import kotlin.math.abs
import kotlin.random.Random

class SevenSegmentClock @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val segmentMap = mapOf(
        '0' to listOf('a', 'b', 'c', 'd', 'e', 'f'),
        '1' to listOf('b', 'c'),
        '2' to listOf('a', 'b', 'd', 'e', 'g'),
        '3' to listOf('a', 'b', 'c', 'd', 'g'),
        '4' to listOf('b', 'c', 'f', 'g'),
        '5' to listOf('a', 'c', 'd', 'f', 'g'),
        '6' to listOf('a', 'c', 'd', 'e', 'f', 'g'),
        '7' to listOf('a', 'b', 'c'),
        '8' to listOf('a', 'b', 'c', 'd', 'e', 'f', 'g'),
        '9' to listOf('a', 'b', 'c', 'd', 'f', 'g'),
    )

    private val allSegments = listOf('a', 'b', 'c', 'd', 'e', 'f', 'g')

    private val paintLit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF0A00")
        style = Paint.Style.FILL
    }

    // Glow mais suave
    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FF0A00")
        style = Paint.Style.FILL
    }

    private val paintDim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D0303")
        style = Paint.Style.FILL
    }

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val BURN_IN_INTERVAL_MS = 30_000L // a cada 30s (mais espaçado)
        private const val MAX_OFFSET = 4f // menor deslocamento máximo
        private const val ANIMATION_DURATION_MS = 800L // duração da animação suave
    }

    // Target offsets (destino final da animação)
    private var targetOffsets = mutableListOf<Pair<Float, Float>>()

    // Current animated offsets (valores atuais sendo interpolados)
    private var currentOffsets = mutableListOf<Pair<Float, Float>>()

    // Animators para cada dígito
    private val animators = mutableListOf<ValueAnimator>()

    init {
        // Inicializa com zero offset
        val totalDigits = 4 // HH:MM
        targetOffsets = MutableList(totalDigits) { 0f to 0f }
        currentOffsets = MutableList(totalDigits) { 0f to 0f }

        handler.post(object : Runnable {
            override fun run() {
                invalidate()
                handler.postDelayed(this, 1000)
            }
        })

        // Atualiza offset anti burn-in com animação suave
        handler.postDelayed(object : Runnable {
            override fun run() {
                applyAntiBurnInShift()
                handler.postDelayed(this, BURN_IN_INTERVAL_MS)
            }
        }, BURN_IN_INTERVAL_MS)
    }

    /**
     * Aplica deslocamento anti-burn-in com animação suave (interpolação linear).
     * Em vez de pular instantaneamente para a nova posição, os dígitos
     * deslizam suavemente durante ANIMATION_DURATION_MS.
     */
    private fun applyAntiBurnInShift() {
        // Para animadores anteriores
        animators.forEach { it.cancel() }
        animators.clear()

        val currentTargets = targetOffsets.toMutableList()
        val startOffsets = currentOffsets.toMutableList()

        // Gera novos offsets alvo
        val newTargets = MutableList(targetOffsets.size) { index ->
            val randomDx = Random.nextFloat() * MAX_OFFSET * 2 - MAX_OFFSET
            val randomDy = Random.nextFloat() * MAX_OFFSET * 2 - MAX_OFFSET
            randomDx to randomDy
        }
        targetOffsets = newTargets

        // Cria um ValueAnimator que interpola de 0f a 1f
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION_MS
            interpolator = DecelerateInterpolator()

            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                // Interpola cada dígito do offset inicial para o target
                for (i in currentTargets.indices) {
                    val startX = startOffsets[i].first
                    val startY = startOffsets[i].second
                    val targetX = newTargets[i].first
                    val targetY = newTargets[i].second

                    currentOffsets[i] = lerp(startX, targetX, fraction) to lerp(startY, targetY, fraction)
                }
                invalidate()
            }

            addOnCancelListener {
                // Garante que ao cancelar os dígitos vão para o target final
                for (i in currentTargets.indices) {
                    currentOffsets[i] = newTargets[i]
                }
                invalidate()
            }

            start()
        }

        animators.add(animator)
    }

    /**
     * Interpolação linear entre start e end com fator fraction (0..1)
     */
    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        val timeStr = String.format("%02d:%02d", hour, minute)

        val digitWidth = width / 7f
        val digitHeight = height * 0.85f
        val digitSize = minOf(digitWidth, digitHeight)

        val totalWidth = digitSize * 4 + digitSize * 0.3f * 3 + digitSize * 0.4f
        val startX = (width - totalWidth) / 2f + digitSize / 2f
        val startY = (height - digitSize) / 2f

        var xPos = startX
        var offsetIndex = 0
        for (i in timeStr.indices) {
            val ch = timeStr[i]
            if (ch == ':') {
                drawColon(canvas, xPos, startY, digitSize, second % 2 == 0)
                xPos += digitSize * 0.4f
            } else {
                val (dx, dy) = currentOffsets.getOrNull(offsetIndex) ?: 0f to 0f
                canvas.save()
                canvas.translate(dx, dy)
                drawDigit(canvas, ch, xPos, startY, digitSize)
                canvas.restore()
                xPos += digitSize + digitSize * 0.3f
                offsetIndex++
            }
        }
    }

    private fun drawDigit(canvas: Canvas, digit: Char, x: Float, y: Float, size: Float) {
        val activeSegments = segmentMap[digit] ?: return
        val segmentThickness = size * 0.12f
        val gap = segmentThickness * 0.5f

        val segmentRects = mapOf(
            'a' to arrayOf(x + gap, y, x + size - gap, y + segmentThickness),
            'b' to arrayOf(x + size - segmentThickness, y + gap, x + size, y + size / 2 - gap),
            'c' to arrayOf(x + size - segmentThickness, y + size / 2 + gap, x + size, y + size - gap),
            'd' to arrayOf(x + gap, y + size - segmentThickness, x + size - gap, y + size),
            'e' to arrayOf(x, y + size / 2 + gap, x + segmentThickness, y + size - gap),
            'f' to arrayOf(x, y + gap, x + segmentThickness, y + size / 2 - gap),
            'g' to arrayOf(x + gap, y + size / 2 - segmentThickness / 2, x + size - gap, y + size / 2 + segmentThickness / 2),
        )

        for (seg in allSegments) {
            val (left, top, right, bottom) = segmentRects[seg]!!
            if (seg in activeSegments) {
                canvas.drawRect(left - gap, top - gap, right + gap, bottom + gap, paintGlow)
                canvas.drawRect(left, top, right, bottom, paintLit)
            } else {
                canvas.drawRect(left, top, right, bottom, paintDim)
            }
        }
    }

    private fun drawColon(canvas: Canvas, x: Float, y: Float, size: Float, visible: Boolean) {
        val dotSize = size * 0.12f
        val spacing = size * 0.25f
        val colonX = x + size * 0.1f
        val centerY = y + size / 2f

        val paint = if (visible) paintLit else paintDim

        canvas.drawRect(colonX, centerY - spacing - dotSize / 2, colonX + dotSize, centerY - spacing + dotSize / 2, paint)
        canvas.drawRect(colonX, centerY + spacing - dotSize / 2, colonX + dotSize, centerY + spacing + dotSize / 2, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
        animators.forEach { it.cancel() }
        animators.clear()
    }
}