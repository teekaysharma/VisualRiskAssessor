package com.hse.visualriskassessor.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hse.visualriskassessor.data.repository.AssessmentRepository
import com.hse.visualriskassessor.model.AssessmentResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: AssessmentRepository) : ViewModel() {

    val assessments: StateFlow<List<AssessmentResult>> = repository.allAssessments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteAssessment(result: AssessmentResult) {
        viewModelScope.launch {
            repository.deleteAssessment(result)
        }
    }

    class Factory(private val repository: AssessmentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
