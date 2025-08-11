package com.example.halfmoonhunt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.halfmoonhunt.model.Clue
import com.example.halfmoonhunt.model.SolvedInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class HuntViewModel(app: Application) : AndroidViewModel(app) {

    private val json = Json { ignoreUnknownKeys = true }
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
        val raw = getApplication<Application>()
            .resources
            .openRawResource(R.raw.clues)
            .bufferedReader()
            .use { it.readText() }
        _clues.value = json.decodeFromString(raw)
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

    fun currentSolved(): SolvedInfo? = _clues.value.getOrNull(_currentIndex.value)?.solved

}