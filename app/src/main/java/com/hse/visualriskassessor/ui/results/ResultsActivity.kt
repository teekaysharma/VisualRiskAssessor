package com.hse.visualriskassessor.ui.results

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.hse.visualriskassessor.R
import com.hse.visualriskassessor.HSEApplication
import com.hse.visualriskassessor.analysis.RiskAssessmentEngine
import com.hse.visualriskassessor.model.AssessmentResult
import com.hse.visualriskassessor.model.AnalysisMode
import com.hse.visualriskassessor.model.OperationResult
import com.hse.visualriskassessor.model.RiskLevel
import com.hse.visualriskassessor.ui.MainActivity
import com.hse.visualriskassessor.ui.widget.RiskMatrixView
import com.hse.visualriskassessor.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultsActivity : AppCompatActivity() {

    private lateinit var assessmentEngine: RiskAssessmentEngine
    private var assessmentResult: AssessmentResult? = null

    private val assessmentRepository by lazy {
        (application as HSEApplication).assessmentRepository
    }

    private lateinit var analyzedImage: ImageView
    private lateinit var riskLevelText: TextView
    private lateinit var riskDescriptionText: TextView
    private lateinit var fallbackWarningText: TextView
    private lateinit var hazardsRecyclerView: RecyclerView
    private lateinit var riskMatrixView: RiskMatrixView
    private lateinit var recommendationsText: TextView
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var loadingText: TextView
    private lateinit var analysisWarningBanner: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        assessmentEngine = RiskAssessmentEngine(this)

        setupViews()
        processImage()
    }

    private fun setupViews() {
        findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setSupportActionBar(this)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setNavigationOnClickListener { finish() }
        }

        analyzedImage = findViewById(R.id.analyzedImage)
        riskLevelText = findViewById(R.id.riskLevelText)
        riskDescriptionText = findViewById(R.id.riskDescriptionText)
        fallbackWarningText = findViewById(R.id.fallbackWarningText)
        hazardsRecyclerView = findViewById(R.id.hazardsRecyclerView)
        riskMatrixView = findViewById(R.id.riskMatrixView)
        recommendationsText = findViewById(R.id.recommendationsText)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingText = findViewById(R.id.loadingText)
        analysisWarningBanner = findViewById(R.id.analysisWarningBanner)

        hazardsRecyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<MaterialButton>(R.id.btnSaveReport).setOnClickListener {
            saveReport()
        }

        findViewById<MaterialButton>(R.id.btnShareReport).setOnClickListener {
            shareReport()
        }

        findViewById<MaterialButton>(R.id.btnNewAssessment).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun processImage() {
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI) ?: run {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imageUri = Uri.parse(imageUriString)
        
        analyzedImage.load(imageUri)

        showLoading(true, getString(R.string.analyzing_image))

        lifecycleScope.launch {
            try {
                updateLoadingMessage(getString(R.string.detecting_hazards))
                
                val result = withContext(Dispatchers.Default) {
                    val bitmap = ImageUtils.loadBitmapFromUri(this@ResultsActivity, imageUri)
                    if (bitmap != null) {
                        assessmentEngine.assessImage(bitmap)
                    } else {
                        assessmentEngine.assessImage(imageUri)
                    }
                }

                updateLoadingMessage(getString(R.string.calculating_risk))
                
                assessmentResult = result
                displayResults(result)
                
                showLoading(false)
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    this@ResultsActivity,
                    getString(R.string.analysis_error),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun displayResults(result: AssessmentResult) {
        riskLevelText.text = result.overallRiskLevel.displayName
        riskLevelText.setTextColor(getRiskColor(result.overallRiskLevel))

        riskDescriptionText.text = result.getSummary()
        fallbackWarningText.visibility = if (result.usedFallbackAnalysis) View.VISIBLE else View.GONE

        analysisWarningBanner.visibility = if (result.analysisMode != AnalysisMode.SUCCESS) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if (result.hasHazards) {
            hazardsRecyclerView.adapter = HazardAdapter(result.hazards)
            riskMatrixView.setHazards(result.hazards)

            val recommendations = result.getAllRecommendations()
            recommendationsText.text = recommendations.take(5).joinToString("\n\n") { "• $it" }
        } else {
            hazardsRecyclerView.adapter = HazardAdapter(emptyList())
            recommendationsText.text = getString(R.string.no_hazards_message)
        }
    }

    private fun showLoading(show: Boolean, message: String? = null) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        message?.let { loadingText.text = it }
    }

    private fun updateLoadingMessage(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            loadingText.text = message
        }
    }

    private fun getRiskColor(riskLevel: RiskLevel): Int {
        return when (riskLevel) {
            RiskLevel.LOW -> Color.rgb(76, 175, 80)
            RiskLevel.MEDIUM -> Color.rgb(255, 193, 7)
            RiskLevel.HIGH -> Color.rgb(255, 152, 0)
            RiskLevel.VERY_HIGH -> Color.rgb(255, 87, 34)
            RiskLevel.EXTREME -> Color.rgb(211, 47, 47)
        }
    }

    private fun saveReport() {
        val result = assessmentResult ?: return

        showLoading(true, getString(R.string.saving_report))

        lifecycleScope.launch {
            val saveResult = withContext(Dispatchers.IO) {
                assessmentRepository.saveAssessment(result)
            }

            showLoading(false)

            when (saveResult) {
                is OperationResult.Success -> {
                    Toast.makeText(
                        this@ResultsActivity,
                        getString(R.string.report_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is OperationResult.Error -> {
                    Toast.makeText(
                        this@ResultsActivity,
                        getString(R.string.report_save_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
                OperationResult.Loading -> Unit
            }
        }
    }

    private fun shareReport() {
        val result = assessmentResult ?: return
        
        val shareText = buildString {
            append("HSE Risk Assessment Report\n\n")
            append("Overall Risk: ${result.overallRiskLevel.displayName}\n\n")
            if (result.analysisMode != AnalysisMode.SUCCESS) {
                append("⚠️ Analysis Warning: Some or all hazards are estimated placeholders due to analysis failure.\n\n")
            }
            append("Hazards Detected: ${result.hazards.size}\n\n")
            if (result.usedFallbackAnalysis) {
                append("⚠ ${getString(R.string.fallback_analysis_warning)}\n\n")
            }
            result.hazards.forEach { hazard ->
                append("• ${hazard.type.displayName} - ${hazard.riskLevel.displayName}\n")
            }
            append("\n${result.getSummary()}")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "HSE Risk Assessment Report")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Report"))
    }

    override fun onDestroy() {
        super.onDestroy()
        assessmentEngine.release()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}
