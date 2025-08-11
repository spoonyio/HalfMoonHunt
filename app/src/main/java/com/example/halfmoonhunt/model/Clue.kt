package com.example.halfmoonhunt.model

data class Clue(
    val text: String,
    val hint: String,
    val lat: Double,
    val lon: Double,
    val threshold: Double
)

data class SolvedInfo(
    val title: String,
    val body: String,
    val facts: String
)