package com.example.halfmoonhunt.model

import kotlinx.serialization.Serializable

@Serializable
data class Clue(
    val text: String,
    val hint: String,
    val lat: Double,
    val lon: Double,
    val threshold: Double,
    val solved: SolvedInfo
)

@Serializable
data class SolvedInfo(
    val title: String,
    val body: String,
    val facts: String
)

@Serializable
data class GameData(
    val rules: List<String>,
    val clues: List<Clue>
)