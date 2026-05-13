package com.example.petowner

import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

object ApiClient {
    private val requestQueue by lazy { Volley.newRequestQueue(MyApp.getContext()) }
    private const val BASE_URL = "http://127.0.0.1:8080"

    // 登录
    fun login(name: String, phone: String, callback: (Boolean, Long, String?) -> Unit) {
        val url = "$BASE_URL/login"
        val params = JSONObject().apply {
            put("name", name)
            put("phone", phone)
        }
        val request = object : StringRequest(Request.Method.POST, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val success = json.optBoolean("success", false)
                    val ownerId = if (success) json.optLong("owner_id", 0) else 0
                    val message = json.optString("message")
                    callback(success, ownerId, message)
                } catch (e: Exception) {
                    callback(false, 0, "解析错误")
                }
            },
            { callback(false, 0, "网络错误") }
        ) {
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
            override fun getBody(): ByteArray = params.toString().toByteArray(Charsets.UTF_8)
        }
        request.retryPolicy = DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        requestQueue.add(request)
    }

    // 获取宠物列表
    fun getPets(name: String, phone: String, callback: (List<Pet>?) -> Unit) {
        val url = "$BASE_URL/pets"
        val params = JSONObject().apply {
            put("name", name)
            put("phone", phone)
        }
        val request = object : StringRequest(Request.Method.POST, url,
            { responseStr ->
                try {
                    val jsonObject = JSONObject(responseStr)
                    val jsonArray = jsonObject.getJSONArray("pets")
                    val pets = parsePets(jsonArray)
                    callback(pets)
                } catch (e: Exception) {
                    callback(null)
                }
            },
            { callback(null) }
        ) {
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
            override fun getBody(): ByteArray = params.toString().toByteArray(Charsets.UTF_8)
        }
        requestQueue.add(request)
    }

    fun createAppointmentWithPet(ownerId: Long, petId: Long, startTime: Long, endTime: Long?, notes: String, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL/appointment"
        val params = JSONObject().apply {
            put("owner_id", ownerId)
            put("pet_id", petId)
            put("start_time", startTime)
            if (endTime != null) put("end_time", endTime)
            put("notes", notes)
        }
        sendAppointmentRequest(params, callback)
    }

    fun createAppointmentWithNewPet(ownerId: Long, tempName: String, tempSpecies: String, tempAge: Int, tempGender: String, startTime: Long, endTime: Long?, notes: String, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL/appointment"
        val params = JSONObject().apply {
            put("owner_id", ownerId)
            put("temp_pet_name", tempName)
            put("temp_pet_species", tempSpecies)
            put("temp_pet_age", tempAge)
            put("temp_pet_gender", tempGender)
            put("start_time", startTime)
            if (endTime != null) put("end_time", endTime)
            put("notes", notes)
        }
        sendAppointmentRequest(params, callback)
    }
    private fun sendAppointmentRequest(params: JSONObject, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL/appointment"
        val request = object : StringRequest(Request.Method.POST, url,
            { response ->
                val success = JSONObject(response).optBoolean("success", false)
                callback(success)
            },
            { callback(false) }
        ) {
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
            override fun getBody(): ByteArray = params.toString().toByteArray(Charsets.UTF_8)
        }
        request.retryPolicy = DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        requestQueue.add(request)
    }

    // 更新备用电话
    fun updateBackupPhone(ownerId: Long, backupPhone: String, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL/update_backup_phone"
        val params = JSONObject().apply {
            put("owner_id", ownerId)
            put("backup_phone", backupPhone)
        }
        val request = object : StringRequest(Request.Method.POST, url,
            { response ->
                val success = JSONObject(response).optBoolean("success", false)
                callback(success)
            },
            { callback(false) }
        ) {
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
            override fun getBody(): ByteArray = params.toString().toByteArray(Charsets.UTF_8)
        }
        requestQueue.add(request)
    }

    // 获取当前用户的预约列表
    fun getMyAppointments(ownerId: Long, callback: (List<Appointment>?) -> Unit) {
        val url = "$BASE_URL/appointments"
        val params = JSONObject().apply { put("owner_id", ownerId) }
        val request = object : StringRequest(Request.Method.POST, url,
            { responseStr ->
                try {
                    val jsonArray = JSONArray(responseStr)
                    val list = mutableListOf<Appointment>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            Appointment(
                                id = obj.getLong("id"),
                                petId = if (obj.has("pet_id") && !obj.isNull("pet_id")) obj.getLong("pet_id") else null,                                petName = obj.getString("pet_name"),
                                startTime = obj.getLong("start_time"),
                                endTime = if (obj.has("end_time") && !obj.isNull("end_time")) obj.getLong("end_time") else null,
                                notes = obj.optString("notes"),
                                status = obj.getString("status"),
                                createTime = obj.getLong("create_time")
                            )
                        )
                    }
                    callback(list)
                } catch (e: Exception) {
                    callback(null)
                }
            },
            { callback(null) }
        ) {
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
            override fun getBody(): ByteArray = params.toString().toByteArray(Charsets.UTF_8)
        }
        requestQueue.add(request)
    }

    // 取消预约
    fun cancelAppointment(appointmentId: Long, callback: (Boolean) -> Unit) {
        val url = "$BASE_URL/appointment/cancel"
        val params = JSONObject().apply { put("appointment_id", appointmentId) }
        val request = object : StringRequest(Request.Method.POST, url,
            { response ->
                val success = JSONObject(response).optBoolean("success", false)
                callback(success)
            },
            { callback(false) }
        ) {
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
            override fun getBody(): ByteArray = params.toString().toByteArray(Charsets.UTF_8)
        }
        requestQueue.add(request)
    }

    private fun parsePets(jsonArray: JSONArray): List<Pet> {
        val list = mutableListOf<Pet>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val pet = Pet(
                id = obj.getLong("pet_id"),
                name = obj.getString("name"),
                species = obj.getString("species"),
                age = obj.getInt("age"),
                gender = obj.getString("gender"),
                activeRecords = parseRecords(obj.getJSONArray("active_records")),
                completedRecords = parseRecords(obj.getJSONArray("completed_records"))
            )
            list.add(pet)
        }
        return list
    }

    private fun parseRecords(jsonArray: JSONArray): List<BoardingRecord> {
        val list = mutableListOf<BoardingRecord>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                BoardingRecord(
                    id = obj.getLong("boarding_id"),
                    startTime = obj.getLong("start_time"),
                    endTime = obj.getLong("end_time"),
                    isActive = obj.getBoolean("is_active"),
                    process = obj.getString("process")
                )
            )
        }
        return list
    }
    // 获取每日看护记录
    fun getDailyCareRecords(boardingId: Long, callback: (List<DailyCareRecord>?) -> Unit) {
        val url = "$BASE_URL/daily_care"
        val params = JSONObject().apply {
            put("boarding_id", boardingId)
        }
        val request = object : StringRequest(Request.Method.POST, url,
            { responseStr ->
                try {
                    val jsonArray = JSONArray(responseStr)
                    val records = parseDailyCareRecords(jsonArray)
                    callback(records)
                } catch (e: Exception) {
                    Log.e("ApiClient", "Parse daily care error", e)
                    callback(null)
                }
            },
            { error ->
                Log.e("ApiClient", "Volley error getting daily care", error)
                callback(null)
            }
        ) {
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
            override fun getBody(): ByteArray = params.toString().toByteArray(Charsets.UTF_8)
        }
        request.retryPolicy = DefaultRetryPolicy(30000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        requestQueue.add(request)
    }

    // 解析每日看护记录
    private fun parseDailyCareRecords(jsonArray: JSONArray): List<DailyCareRecord> {
        val list = mutableListOf<DailyCareRecord>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val record = DailyCareRecord(
                date = obj.getLong("date"),
                foodBrand = obj.optString("foodBrand", ""),
                mealAmount = obj.optString("mealAmount", ""),
                waterAmount = obj.optString("waterAmount", ""),
                waterChangeTime = obj.optString("waterChangeTime", ""),
                remarks = obj.optString("remarks", ""),
                medicationUsed = obj.optString("medicationUsed", ""),
                medicationDose = obj.optString("medicationDose", ""),
                medicationTime = obj.optString("medicationTime", ""),
                medicationMethod = obj.optString("medicationMethod", ""),
                walkTime1 = obj.optString("walkTime1", "08:00"),
                walkTime2 = obj.optString("walkTime2", "18:00"),
                peeStatus = obj.optString("peeStatus", ""),
                poopStatus = obj.optString("poopStatus", ""),
                spiritStatus = obj.optString("spiritStatus", ""),
                appearanceStatus = obj.optString("appearanceStatus", "")
            )
            list.add(record)
        }
        return list
    }
}