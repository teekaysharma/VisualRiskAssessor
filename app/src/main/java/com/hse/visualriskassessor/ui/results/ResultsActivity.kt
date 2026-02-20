package com.hse.visualriskassessor.ui.results

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
import com.hse.visualriskassessor.analysis.RiskAssessmentEngine
import com.hse.visualriskassessor.data.database.AppDatabase
import com.hse.visualriskassessor.data.repository.AssessmentRepository
import com.hse.visualriskassessor.model.AssessmentResult
import com.hse.visualriskassessor.model.RiskLevel
import com.hse.visualriskassessor.ui.MainActivity
import com.hse.visualriskassessor.ui.widget.RiskMatrixView
import com.hse.visualriskassessor.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultsActivity : AppCompatActivity() {

    private lateinit var assessmentEngine: RiskAssessmentEngine
    private lateinit var repository: AssessmentRepository
    private var assessmentResult: AssessmentResult? = null

    private lateinit var analyzedImage: ImageView
    private lateinit var riskLevelText: TextView
    private lateinit var riskDescriptionText: TextView
    private lateinit var hazardsRecyclerView: RecyclerView
    private lateinit var riskMatrixView: RiskMatrixView
    private lateinit var recommendationsText: TextView
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var loadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        assessmentEngine = RiskAssessmentEngine(this)
        val db = AppDatabase.getInstance(applicationContext)
        repository = AssessmentRepository(db.assessmentDao())

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
        hazardsRecyclerView = findViewById(R.id.hazardsRecyclerView)
        riskMatrixView = findViewById(R.id.riskMatrixView)
        recommendationsText = findViewById(R.id.recommendationsText)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingText = findViewById(R.id.loadingText)

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
                saveAssessmentToHistory(result)
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

    private suspend fun saveAssessmentToHistory(result: AssessmentResult) {
        try {
            repository.saveAssessment(result)
        } catch (e: Exception) {
            // History save failure is non-critical - results still shown
        }
    }

    private fun displayResults(result: AssessmentResult) {
        riskLevelText.text = result.overallRiskLevel.displayName
        riskLevelText.setTextColor(getRiskColor(result.overallRiskLevel))

        riskDescriptionText.text = result.getSummary()

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

        lifecycleScope.launch {
            try {
                val reportContent = buildReportContent(result)
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "HSE_Risk_Report_$timestamp.txt"

                withContext(Dispatchers.IO) {
                    writeReportToDownloads(fileName, reportContent)
                }

                Toast.makeText(this@ResultsActivity, getString(R.string.report_saved), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@ResultsActivity, getString(R.string.report_save_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writeReportToDownloads(fileName: String, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create file in Downloads")
            contentResolver.openOutputStream(uri)?.use { stream ->
                writeContent(stream, content)
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            file.writeText(content)
        }
    }

    private fun writeContent(stream: OutputStream, content: String) {
        stream.write(content.toByteArray(Charsets.UTF_8))
    }

    private fun buildReportContent(result: AssessmentResult): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return buildString {
            appendLine("=" .repeat(60))
            appendLine("HSE RISK ASSESSMENT REPORT")
            appendLine("Visual Risk Assessor - ISO 45001 Aligned")
            appendLine("=".repeat(60))
            appendLine()
            appendLine("Date: ${dateFormat.format(result.timestamp)}")
            appendLine("Assessment ID: ${result.id}")
            appendLine("Analysis Time: ${result.analysisTimeMs}ms")
            appendLine()
            appendLine("-".repeat(60))
            appendLine("OVERALL RISK LEVEL: ${result.overallRiskLevel.displayName}")
            appendLine("-".repeat(60))
            appendLine()
            appendLine(result.getSummary())
            appendLine()
            appendLine("-".repeat(60))
            appendLine("HAZARDS IDENTIFIED (${result.hazards.size})")
            appendLine("-".repeat(60))
            appendLine()
            if (result.hazards.isEmpty()) {
                appendLine("No significant hazards detected.")
            } else {
                result.hazards.forEachIndexed { index, hazard ->
                    appendLine("${index + 1}. ${hazard.type.displayName}")
                    appendLine("   Risk Level: ${hazard.riskLevel.displayName}")
                    appendLine("   Likelihood: ${hazard.likelihood}/5")
                    appendLine("   Severity:   ${hazard.severity}/5")
                    appendLine("   Risk Score: ${hazard.riskScore}/25")
                    appendLine("   Confidence: ${(hazard.confidence * 100).toInt()}%")
                    hazard.details?.let { appendLine("   Details: $it") }
                    hazard.location?.let { appendLine("   Location: $it") }
                    appendLine()
                }
            }
            appendLine("-".repeat(60))
            appendLine("RECOMMENDED CONTROL MEASURES")
            appendLine("-".repeat(60))
            appendLine()
            val recommendations = result.getAllRecommendations()
            if (recommendations.isEmpty()) {
                appendLine("Continue regular safety monitoring procedures.")
            } else {
                recommendations.forEachIndexed { index, rec ->
                    appendLine("${index + 1}. $rec")
                }
            }
            appendLine()
            appendLine("=".repeat(60))
            appendLine("DISCLAIMER: This report is generated by an automated")
            appendLine("system and should be reviewed by a qualified safety")
            appendLine("professional. Always follow your organisation's safety")
            appendLine("procedures and local regulations.")
            appendLine("=".repeat(60))
        }
    }

    private fun shareReport() {
        val result = assessmentResult ?: return

        val shareText = buildString {
            append("HSE Risk Assessment Report\n\n")
            append("Overall Risk: ${result.overallRiskLevel.displayName}\n\n")
            append("Hazards Detected: ${result.hazards.size}\n\n")
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
