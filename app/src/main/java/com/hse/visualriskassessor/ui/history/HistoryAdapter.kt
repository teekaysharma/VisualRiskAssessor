package com.hse.visualriskassessor.ui.history

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.hse.visualriskassessor.R
import com.hse.visualriskassessor.model.AssessmentResult
import com.hse.visualriskassessor.model.RiskLevel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (AssessmentResult) -> Unit,
    private val onItemLongClick: (AssessmentResult) -> Boolean
) : ListAdapter<AssessmentResult, HistoryAdapter.HistoryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick, onItemLongClick)
    }

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImage: ImageView = itemView.findViewById(R.id.thumbnailImage)
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        private val hazardCountText: TextView = itemView.findViewById(R.id.hazardCountText)
        private val riskLevelBadge: TextView = itemView.findViewById(R.id.riskLevelBadge)

        private val dateFormat = SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault())

        fun bind(
            result: AssessmentResult,
            onItemClick: (AssessmentResult) -> Unit,
            onItemLongClick: (AssessmentResult) -> Boolean
        ) {
            dateText.text = dateFormat.format(result.timestamp)
            hazardCountText.text = itemView.context.resources.getQuantityString(
                R.plurals.hazards_count,
                result.hazards.size,
                result.hazards.size
            )

            riskLevelBadge.text = result.overallRiskLevel.displayName

            val badgeColor = when (result.overallRiskLevel) {
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
            riskLevelBadge.background = background
            riskLevelBadge.setTextColor(Color.WHITE)

            val imageFile = File(result.imagePath)
            if (imageFile.exists()) {
                thumbnailImage.load(imageFile) {
                    crossfade(true)
                }
            } else {
                thumbnailImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            itemView.setOnClickListener { onItemClick(result) }
            itemView.setOnLongClickListener { onItemLongClick(result) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AssessmentResult>() {
            override fun areItemsTheSame(oldItem: AssessmentResult, newItem: AssessmentResult): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: AssessmentResult, newItem: AssessmentResult): Boolean {
                return oldItem == newItem
            }
        }
    }
}
