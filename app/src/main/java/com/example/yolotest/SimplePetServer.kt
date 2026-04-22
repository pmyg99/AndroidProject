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
                path == "/pets" && method == "POST" -> handleGetPets(body, writer)
                path == "/test" && method == "GET" -> handleTest(writer)
                else -> sendResponse(writer, 404, "text/plain", "Not Found")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket.close()
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

    private fun handleTest(writer: PrintWriter) {
        sendResponse(writer, 200, "text/plain", "OK")
    }

    private fun sendResponse(writer: PrintWriter, statusCode: Int, contentType: String, content: String) {
        val statusLine = when (statusCode) {
            200 -> "HTTP/1.1 200 OK"
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