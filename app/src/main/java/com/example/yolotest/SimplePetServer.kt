package com.example.yolotest

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class SimplePetServer(private val context: Context, private val port: Int) {
    private lateinit var dbHelper: DatabaseHelper
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun start() {
        dbHelper = DatabaseHelper(context)
        serverSocket = ServerSocket(port)
        isRunning = true
        println("Server started on port $port")
        thread {
            while (isRunning) {
                val socket = serverSocket?.accept() ?: break
                thread { handleClient(socket) }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val writer = PrintWriter(socket.getOutputStream(), true)

            // 读取请求行
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val path = parts[1]

            // 读取 headers 直到空行
            var line: String?
            var contentLength = 0
            while (reader.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            // 读取 body
            val body = if (contentLength > 0) {
                val chars = CharArray(contentLength)
                reader.read(chars, 0, contentLength)
                String(chars)
            } else ""

            println("Request: $method $path, body: $body")

            when {
                path == "/appointment/delete" && method == "POST" -> handleDeleteAppointment(body, writer)
                path == "/appointment" && method == "GET" -> handleGetAppointmentById(writer, path)
                path == "/login" && method == "POST" -> handleLogin(body, writer)
                path == "/pets" && method == "POST" -> handleGetPets(body, writer)
                path == "/daily_care" && method == "POST" -> handleGetDailyCare(body, writer)
                path == "/appointment" && method == "POST" -> handleCreateAppointment(body, writer)
                path == "/appointment/cancel" && method == "POST" -> handleCancelAppointment(body, writer)
                path == "/appointments" && method == "POST" -> handleGetAppointments(body, writer)
                path == "/test" && method == "GET" -> handleTest(writer)
                path == "/appointments/pending" && method == "GET" -> handleGetPendingAppointments(writer)
                path == "/appointment/confirm" && method == "POST" -> handleConfirmAppointment(body, writer)
                path == "/appointments/pending" && method == "GET" -> handleGetPendingAppointments(writer)
                path == "/create_pet" && method == "POST" -> handleCreatePet(body, writer)
                path == "/update_backup_phone" && method == "POST" -> handleUpdateBackupPhone(body, writer)
                else -> sendResponse(writer, 404, "text/plain", "Not Found")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket.close()
        }
    }

    private fun handleLogin(body: String, writer: PrintWriter) {
        try {
            val json = JSONObject(body)
            val name = json.optString("name", "")
            val phone = json.optString("phone", "")
            println("Login: name=$name, phone=$phone")

            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT id FROM owners WHERE name = ? AND phone = ?",
                arrayOf(name, phone)
            )
            val ownerId = if (cursor.moveToFirst()) {
                cursor.getLong(0)
            } else {
                // 新用户：自动注册
                cursor.close()
                dbHelper.insertOwner(name, phone, "")
            }
            cursor.close()
            db.close()

            val response = JSONObject().apply {
                put("success", true)
                put("message", "登录成功")
                put("owner_id", ownerId)
            }
            // 关键：Content-Type 加上 charset=utf-8
            sendResponse(writer, 200, "application/json; charset=utf-8", response.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            sendResponse(writer, 500, "application/json; charset=utf-8", "{\"error\":\"Server error\"}")
        }
    }

    private fun handleGetAppointmentById(writer: PrintWriter, path: String) {
        try {
            val idStr = path.substringAfter("id=").substringBefore("&")
            val id = idStr.toLongOrNull()
            if (id == null) {
                sendResponse(writer, 400, "application/json", "{\"error\":\"Invalid id\"}")
                return
            }
            val appointment = dbHelper.getAppointmentById(id)
            if (appointment == null) {
                sendResponse(writer, 404, "application/json", "{\"error\":\"Not found\"}")
                return
            }
            // 修改这一行：增加空安全判断
            val pet = if (appointment.petId != null) dbHelper.getPetById(appointment.petId!!) else null
            val owner = if (pet != null) dbHelper.getOwnerById(pet.ownerId) else null
            val response = JSONObject().apply {
                put("id", appointment.id)
                put("pet_id", appointment.petId ?: JSONObject.NULL)
                put("pet_name", pet?.name ?: appointment.tempPetName ?: "")
                put("pet_species", pet?.species ?: appointment.tempPetSpecies ?: "")
                put("owner_name", owner?.name ?: "")
                put("backup_phone", owner?.backupPhone ?: "")
                put("start_time", appointment.startTime)
                if (appointment.endTime != null) put("end_time", appointment.endTime)
                put("notes", appointment.notes)
            }
            sendResponse(writer, 200, "application/json", response.toString())
        } catch (e: Exception) {
            sendResponse(writer, 500, "application/json", "{\"error\":\"Server error\"}")
        }
    }

    private fun handleConfirmAppointment(body: String, writer: PrintWriter) {
        try {
            val json = JSONObject(body)
            val appointmentId = json.getLong("appointment_id")
            val appointment = dbHelper.getAppointmentById(appointmentId)
            if (appointment == null) {
                sendResponse(writer, 404, "application/json", "{\"success\":false, \"error\":\"Appointment not found\"}")
                return
            }
            var petId = appointment.petId
            if (petId == null && appointment.tempPetName != null) {
                // 创建新宠物，使用预约中的 ownerId
                val ownerId = appointment.ownerId
                if (ownerId == null) {
                    sendResponse(writer, 400, "application/json", "{\"success\":false, \"error\":\"Missing owner id\"}")
                    return
                }
                petId = dbHelper.insertPet(
                    ownerId,
                    appointment.tempPetName,
                    appointment.tempPetSpecies ?: "未知",
                    appointment.tempPetAge ?: 0,
                    appointment.tempPetGender ?: "公"
                )
                // 更新预约记录中的 pet_id
                dbHelper.updateAppointmentPetId(appointmentId, petId)
            }
            if (petId == null) {
                sendResponse(writer, 400, "application/json", "{\"success\":false, \"error\":\"Cannot determine pet\"}")
                return
            }
            val endTime = appointment.endTime ?: (appointment.startTime + 7 * 24 * 60 * 60 * 1000L)
            val boardingId = dbHelper.insertBoardingRecord(petId, appointment.startTime, endTime, 1, appointment.notes)
            dbHelper.updateAppointmentStatus(appointmentId, "completed")
            sendResponse(writer, 200, "application/json", "{\"success\":true, \"boarding_id\":$boardingId}")
        } catch (e: Exception) {
            e.printStackTrace()
            sendResponse(writer, 500, "application/json", "{\"success\":false}")
        }
    }
    private fun handleUpdateBackupPhone(body: String, writer: PrintWriter) {
        try {
            val json = JSONObject(body)
            val ownerId = json.getLong("owner_id")
            val backupPhone = json.optString("backup_phone", "")
            dbHelper.updateBackupPhone(ownerId, backupPhone)
            sendResponse(writer, 200, "application/json", "{\"success\":true}")
        } catch (e: Exception) {
            sendResponse(writer, 500, "application/json", "{\"success\":false}")
        }
    }


    private fun handleGetPendingAppointments(writer: PrintWriter) {
        try {
            val list = dbHelper.getPendingAppointments()
            val jsonArray = JSONArray()
            for (a in list) {
                val obj = JSONObject().apply {
                    put("id", a.id)
                    put("pet_id", a.petId ?: JSONObject.NULL)
                    put("pet_name", a.petName)
                    put("start_time", a.startTime)
                    if (a.endTime != null) put("end_time", a.endTime)
                    put("notes", a.notes)
                    put("create_time", a.createTime)
                }
                jsonArray.put(obj)
            }
            sendResponse(writer, 200, "application/json", jsonArray.toString())
        } catch (e: Exception) {
            sendResponse(writer, 500, "application/json", "[]")
        }
    }
    private fun handleGetPets(body: String, writer: PrintWriter) {
        try {
            val json = JSONObject(body)
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
                sendResponse(writer, 401, "application/json", "{\"error\":\"Unauthorized\"}")
                return
            }
            val ownerId = ownerCursor.getLong(0)
            ownerCursor.close()

            val petsQuery = """
                SELECT p.id, p.name, p.species, p.age, p.gender,
                       b.id, b.start_time, b.end_time, b.is_active, b.process
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

            val responseObj = JSONObject().apply {
                put("pets", JSONArray(petsMap.values))
            }
            val responseStr = responseObj.toString()
            println("Response content: $responseStr")
            sendResponse(writer, 200, "application/json; charset=utf-8", responseStr)
        } catch (e: Exception) {
            e.printStackTrace()
            sendResponse(writer, 500, "application/json", "{\"error\":\"Server error\"}")
        }
    }

    private fun handleGetDailyCare(body: String, writer: PrintWriter) {
        try {
            val json = JSONObject(body)
            val boardingId = json.optLong("boarding_id", -1)
            if (boardingId == -1L) {
                sendResponse(writer, 400, "application/json", "{\"error\":\"Missing boarding_id\"}")
                return
            }
            println("Get daily care for boarding_id=$boardingId")
            val records = dbHelper.getDailyCareRecordsByBoardingId(boardingId)
            val jsonArray = JSONArray()
            for (record in records) {
                val obj = JSONObject().apply {
                    put("date", record.date)
                    put("foodBrand", record.foodBrand)
                    put("mealAmount", record.mealAmount)
                    put("waterAmount", record.waterAmount)
                    put("waterChangeTime", record.waterChangeTime)
                    put("remarks", record.remarks)
                    put("medicationUsed", record.medicationUsed)
                    put("medicationDose", record.medicationDose)
                    put("medicationTime", record.medicationTime)
                    put("medicationMethod", record.medicationMethod)
                    put("walkTime1", record.walkTime1)
                    put("walkTime2", record.walkTime2)
                    put("peeStatus", record.peeStatus)
                    put("poopStatus", record.poopStatus)
                    put("spiritStatus", record.spiritStatus)
                    put("appearanceStatus", record.appearanceStatus)
                }
                jsonArray.put(obj)
            }
            sendResponse(writer, 200, "application/json; charset=utf-8", jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            sendResponse(writer, 500, "application/json", "{\"error\":\"Server error\"}")
        }
    }

    // 新增预约接口
    private fun handleCreateAppointment(body: String, writer: PrintWriter) {
        println("handleCreateAppointment called, body: $body")
        try {
            val json = JSONObject(body)
            val ownerId = json.getLong("owner_id")
            val petId = if (json.has("pet_id") && !json.isNull("pet_id")) json.getLong("pet_id") else null
            val tempName = if (json.has("temp_pet_name")) json.getString("temp_pet_name") else null
            val tempSpecies = if (json.has("temp_pet_species")) json.getString("temp_pet_species") else null
            val tempAge = if (json.has("temp_pet_age")) json.getInt("temp_pet_age") else null
            val tempGender = if (json.has("temp_pet_gender")) json.getString("temp_pet_gender") else null
            val startTime = json.getLong("start_time")
            val endTime = if (json.has("end_time") && !json.isNull("end_time")) json.getLong("end_time") else null
            val notes = json.optString("notes", "")
            val id = dbHelper.insertAppointment(ownerId, petId, tempName, tempSpecies, tempAge, tempGender, startTime, endTime, notes)
            println("Inserted appointment id = $id")
            if (id == -1L) {
                println("Insert failed, check database")
            }
            sendResponse(writer, 200, "application/json", "{\"success\":true, \"id\":$id}")
        } catch (e: Exception) {
            e.printStackTrace()
            sendResponse(writer, 500, "application/json", "{\"success\":false, \"error\":\"${e.message}\"}")
        }
    }
    private fun handleDeleteAppointment(body: String, writer: PrintWriter) {
        try {
            val json = JSONObject(body)
            val appointmentId = json.getLong("appointment_id")
            val success = dbHelper.deleteAppointment(appointmentId)
            sendResponse(writer, 200, "application/json", "{\"success\":$success}")
        } catch (e: Exception) {
            sendResponse(writer, 500, "application/json", "{\"success\":false}")
        }
    }
    private fun handleCreatePet(body: String, writer: PrintWriter) {
        try {
            val json = JSONObject(body)
            val ownerId = json.getLong("owner_id")
            val name = json.getString("name")
            val species = json.optString("species", "")
            val age = json.getInt("age")
            val gender = json.getString("gender")
            val petId = dbHelper.insertPet(ownerId, name, species, age, gender)
            sendResponse(writer, 200, "application/json", "{\"success\":true, \"pet_id\":$petId}")
        } catch (e: Exception) {
            sendResponse(writer, 500, "application/json", "{\"success\":false}")
        }
    }

    private fun handleCancelAppointment(body: String, writer: PrintWriter) {
        try {
            val json = JSONObject(body)
            val appointmentId = json.getLong("appointment_id")
            val success = dbHelper.cancelAppointment(appointmentId)
            sendResponse(writer, 200, "application/json", "{\"success\":$success}")
        } catch (e: Exception) {
            sendResponse(writer, 500, "application/json", "{\"success\":false}")
        }
    }

    private fun handleGetAppointments(body: String, writer: PrintWriter) {
        try {
            val json = JSONObject(body)
            val ownerId = json.optLong("owner_id", -1)
            if (ownerId == -1L) {
                sendResponse(writer, 400, "application/json", "{\"error\":\"Missing owner_id\"}")
                return
            }
            val list = dbHelper.getAppointmentsByOwner(ownerId)
            val jsonArray = JSONArray()
            for (a in list) {
                val obj = JSONObject().apply {
                    put("id", a.id)
                    put("pet_id", a.petId ?: JSONObject.NULL)
                    put("pet_name", a.petName)
                    put("start_time", a.startTime)
                    if (a.endTime != null) put("end_time", a.endTime)
                    put("notes", a.notes)
                    put("status", a.status)
                    put("create_time", a.createTime)
                }
                jsonArray.put(obj)
            }
            sendResponse(writer, 200, "application/json", jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            sendResponse(writer, 500, "application/json", "[]")
        }
    }

    private fun handleTest(writer: PrintWriter) {
        sendResponse(writer, 200, "text/plain", "OK")
    }

    private fun sendResponse(writer: PrintWriter, statusCode: Int, contentType: String, content: String) {
        val statusLine = when (statusCode) {
            200 -> "HTTP/1.1 200 OK"
            400 -> "HTTP/1.1 400 Bad Request"
            401 -> "HTTP/1.1 401 Unauthorized"
            404 -> "HTTP/1.1 404 Not Found"
            500 -> "HTTP/1.1 500 Internal Server Error"
            else -> "HTTP/1.1 500 Internal Server Error"
        }
        writer.println(statusLine)
        writer.println("Content-Type: $contentType")
        writer.println("Content-Length: ${content.toByteArray(Charsets.UTF_8).size}")
        writer.println("Connection: close")
        writer.println()
        writer.print(content)
        writer.flush()
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
        dbHelper.close()
    }
}