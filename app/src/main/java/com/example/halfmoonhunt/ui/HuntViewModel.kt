package com.example.halfmoonhunt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.halfmoonhunt.model.Clue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HuntViewModel : ViewModel() {

    private val _timer = MutableStateFlow(0L)
    val timer = _timer.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timer.value++
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
    }

    fun stopTimer() {
        _timer.value = 0
        timerJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    private val _clues = MutableStateFlow<List<Clue>>(emptyList())
    val clues = _clues.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    fun loadClues() {
        _clues.value = listOf(
            Clue(
                text = "Not all giants walk the land... some crash into cliffs.",
                hint = "You won’t need a jet to find this Maverick, but the waves still fly high.",
                lat = 37.501630,
                lon = -122.496700,
                threshold = 500.0
            ),
            Clue(
                text = "No curtains, no lights... just sand, sea, and sky performing on nature’s stage.",
                hint = "It's called Francis, but don’t expect a sermon... this beachside stage speaks in wind and waves.",
                lat = 37.470194,
                lon = -122.446411,
                threshold = 200.0
            )
        )
        _currentIndex.value = 0
    }

    fun resetHunt() {
        _currentIndex.value = 0
        stopTimer()
    }

    fun isLastClue(): Boolean = _currentIndex.value >= (_clues.value.size - 1)

    fun advance(): Boolean {
        val last = isLastClue()
        if (!last) {
            _currentIndex.value = _currentIndex.value + 1
            return false
        }
        return true
    }

}