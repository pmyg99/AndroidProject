data class DailyCareRecord(
    val date: Long,
    val foodBrand: String = "",
    val mealAmount: String = "",
    val waterAmount: String = "",
    val waterChangeTime: String = "",
    val remarks: String = "",
    val medicationUsed: String = "",
    val medicationDose: String = "",
    val medicationTime: String = "",
    val medicationMethod: String = "",
    val walkTime1: String = "08:00",
    val walkTime2: String = "18:00",
    val peeStatus: String = "",
    val poopStatus: String = "",
    val spiritStatus: String = "",
    val appearanceStatus: String = ""
)