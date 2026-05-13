package com.example.petowner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DailyCareRecord(
    val date: Long,
    val foodBrand: String,
    val mealAmount: String,
    val waterAmount: String,
    val waterChangeTime: String,
    val remarks: String,
    val medicationUsed: String,
    val medicationDose: String,
    val medicationTime: String,
    val medicationMethod: String,
    val walkTime1: String,
    val walkTime2: String,
    val peeStatus: String,
    val poopStatus: String,
    val spiritStatus: String,
    val appearanceStatus: String
) : Parcelable