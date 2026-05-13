package com.example.petowner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Appointment(
    val id: Long,
    val petId: Long?,          // 改为可空
    val petName: String,
    val startTime: Long,
    val endTime: Long?,
    val notes: String,
    val status: String,
    val createTime: Long
) : Parcelable