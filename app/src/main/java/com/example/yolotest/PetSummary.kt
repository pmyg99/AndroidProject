package com.example.yolotest

data class PetSummary(
    val petId: Long,
    val petName: String,
    val species: String,
    val age: Int,
    val gender: String,
    val ownerName: String,
    val isBoarding: Boolean,      // 是否有正在寄养的记录
    val lastBoardingStartTime: Long? = null
)