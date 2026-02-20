package com.hse.visualriskassessor.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.hse.visualriskassessor.R
import com.hse.visualriskassessor.data.database.AppDatabase
import com.hse.visualriskassessor.data.repository.AssessmentRepository
import com.hse.visualriskassessor.model.AssessmentResult
import com.hse.visualriskassessor.ui.results.ResultsActivity
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var emptyView: LinearLayout
    private lateinit var adapter: HistoryAdapter

    private val viewModel: HistoryViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        HistoryViewModel.Factory(AssessmentRepository(db.assessmentDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        setupViews()
        observeHistory()
    }

    private fun setupViews() {
        findViewById<MaterialToolbar>(R.id.toolbar)?.apply {
            setSupportActionBar(this)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setNavigationOnClickListener { finish() }
        }

        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        emptyView = findViewById(R.id.emptyView)

        adapter = HistoryAdapter(
            onItemClick = { result -> openResult(result) },
            onItemLongClick = { result ->
                showDeleteConfirmation(result)
                true
            }
        )

        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyRecyclerView.adapter = adapter
    }

    private fun observeHistory() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.assessments.collect { assessments ->
                    adapter.submitList(assessments)
                    updateEmptyState(assessments.isEmpty())
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            historyRecyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            historyRecyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }

    private fun openResult(result: AssessmentResult) {
        val intent = Intent(this, ResultsActivity::class.java).apply {
            putExtra(ResultsActivity.EXTRA_IMAGE_URI, "file://${result.imagePath}")
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmation(result: AssessmentResult) {
        AlertDialog.Builder(this)
            .setTitle(R.string.btn_delete)
            .setMessage(R.string.delete_confirmation)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewModel.deleteAssessment(result)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    companion object {
        private const val TAG = "HistoryActivity"
    }
}
