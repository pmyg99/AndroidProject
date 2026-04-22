package com.example.petowner

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private const val BASE_URL = "http://127.0.0.1:8080"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun getPets(name: String, phone: String, callback: (List<Pet>?) -> Unit) {
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("$BASE_URL/pets")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                // 构建 JSON 请求体
                val jsonBody = JSONObject().apply {
                    put("name", name)
                    put("phone", phone)
                }.toString()
                Log.d("ApiClient", "Sending: $jsonBody")

                // 写入请求体（UTF-8）
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }

                // 获取响应码
                val responseCode = connection.responseCode
                Log.d("ApiClient", "Response code: $responseCode")

                val responseBody = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                }
                Log.d("ApiClient", "Response body: $responseBody")

                if (responseCode == 200) {
                    val jsonObject = JSONObject(responseBody)
                    val petsArray = jsonObject.getJSONArray("pets")
                    val pets = parsePets(petsArray)
                    mainHandler.post { callback(pets) }
                } else {
                    Log.e("ApiClient", "HTTP error: $responseCode")
                    mainHandler.post { callback(null) }
                }
            } catch (e: Exception) {
                Log.e("ApiClient", "Request failed", e)
                mainHandler.post { callback(null) }
            } finally {
                connection?.disconnect()
            }
        }.start()
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
            val record = BoardingRecord(
                id = obj.getLong("boarding_id"),
                startTime = obj.getLong("start_time"),
                endTime = obj.getLong("end_time"),
                isActive = obj.getBoolean("is_active"),
                process = obj.getString("process")
            )
            list.add(record)
        }
        return list
    }
}