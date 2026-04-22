package com.example.petowner

data class PetSummary(
    val petId: Long,
    val name: String,
    val species: String,
    val age: Int,
    val gender: String,
    val activeRecords: List<BoardingRecord>,
    val completedRecords: List<BoardingRecord>
)