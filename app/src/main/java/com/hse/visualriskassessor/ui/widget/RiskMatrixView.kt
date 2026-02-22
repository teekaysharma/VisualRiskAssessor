package com.hse.visualriskassessor.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.hse.visualriskassessor.model.Hazard
import com.hse.visualriskassessor.model.RiskLevel

class RiskMatrixView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.GRAY
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 28f
        color = Color.BLACK
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        color = Color.DKGRAY
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private val matrix = Array(5) { IntArray(5) }
    private var hazards: List<Hazard> = emptyList()
    
    private val labels = arrayOf("1", "2", "3", "4", "5")
    private val likelihoodLabels = arrayOf("Rare", "Unlikely", "Possible", "Likely", "Certain")
    private val severityLabels = arrayOf("Negligible", "Minor", "Moderate", "Major", "Catastrophic")

    init {
        initializeMatrix()
    }

    private fun initializeMatrix() {
        for (likelihood in 0..4) {
            for (severity in 0..4) {
                val score = (likelihood + 1) * (severity + 1)
                matrix[likelihood][severity] = score
            }
        }
    }

    fun setHazards(hazards: List<Hazard>) {
        this.hazards = hazards
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = 80f
        val availableWidth = width - padding * 2
        val availableHeight = height - padding * 2
        val cellSize = minOf(availableWidth / 5, availableHeight / 5)

        val startX = padding + (availableWidth - cellSize * 5) / 2
        val startY = padding + (availableHeight - cellSize * 5) / 2

        for (row in 0..4) {
            for (col in 0..4) {
                val left = startX + col * cellSize
                val top = startY + (4 - row) * cellSize
                val rect = RectF(left, top, left + cellSize, top + cellSize)

                val score = matrix[row][col]
                val riskLevel = RiskLevel.fromScore(score)
                cellPaint.color = getRiskColor(riskLevel)
                
                canvas.drawRect(rect, cellPaint)
                canvas.drawRect(rect, borderPaint)

                textPaint.color = if (riskLevel == RiskLevel.LOW) Color.BLACK else Color.WHITE
                canvas.drawText(
                    score.toString(),
                    rect.centerX(),
                    rect.centerY() + textPaint.textSize / 3,
                    textPaint
                )
            }
        }

        for (hazard in hazards) {
            val col = hazard.severity - 1
            val row = hazard.likelihood - 1
            
            if (row in 0..4 && col in 0..4) {
                val centerX = startX + col * cellSize + cellSize / 2
                val centerY = startY + (4 - row) * cellSize + cellSize / 2
                
                canvas.drawCircle(centerX, centerY, 12f, markerPaint.apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                })
                canvas.drawCircle(centerX, centerY, 8f, markerPaint.apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                })
            }
        }

        labelPaint.textAlign = Paint.Align.CENTER
        val severityLabelY = startY + 5 * cellSize + labelPaint.textSize
        for (col in 0..4) {
            val labelX = startX + col * cellSize + cellSize / 2
            canvas.drawText(
                severityLabels[col],
                labelX,
                severityLabelY,
                labelPaint
            )
        }

        canvas.drawText(
            "Severity →",
            width / 2f,
            height - 10f,
            labelPaint
        )

        canvas.save()
        canvas.rotate(-90f, 20f, height / 2f)
        canvas.drawText(
            "Likelihood →",
            20f,
            height / 2f,
            labelPaint
        )
        canvas.restore()
    }

    private fun getRiskColor(riskLevel: RiskLevel): Int {
        return when (riskLevel) {
            RiskLevel.LOW -> Color.rgb(76, 175, 80)
            RiskLevel.MEDIUM -> Color.rgb(255, 235, 59)
            RiskLevel.HIGH -> Color.rgb(255, 152, 0)
            RiskLevel.VERY_HIGH -> Color.rgb(255, 87, 34)
            RiskLevel.EXTREME -> Color.rgb(211, 47, 47)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = 600
        
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredSize, widthSize)
            else -> desiredSize
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredSize, heightSize)
            else -> desiredSize
        }

        setMeasuredDimension(width, height)
    }
}
