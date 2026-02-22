package com.hse.visualriskassessor.ui.history

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.hse.visualriskassessor.HSEApplication
import com.hse.visualriskassessor.R
import com.hse.visualriskassessor.model.AssessmentResult
import com.hse.visualriskassessor.model.OperationResult
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var historyAdapter: HistoryAdapter
    private var cachedAssessments: List<AssessmentResult> = emptyList()

    private val assessmentRepository by lazy {
        (application as HSEApplication).assessmentRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        setupViews()
        loadHistory()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_CLEAR_ALL, 0, getString(R.string.btn_clear_all))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == MENU_CLEAR_ALL) {
            promptClearAll()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupViews() {
        findViewById<MaterialToolbar>(R.id.toolbar)?.apply {
            setSupportActionBar(this)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setNavigationOnClickListener { finish() }
        }

        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        emptyView = findViewById(R.id.emptyView)

        historyAdapter = HistoryAdapter(emptyList(), ::promptDelete)
        historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }

        updateEmptyState(true)
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                assessmentRepository.getAssessments().collect { assessments ->
                    cachedAssessments = assessments
                    historyAdapter.submitList(assessments)
                    updateEmptyState(assessments.isEmpty())
                }
            }
        }
    }

    private fun promptDelete(assessment: AssessmentResult) {
        AlertDialog.Builder(this)
            .setTitle(R.string.btn_delete)
            .setMessage(R.string.delete_confirmation)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                deleteAssessment(assessment)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun deleteAssessment(assessment: AssessmentResult) {
        lifecycleScope.launch {
            when (assessmentRepository.deleteAssessment(assessment)) {
                is OperationResult.Success -> Toast.makeText(this@HistoryActivity, R.string.delete_success, Toast.LENGTH_SHORT).show()
                is OperationResult.Error -> Toast.makeText(this@HistoryActivity, R.string.delete_failed, Toast.LENGTH_LONG).show()
                OperationResult.Loading -> Unit
            }
        }
    }

    private fun promptClearAll() {
        if (cachedAssessments.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle(R.string.btn_clear_all)
            .setMessage(R.string.clear_all_confirmation)
            .setPositiveButton(R.string.btn_clear_all) { _, _ ->
                clearAllAssessments()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun clearAllAssessments() {
        lifecycleScope.launch {
            when (assessmentRepository.clearAssessments(cachedAssessments)) {
                is OperationResult.Success -> Toast.makeText(this@HistoryActivity, R.string.clear_all_success, Toast.LENGTH_SHORT).show()
                is OperationResult.Error -> Toast.makeText(this@HistoryActivity, R.string.clear_all_failed, Toast.LENGTH_LONG).show()
                OperationResult.Loading -> Unit
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        historyRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    companion object {
        private const val MENU_CLEAR_ALL = 100
    }
}
