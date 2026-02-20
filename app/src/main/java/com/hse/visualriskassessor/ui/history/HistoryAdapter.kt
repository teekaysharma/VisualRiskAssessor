package com.hse.visualriskassessor.ui.history

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.hse.visualriskassessor.R
import com.hse.visualriskassessor.model.AssessmentResult
import java.io.File
import java.text.DateFormat

class HistoryAdapter(
    private var assessments: List<AssessmentResult>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val dateFormatter = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view, dateFormatter)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(assessments[position])
    }

    override fun getItemCount() = assessments.size

    fun submitList(newAssessments: List<AssessmentResult>) {
        assessments = newAssessments
        notifyDataSetChanged()
    }

    class HistoryViewHolder(
        itemView: View,
        private val dateFormatter: DateFormat
    ) : RecyclerView.ViewHolder(itemView) {
        private val historyImage: ImageView = itemView.findViewById(R.id.historyImage)
        private val historyDate: TextView = itemView.findViewById(R.id.historyDate)
        private val historyRiskLevel: TextView = itemView.findViewById(R.id.historyRiskLevel)
        private val historyHazardCount: TextView = itemView.findViewById(R.id.historyHazardCount)

        fun bind(assessment: AssessmentResult) {
            historyImage.load(File(assessment.imagePath)) {
                crossfade(true)
            }

            historyDate.text = dateFormatter.format(assessment.timestamp)
            historyRiskLevel.text = assessment.overallRiskLevel.displayName

            val riskColor = ContextCompat.getColor(itemView.context, assessment.overallRiskLevel.colorRes)
            historyRiskLevel.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(riskColor)
            }
            historyRiskLevel.setTextColor(Color.WHITE)

            val hazardLabel = itemView.context.getString(R.string.hazards_detected)
            historyHazardCount.text = "$hazardLabel: ${assessment.hazards.size}"
        }
    }
}
