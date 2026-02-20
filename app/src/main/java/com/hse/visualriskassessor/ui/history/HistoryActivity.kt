package com.hse.visualriskassessor.ui.history

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.hse.visualriskassessor.HSEApplication
import com.hse.visualriskassessor.R
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var historyAdapter: HistoryAdapter

    private val assessmentRepository by lazy {
        (application as HSEApplication).assessmentRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        setupViews()
        loadHistory()
    }

    private fun setupViews() {
        findViewById<MaterialToolbar>(R.id.toolbar)?.apply {
            setSupportActionBar(this)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setNavigationOnClickListener { finish() }
        }

        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        emptyView = findViewById(R.id.emptyView)

        historyAdapter = HistoryAdapter(emptyList())
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
                    historyAdapter.submitList(assessments)
                    updateEmptyState(assessments.isEmpty())
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        historyRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
