package com.example.petowner

data class Pet(
    val id: Long,
    val name: String,
    val species: String,
    val age: Int,
    val gender: String,
    val activeRecords: List<BoardingRecord>,
    val completedRecords: List<BoardingRecord>
)

data class BoardingRecord(
    val id: Long,
    val startTime: Long,
    val endTime: Long,
    val isActive: Boolean,
    val process: String
)