package com.example.petowner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BoardingRecord(
    val id: Long,
    val startTime: Long,
    val endTime: Long,
    val isActive: Boolean,
    val process: String
) : Parcelable