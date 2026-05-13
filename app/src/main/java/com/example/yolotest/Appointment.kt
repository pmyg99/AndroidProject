package com.example.yolotest

data class Appointment(
    val id: Long,
    val ownerId: Long,
    val petId: Long? = null,
    val petName: String = "",
    val startTime: Long,
    val endTime: Long? = null,
    val notes: String = "",
    val status: String = "pending",
    val createTime: Long = 0,
    val tempPetName: String? = null,
    val tempPetSpecies: String? = null,
    val tempPetAge: Int? = null,
    val tempPetGender: String? = null
)