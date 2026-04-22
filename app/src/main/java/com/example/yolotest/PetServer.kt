package com.example.yolotest

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class PetServer(private val context: Context, private val port: Int) : NanoHTTPD(port) {

    private lateinit var dbHelper: DatabaseHelper

    @Throws(IOException::class)
    override fun start() {
        dbHelper = DatabaseHelper(context)
        super.start()
        println("Server started on port $port")
    }

    override fun serve(session: IHTTPSession): Response {
        return try {
            val uri = session.uri
            val method = session.method
            println("Received request: $method $uri")
            when {
                uri == "/login" && method == Method.POST -> handleLogin(session)
                uri == "/pets" && method == Method.POST -> handleGetPets(session)
                uri == "/test" && method == Method.GET -> newFixedLengthResponse(Status.OK, "text/plain", "OK")
                else -> newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "Not Found")
            }
        } catch (e: Exception) {
            println("Error in serve: ${e.message}")
            e.printStackTrace()
            newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", "Server Error: ${e.message}")
        }
    }

    private fun handleLogin(session: IHTTPSession): Response {
        val json = getJsonBody(session)
        val name = json.optString("name", "")
        val phone = json.optString("phone", "")
        println("Login: name=$name, phone=$phone")
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id FROM owners WHERE name = ? AND phone = ?",
            arrayOf(name, phone)
        )
        val valid = cursor.count > 0
        cursor.close()
        db.close()
        val response = JSONObject().apply {
            put("success", valid)
            put("message", if (valid) "登录成功" else "姓名或联系方式错误")
        }
        return newFixedLengthResponse(Status.OK, "application/json", response.toString())
    }

    private fun handleGetPets(session: IHTTPSession): Response {
        return try {
            val json = getJsonBody(session)
            val name = json.optString("name", "")
            val phone = json.optString("phone", "")
            println("Get pets: name=$name, phone=$phone")

            val db = dbHelper.readableDatabase
            val ownerCursor = db.rawQuery(
                "SELECT id FROM owners WHERE name = ? AND phone = ?",
                arrayOf(name, phone)
            )
            if (!ownerCursor.moveToFirst()) {
                ownerCursor.close()
                db.close()
                println("Owner not found")
                return newFixedLengthResponse(
                    Status.UNAUTHORIZED, "application/json",
                    JSONObject().put("error", "Unauthorized").toString()
                )
            }
            val ownerId = ownerCursor.getLong(0)
            ownerCursor.close()
            println("Owner found, id=$ownerId")

            val petsQuery = """
        SELECT 
            p.id as pet_id, p.name as pet_name, p.species, p.age, p.gender,
            b.id as boarding_id, b.start_time, b.end_time, b.is_active, b.process
        FROM pets p
        LEFT JOIN boarding_records b ON p.id = b.pet_id
        WHERE p.owner_id = ?
        ORDER BY p.name, b.start_time DESC
    """.trimIndent()
            val petCursor = db.rawQuery(petsQuery, arrayOf(ownerId.toString()))
            val petsMap = mutableMapOf<Long, JSONObject>()
            while (petCursor.moveToNext()) {
                val petId = petCursor.getLong(0)
                if (!petsMap.containsKey(petId)) {
                    val petJson = JSONObject().apply {
                        put("pet_id", petId)
                        put("name", petCursor.getString(1))
                        put("species", petCursor.getString(2))
                        put("age", petCursor.getInt(3))
                        put("gender", petCursor.getString(4))
                        put("active_records", JSONArray())
                        put("completed_records", JSONArray())
                    }
                    petsMap[petId] = petJson
                }
                if (!petCursor.isNull(5)) {
                    val recordJson = JSONObject().apply {
                        put("boarding_id", petCursor.getLong(5))
                        put("start_time", petCursor.getLong(6))
                        put("end_time", petCursor.getLong(7))
                        put("is_active", petCursor.getInt(8) == 1)
                        put("process", petCursor.getString(9))
                    }
                    val petJson = petsMap[petId]!!
                    if (recordJson.getBoolean("is_active")) {
                        petJson.getJSONArray("active_records").put(recordJson)
                    } else {
                        petJson.getJSONArray("completed_records").put(recordJson)
                    }
                }
            }
            petCursor.close()
            db.close()
// 构建响应 JSON 对象
            val responseObj = JSONObject().apply {
                put("pets", JSONArray(petsMap.values))
            }
            val responseStr = responseObj.toString()
            println("Response content: $responseStr")

// 手动创建响应，确保 Content-Length 正确
            val response = newFixedLengthResponse(Status.OK, "application/json; charset=utf-8", responseStr)
            response.addHeader("Content-Length", responseStr.toByteArray(Charsets.UTF_8).size.toString())
            response.addHeader("Connection", "close")
            return response
        } catch (e: Exception) {
            println("Error in handleGetPets: ${e.message}")
            e.printStackTrace()
            newFixedLengthResponse(
                Status.INTERNAL_ERROR,
                "application/json",
                "{\"error\":\"Server error\"}"
            )
        }
    }
    private fun getJsonBody(session: IHTTPSession): JSONObject {
        return try {
            val inputStream = session.getInputStream()
            val body = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            println("Request body: $body")
            JSONObject(body)
        } catch (e: Exception) {
            println("Error reading JSON body: ${e.message}")
            JSONObject()
        }
    }

    override fun stop() {
        super.stop()
        dbHelper.close()
    }
}