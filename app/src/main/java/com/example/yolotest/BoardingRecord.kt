package com.example.yolotest

data class BoardingRecord(
    val id: Long,
    val petId: Long,
    val startTime: Long,
    val endTime: Long,
    val isActive: Boolean,
    val process: String,
    val petName: String = "",
    val index: Int = 0   // 新增序号，默认0
)