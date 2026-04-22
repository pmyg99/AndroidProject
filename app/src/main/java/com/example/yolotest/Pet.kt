package com.example.yolotest

data class Pet(
    val id: Long,
    val name: String,
    val species: String,
    val age: Int,
    val gender: String,
    val ownerId: Long,
    val ownerName: String = "",
    val phone: String = "",
    val backupPhone: String = ""
)