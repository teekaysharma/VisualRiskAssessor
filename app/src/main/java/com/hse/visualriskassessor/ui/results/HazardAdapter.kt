package com.hse.visualriskassessor.ui.results

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hse.visualriskassessor.R
import com.hse.visualriskassessor.model.Hazard
import com.hse.visualriskassessor.model.RiskLevel

class HazardAdapter(
    private val hazards: List<Hazard>
) : RecyclerView.Adapter<HazardAdapter.HazardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HazardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hazard, parent, false)
        return HazardViewHolder(view)
    }

    override fun onBindViewHolder(holder: HazardViewHolder, position: Int) {
        holder.bind(hazards[position])
    }

    override fun getItemCount() = hazards.size

    class HazardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val hazardTypeText: TextView = itemView.findViewById(R.id.hazardTypeText)
        private val hazardDescriptionText: TextView = itemView.findViewById(R.id.hazardDescriptionText)
        private val likelihoodText: TextView = itemView.findViewById(R.id.likelihoodText)
        private val severityText: TextView = itemView.findViewById(R.id.severityText)
        private val riskBadge: TextView = itemView.findViewById(R.id.riskBadge)

        fun bind(hazard: Hazard) {
            hazardTypeText.text = hazard.type.displayName
            hazardDescriptionText.text = hazard.details ?: hazard.type.description
            likelihoodText.text = hazard.likelihood.toString()
            severityText.text = hazard.severity.toString()
            
            riskBadge.text = hazard.riskLevel.displayName
            
            val badgeColor = when (hazard.riskLevel) {
                RiskLevel.LOW -> Color.rgb(76, 175, 80)
                RiskLevel.MEDIUM -> Color.rgb(255, 193, 7)
                RiskLevel.HIGH -> Color.rgb(255, 152, 0)
                RiskLevel.VERY_HIGH -> Color.rgb(255, 87, 34)
                RiskLevel.EXTREME -> Color.rgb(211, 47, 47)
            }
            
            val background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 32f
                setColor(badgeColor)
            }
            riskBadge.background = background
            riskBadge.setTextColor(Color.WHITE)
        }
    }
}
