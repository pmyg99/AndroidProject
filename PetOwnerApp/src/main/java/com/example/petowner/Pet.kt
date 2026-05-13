package com.example.petowner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pet(
    val id: Long,
    val name: String,
    val species: String,
    val age: Int,
    val gender: String,
    val activeRecords: List<BoardingRecord>,
    val completedRecords: List<BoardingRecord>,
    var hasPendingAppointment: Boolean = false  // 新增字段，用于高亮显示
) : Parcelable