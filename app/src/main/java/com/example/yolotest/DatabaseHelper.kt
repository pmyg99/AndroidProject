package com.example.yolotest

import DailyCareRecord
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "pet_boarding.db"
        private const val DATABASE_VERSION = 21

        // 主人表
        const val TABLE_OWNERS = "owners"
        const val COL_OWNER_ID = "id"
        const val COL_OWNER_NAME = "name"
        const val COL_OWNER_PHONE = "phone"
        const val COL_OWNER_BACKUP_PHONE = "backup_phone"

        // 宠物表（仅静态信息）
        const val TABLE_PETS = "pets"
        const val COL_PET_ID = "id"
        const val COL_PET_NAME = "name"
        const val COL_PET_SPECIES = "species"
        const val COL_PET_AGE = "age"
        const val COL_PET_GENDER = "gender"
        const val COL_PET_OWNER_ID = "owner_id"

        // 寄养记录表（每次寄养一条记录）
        const val TABLE_BOARDING_RECORDS = "boarding_records"
        const val COL_BOARDING_ID = "id"
        const val COL_BOARDING_PET_ID = "pet_id"
        const val COL_BOARDING_START_TIME = "start_time"
        const val COL_BOARDING_END_TIME = "end_time"
        const val COL_BOARDING_IS_ACTIVE = "is_active"  // 1=正在寄养，0=已完成
        const val COL_BOARDING_PROCESS = "process"      // 寄养过程备注（可选）

        // 日常看护记录表（关联寄养记录）
        const val TABLE_DAILY_CARE = "daily_care_records"
        const val COL_CARE_ID = "id"
        const val COL_CARE_BOARDING_ID = "boarding_id"
        const val COL_CARE_DATE = "care_date"
        // 日常看护详细字段
        const val COL_FOOD_BRAND = "food_brand"
        const val COL_MEAL_AMOUNT = "meal_amount"
        const val COL_WATER_AMOUNT = "water_amount"
        const val COL_WATER_CHANGE_TIME = "water_change_time"
        const val COL_REMARKS = "remarks"
        const val COL_MEDICATION_USED = "medication_used"
        const val COL_MEDICATION_DOSE = "medication_dose"
        const val COL_MEDICATION_TIME = "medication_time"
        const val COL_MEDICATION_METHOD = "medication_method"
        const val COL_WALK_TIME1 = "walk_time1"
        const val COL_WALK_TIME2 = "walk_time2"
        const val COL_PEE_STATUS = "pee_status"
        const val COL_POOP_STATUS = "poop_status"
        const val COL_SPIRIT_STATUS = "spirit_status"
        const val COL_APPEARANCE_STATUS = "appearance_status"
        //预约表
        const val TABLE_APPOINTMENTS = "appointments"
        const val COL_APPOINTMENT_ID = "id"
        const val COL_APPOINTMENT_PET_ID = "pet_id"
        const val COL_APPOINTMENT_START_TIME = "start_time"
        const val COL_APPOINTMENT_END_TIME = "end_time"
        const val COL_APPOINTMENT_NOTES = "notes"
        const val COL_APPOINTMENT_STATUS = "status"  // 'pending', 'cancelled'
        const val COL_APPOINTMENT_CREATE_TIME = "create_time"
        const val COL_TEMP_PET_NAME = "temp_pet_name"
        const val COL_TEMP_PET_SPECIES = "temp_pet_species"
        const val COL_TEMP_PET_AGE = "temp_pet_age"
        const val COL_TEMP_PET_GENDER = "temp_pet_gender"
        const val COL_APPOINTMENT_OWNER_ID = "owner_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
        insertSampleData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {


            db.execSQL("DROP TABLE IF EXISTS $TABLE_APPOINTMENTS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_DAILY_CARE")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_BOARDING_RECORDS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PETS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_OWNERS")
            onCreate(db)

    }

    private fun createTables(db: SQLiteDatabase) {
        // 主人表
        db.execSQL(
            """
            CREATE TABLE $TABLE_OWNERS (
                $COL_OWNER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_OWNER_NAME TEXT NOT NULL,
                $COL_OWNER_PHONE TEXT,
                $COL_OWNER_BACKUP_PHONE TEXT
            )
        """.trimIndent()
        )

        // 宠物表（静态信息）
        db.execSQL(
            """
            CREATE TABLE $TABLE_PETS (
                $COL_PET_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PET_NAME TEXT NOT NULL,
                $COL_PET_SPECIES TEXT,
                $COL_PET_AGE INTEGER,
                $COL_PET_GENDER TEXT,
                $COL_PET_OWNER_ID INTEGER,
                FOREIGN KEY($COL_PET_OWNER_ID) REFERENCES $TABLE_OWNERS($COL_OWNER_ID)
            )
        """.trimIndent()
        )

        // 寄养记录表
        db.execSQL(
            """
            CREATE TABLE $TABLE_BOARDING_RECORDS (
                $COL_BOARDING_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BOARDING_PET_ID INTEGER NOT NULL,
                $COL_BOARDING_START_TIME INTEGER,
                $COL_BOARDING_END_TIME INTEGER,
                $COL_BOARDING_IS_ACTIVE INTEGER DEFAULT 1,
                $COL_BOARDING_PROCESS TEXT,
                FOREIGN KEY($COL_BOARDING_PET_ID) REFERENCES $TABLE_PETS($COL_PET_ID)
            )
        """.trimIndent()
        )

        // 日常看护表
        db.execSQL(
            """
            CREATE TABLE $TABLE_DAILY_CARE (
                $COL_CARE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CARE_BOARDING_ID INTEGER NOT NULL,
                $COL_CARE_DATE INTEGER NOT NULL,
                $COL_FOOD_BRAND TEXT,
                $COL_MEAL_AMOUNT TEXT,
                $COL_WATER_AMOUNT TEXT,
                $COL_WATER_CHANGE_TIME TEXT,
                $COL_REMARKS TEXT,
                $COL_MEDICATION_USED TEXT,
                $COL_MEDICATION_DOSE TEXT,
                $COL_MEDICATION_TIME TEXT,
                $COL_MEDICATION_METHOD TEXT,
                $COL_WALK_TIME1 TEXT,
                $COL_WALK_TIME2 TEXT,
                $COL_PEE_STATUS TEXT,
                $COL_POOP_STATUS TEXT,
                $COL_SPIRIT_STATUS TEXT,
                $COL_APPEARANCE_STATUS TEXT,
                FOREIGN KEY($COL_CARE_BOARDING_ID) REFERENCES $TABLE_BOARDING_RECORDS($COL_BOARDING_ID),
                UNIQUE($COL_CARE_BOARDING_ID, $COL_CARE_DATE)
            )
        """.trimIndent()
        )

// 预约表
        db.execSQL("""
    CREATE TABLE $TABLE_APPOINTMENTS (
        $COL_APPOINTMENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $COL_APPOINTMENT_OWNER_ID INTEGER NOT NULL,
        $COL_APPOINTMENT_PET_ID INTEGER,
        $COL_TEMP_PET_NAME TEXT,
        $COL_TEMP_PET_SPECIES TEXT,
        $COL_TEMP_PET_AGE INTEGER,
        $COL_TEMP_PET_GENDER TEXT,
        $COL_APPOINTMENT_START_TIME INTEGER NOT NULL,
        $COL_APPOINTMENT_END_TIME INTEGER,
        $COL_APPOINTMENT_NOTES TEXT,
        $COL_APPOINTMENT_STATUS TEXT DEFAULT 'pending',
        $COL_APPOINTMENT_CREATE_TIME INTEGER NOT NULL,
        FOREIGN KEY($COL_APPOINTMENT_PET_ID) REFERENCES $TABLE_PETS($COL_PET_ID),
        FOREIGN KEY($COL_APPOINTMENT_OWNER_ID) REFERENCES $TABLE_OWNERS($COL_OWNER_ID)
    )
""".trimIndent()
        )
    }

    fun getDailyCareRecordsByBoardingId(boardingId: Long): List<DailyCareRecord> {
        val records = mutableListOf<DailyCareRecord>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_DAILY_CARE WHERE $COL_CARE_BOARDING_ID = ? ORDER BY $COL_CARE_DATE ASC",
            arrayOf(boardingId.toString())
        )
        while (cursor.moveToNext()) {
            val record = DailyCareRecord(
                date = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CARE_DATE)),
                foodBrand = cursor.getString(cursor.getColumnIndexOrThrow(COL_FOOD_BRAND)),
                mealAmount = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEAL_AMOUNT)),
                waterAmount = cursor.getString(cursor.getColumnIndexOrThrow(COL_WATER_AMOUNT)),
                waterChangeTime = cursor.getString(cursor.getColumnIndexOrThrow(COL_WATER_CHANGE_TIME)),
                remarks = cursor.getString(cursor.getColumnIndexOrThrow(COL_REMARKS)),
                medicationUsed = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEDICATION_USED)),
                medicationDose = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEDICATION_DOSE)),
                medicationTime = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEDICATION_TIME)),
                medicationMethod = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEDICATION_METHOD)),
                walkTime1 = cursor.getString(cursor.getColumnIndexOrThrow(COL_WALK_TIME1)),
                walkTime2 = cursor.getString(cursor.getColumnIndexOrThrow(COL_WALK_TIME2)),
                peeStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_PEE_STATUS)),
                poopStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_POOP_STATUS)),
                spiritStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_SPIRIT_STATUS)),
                appearanceStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_APPEARANCE_STATUS))
            )
            records.add(record)
        }
        cursor.close()
        db.close()
        return records
    }
    fun addDailyCare(boardingId: Long, date: Long, remarks: String) {
        val values = ContentValues().apply {
            put(COL_CARE_BOARDING_ID, boardingId)
            put(COL_CARE_DATE, date)
            put(COL_REMARKS, remarks)
            put(COL_FOOD_BRAND, "")
            put(COL_MEAL_AMOUNT, "")
            put(COL_WATER_AMOUNT, "")
            put(COL_WATER_CHANGE_TIME, "")
            put(COL_MEDICATION_USED, "")
            put(COL_MEDICATION_DOSE, "")
            put(COL_MEDICATION_TIME, "")
            put(COL_MEDICATION_METHOD, "")
            put(COL_WALK_TIME1, "08:00")
            put(COL_WALK_TIME2, "18:00")
            put(COL_PEE_STATUS, "")
            put(COL_POOP_STATUS, "")
            put(COL_SPIRIT_STATUS, "")
            put(COL_APPEARANCE_STATUS, "")
        }
        writableDatabase.insert(TABLE_DAILY_CARE, null, values)
    }
    private fun insertSampleData(db: SQLiteDatabase) {
        // 插入主人
        val owner1Id = insertOwner(db, "张三", "13800001111", "13800002222")
        val owner2Id = insertOwner(db, "李四", "13900001111", "")

        // 插入宠物
        val pet1Id = insertPet(db, "旺财", "边境牧羊犬", 3, "公", owner1Id)
        val pet2Id = insertPet(db, "咪咪", "萨摩耶犬", 2, "母", owner2Id)

        val now = System.currentTimeMillis()

        // 为宠物1添加两条寄养记录：一条已完成（历史），一条正在寄养
        val boarding1Id = insertBoardingRecord(db, pet1Id,
            now - 30 * 24 * 60 * 60 * 1000L,
            now - 10 * 24 * 60 * 60 * 1000L,
            0, "第一次寄养")
        val boarding2Id = insertBoardingRecord(db, pet1Id,
            now - 2 * 24 * 60 * 60 * 1000L,
            now + 5 * 24 * 60 * 60 * 1000L,
            1, "当前寄养")

        // 为宠物2添加一条寄养记录（已完成）
        insertBoardingRecord(db, pet2Id,
            now - 20 * 24 * 60 * 60 * 1000L,
            now - 5 * 24 * 60 * 60 * 1000L,
            0, "寄养")

        // 为当前寄养记录添加每日看护示例
        val today = getStartOfDay(now)
        val yesterday = today - 24 * 60 * 60 * 1000L
        insertDailyCare(db, boarding2Id, today, "皇家", "150g", "500ml", "09:00", "正常",
            "", "", "", "", "08:00", "18:00", "正常", "成型", "活泼", "良好")
        insertDailyCare(db, boarding2Id, yesterday, "皇家", "150g", "500ml", "09:00", "正常",
            "", "", "", "", "08:00", "18:00", "正常", "成型", "活泼", "良好")
    }

    private fun insertOwner(db: SQLiteDatabase, name: String, phone: String, backupPhone: String): Long {
        val values = ContentValues().apply {
            put(COL_OWNER_NAME, name)
            put(COL_OWNER_PHONE, phone)
            put(COL_OWNER_BACKUP_PHONE, backupPhone)
        }
        return db.insert(TABLE_OWNERS, null, values)
    }

    private fun insertPet(db: SQLiteDatabase, name: String, species: String, age: Int, gender: String, ownerId: Long): Long {
        val values = ContentValues().apply {
            put(COL_PET_NAME, name)
            put(COL_PET_SPECIES, species)
            put(COL_PET_AGE, age)
            put(COL_PET_GENDER, gender)
            put(COL_PET_OWNER_ID, ownerId)
        }
        return db.insert(TABLE_PETS, null, values)
    }
    fun insertAppointment(
        ownerId: Long,
        petId: Long?,
        tempName: String?,
        tempSpecies: String?,
        tempAge: Int?,
        tempGender: String?,
        startTime: Long,
        endTime: Long?,
        notes: String
    ): Long {
        val values = ContentValues().apply {
            put(COL_APPOINTMENT_OWNER_ID, ownerId)
            if (petId != null) put(COL_APPOINTMENT_PET_ID, petId)
            if (tempName != null) put(COL_TEMP_PET_NAME, tempName)
            if (tempSpecies != null) put(COL_TEMP_PET_SPECIES, tempSpecies)
            if (tempAge != null) put(COL_TEMP_PET_AGE, tempAge)
            if (tempGender != null) put(COL_TEMP_PET_GENDER, tempGender)
            put(COL_APPOINTMENT_START_TIME, startTime)
            if (endTime != null) put(COL_APPOINTMENT_END_TIME, endTime)
            put(COL_APPOINTMENT_NOTES, notes)
            put(COL_APPOINTMENT_STATUS, "pending")
            put(COL_APPOINTMENT_CREATE_TIME, System.currentTimeMillis())
        }
        return writableDatabase.insert(TABLE_APPOINTMENTS, null, values)
    }
    fun deleteAppointment(appointmentId: Long): Boolean {
        return writableDatabase.delete(TABLE_APPOINTMENTS, "$COL_APPOINTMENT_ID = ?", arrayOf(appointmentId.toString())) > 0
    }
    fun cancelAppointment(appointmentId: Long): Boolean {
        val values = ContentValues().apply {
            put(COL_APPOINTMENT_STATUS, "cancelled")
        }
        return writableDatabase.update(TABLE_APPOINTMENTS, values, "$COL_APPOINTMENT_ID = ?", arrayOf(appointmentId.toString())) > 0
    }
    // 插入新宠物
    fun insertPet(ownerId: Long, name: String, species: String, age: Int, gender: String): Long {
        val values = ContentValues().apply {
            put(COL_PET_NAME, name)
            put(COL_PET_SPECIES, species)
            put(COL_PET_AGE, age)
            put(COL_PET_GENDER, gender)
            put(COL_PET_OWNER_ID, ownerId)
        }
        return writableDatabase.insert(TABLE_PETS, null, values)
    }
    fun updateAppointmentPetId(appointmentId: Long, petId: Long) {
        val values = ContentValues().apply {
            put(COL_APPOINTMENT_PET_ID, petId)
        }
        writableDatabase.update(TABLE_APPOINTMENTS, values, "$COL_APPOINTMENT_ID = ?", arrayOf(appointmentId.toString()))
    }

    // 获取所有待处理预约（用于管理界面）
    fun getPendingAppointments(): List<Appointment> {
        val list = mutableListOf<Appointment>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
        SELECT a.*, 
               CASE WHEN a.pet_id IS NOT NULL THEN p.name ELSE a.temp_pet_name END as pet_name
        FROM $TABLE_APPOINTMENTS a
        LEFT JOIN $TABLE_PETS p ON a.pet_id = p.id
        WHERE a.status = 'pending'
        ORDER BY a.create_time ASC
        """.trimIndent(),
            null
        )
        while (cursor.moveToNext()) {
            list.add(parseAppointment(cursor))
        }
        cursor.close()
        db.close()
        return list
    }



    // 根据ID获取预约详情
    fun getAppointmentById(id: Long): Appointment? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
        SELECT a.*, 
               CASE WHEN a.pet_id IS NOT NULL THEN p.name ELSE a.temp_pet_name END as pet_name
        FROM $TABLE_APPOINTMENTS a
        LEFT JOIN $TABLE_PETS p ON a.$COL_APPOINTMENT_PET_ID = p.$COL_PET_ID
        WHERE a.$COL_APPOINTMENT_ID = ?
        """.trimIndent(),
            arrayOf(id.toString())
        )
        val appointment = if (cursor.moveToFirst()) parseAppointment(cursor) else null
        cursor.close()
        db.close()
        return appointment
    }

    // 更新预约状态
    fun updateAppointmentStatus(appointmentId: Long, status: String) {
        val values = ContentValues().apply {
            put(COL_APPOINTMENT_STATUS, status)
        }
        writableDatabase.update(TABLE_APPOINTMENTS, values, "$COL_APPOINTMENT_ID = ?", arrayOf(appointmentId.toString()))
    }

    // 搜索待处理预约
    fun searchPendingAppointments(keyword: String): List<Appointment> {
        val list = mutableListOf<Appointment>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT a.*, p.name as pet_name, o.name as owner_name FROM $TABLE_APPOINTMENTS a " +
                    "JOIN $TABLE_PETS p ON a.$COL_APPOINTMENT_PET_ID = p.$COL_PET_ID " +
                    "JOIN $TABLE_OWNERS o ON p.$COL_PET_OWNER_ID = o.$COL_OWNER_ID " +
                    "WHERE a.$COL_APPOINTMENT_STATUS = 'pending' AND (o.$COL_OWNER_NAME LIKE ? OR p.$COL_PET_NAME LIKE ?) " +
                    "ORDER BY a.$COL_APPOINTMENT_CREATE_TIME ASC",
            arrayOf("%$keyword%", "%$keyword%")
        )
        while (cursor.moveToNext()) {
            list.add(parseAppointment(cursor))
        }
        cursor.close()
        db.close()
        return list
    }
    private fun parseAppointment(cursor: Cursor): Appointment {
        val petId = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_APPOINTMENT_PET_ID))) null
        else cursor.getLong(cursor.getColumnIndexOrThrow(COL_APPOINTMENT_PET_ID))
        val petName = cursor.getString(cursor.getColumnIndexOrThrow("pet_name"))
        val endTime = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_APPOINTMENT_END_TIME))) null
        else cursor.getLong(cursor.getColumnIndexOrThrow(COL_APPOINTMENT_END_TIME))
        val tempPetName = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_PET_NAME))
        val tempPetSpecies = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_PET_SPECIES))
        val tempPetAge = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_TEMP_PET_AGE))) null
        else cursor.getInt(cursor.getColumnIndexOrThrow(COL_TEMP_PET_AGE))
        val tempPetGender = cursor.getString(cursor.getColumnIndexOrThrow(COL_TEMP_PET_GENDER))

        return Appointment(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_APPOINTMENT_ID)),
            ownerId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_APPOINTMENT_OWNER_ID)),
            petId = petId,
            petName = petName,
            startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_APPOINTMENT_START_TIME)),
            endTime = endTime,
            notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_APPOINTMENT_NOTES)),
            status = cursor.getString(cursor.getColumnIndexOrThrow(COL_APPOINTMENT_STATUS)),
            createTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_APPOINTMENT_CREATE_TIME)),
            tempPetName = tempPetName,
            tempPetSpecies = tempPetSpecies,
            tempPetAge = tempPetAge,
            tempPetGender = tempPetGender
        )
    }
    fun insertOwner(name: String, phone: String, backupPhone: String): Long {
        val values = ContentValues().apply {
            put(COL_OWNER_NAME, name)
            put(COL_OWNER_PHONE, phone)
            put(COL_OWNER_BACKUP_PHONE, backupPhone)
        }
        return writableDatabase.insert(TABLE_OWNERS, null, values)
    }


    // 插入寄养记录（需要确保已有方法）
    fun insertBoardingRecord(petId: Long, startTime: Long, endTime: Long, isActive: Int, process: String): Long {
        val values = ContentValues().apply {
            put(COL_BOARDING_PET_ID, petId)
            put(COL_BOARDING_START_TIME, startTime)
            put(COL_BOARDING_END_TIME, endTime)
            put(COL_BOARDING_IS_ACTIVE, isActive)
            put(COL_BOARDING_PROCESS, process)
        }
        return writableDatabase.insert(TABLE_BOARDING_RECORDS, null, values)
    }
    // 获取宠物信息
    fun getPetById(petId: Long): Pet? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PETS WHERE $COL_PET_ID = ?", arrayOf(petId.toString()))
        val pet = if (cursor.moveToFirst()) {
            Pet(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_PET_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COL_PET_NAME)),
                species = cursor.getString(cursor.getColumnIndexOrThrow(COL_PET_SPECIES)),
                age = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PET_AGE)),
                gender = cursor.getString(cursor.getColumnIndexOrThrow(COL_PET_GENDER)),
                ownerId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_PET_OWNER_ID))
            )
        } else null
        cursor.close()
        db.close()
        return pet
    }

    // 获取主人信息
    fun getOwnerById(ownerId: Long): Owner? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_OWNERS WHERE $COL_OWNER_ID = ?", arrayOf(ownerId.toString()))
        val owner = if (cursor.moveToFirst()) {
            Owner(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_OWNER_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COL_OWNER_NAME)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(COL_OWNER_PHONE)),
                backupPhone = cursor.getString(cursor.getColumnIndexOrThrow(COL_OWNER_BACKUP_PHONE))
            )
        } else null
        cursor.close()
        db.close()
        return owner
    }

    // 更新备用电话
    fun updateBackupPhone(ownerId: Long, backupPhone: String) {
        val values = ContentValues().apply {
            put(COL_OWNER_BACKUP_PHONE, backupPhone)
        }
        writableDatabase.update(TABLE_OWNERS, values, "$COL_OWNER_ID = ?", arrayOf(ownerId.toString()))
    }
    fun updateAppointmentTime(appointmentId: Long, startTime: Long, endTime: Long?) {
        val values = ContentValues().apply {
            put(COL_APPOINTMENT_START_TIME, startTime)
            if (endTime != null) put(COL_APPOINTMENT_END_TIME, endTime)
        }
        writableDatabase.update(TABLE_APPOINTMENTS, values, "$COL_APPOINTMENT_ID = ?", arrayOf(appointmentId.toString()))
    }
    // 根据主人ID获取其所有预约
    fun getAppointmentsByOwner(ownerId: Long): List<Appointment> {
        val list = mutableListOf<Appointment>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
        SELECT a.*, 
               CASE WHEN a.pet_id IS NOT NULL THEN p.name ELSE a.temp_pet_name END as pet_name
        FROM $TABLE_APPOINTMENTS a
        LEFT JOIN $TABLE_PETS p ON a.$COL_APPOINTMENT_PET_ID = p.$COL_PET_ID
        WHERE a.$COL_APPOINTMENT_OWNER_ID = ?
        ORDER BY a.$COL_APPOINTMENT_CREATE_TIME DESC
        """.trimIndent(),
            arrayOf(ownerId.toString())
        )
        while (cursor.moveToNext()) {
            list.add(parseAppointment(cursor))
        }
        cursor.close()
        db.close()
        return list
    }


    // 根据关键词搜索待处理预约（主人名或宠物名）
    fun searchAppointments(keyword: String): List<Appointment> {
        val list = mutableListOf<Appointment>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT a.*, p.name as pet_name, o.name as owner_name FROM $TABLE_APPOINTMENTS a " +
                    "LEFT JOIN $TABLE_PETS p ON a.$COL_APPOINTMENT_PET_ID = p.$COL_PET_ID " +
                    "LEFT JOIN $TABLE_OWNERS o ON p.$COL_PET_OWNER_ID = o.$COL_OWNER_ID " +
                    "WHERE a.$COL_APPOINTMENT_STATUS = 'pending' AND (o.$COL_OWNER_NAME LIKE ? OR p.$COL_PET_NAME LIKE ?) " +
                    "ORDER BY a.$COL_APPOINTMENT_CREATE_TIME DESC",
            arrayOf("%$keyword%", "%$keyword%")
        )
        while (cursor.moveToNext()) {
            list.add(parseAppointment(cursor))
        }
        cursor.close()
        db.close()
        return list
    }
    private fun insertBoardingRecord(db: SQLiteDatabase, petId: Long, startTime: Long, endTime: Long, isActive: Int, process: String): Long {
        val values = ContentValues().apply {
            put(COL_BOARDING_PET_ID, petId)
            put(COL_BOARDING_START_TIME, startTime)
            put(COL_BOARDING_END_TIME, endTime)
            put(COL_BOARDING_IS_ACTIVE, isActive)
            put(COL_BOARDING_PROCESS, process)
        }
        return db.insert(TABLE_BOARDING_RECORDS, null, values)
    }

    private fun insertDailyCare(db: SQLiteDatabase, boardingId: Long, date: Long,
                                foodBrand: String, mealAmount: String, waterAmount: String,
                                waterChangeTime: String, remarks: String,
                                medicationUsed: String, medicationDose: String,
                                medicationTime: String, medicationMethod: String,
                                walkTime1: String, walkTime2: String,
                                peeStatus: String, poopStatus: String,
                                spiritStatus: String, appearanceStatus: String) {
        val values = ContentValues().apply {
            put(COL_CARE_BOARDING_ID, boardingId)
            put(COL_CARE_DATE, date)
            put(COL_FOOD_BRAND, foodBrand)
            put(COL_MEAL_AMOUNT, mealAmount)
            put(COL_WATER_AMOUNT, waterAmount)
            put(COL_WATER_CHANGE_TIME, waterChangeTime)
            put(COL_REMARKS, remarks)
            put(COL_MEDICATION_USED, medicationUsed)
            put(COL_MEDICATION_DOSE, medicationDose)
            put(COL_MEDICATION_TIME, medicationTime)
            put(COL_MEDICATION_METHOD, medicationMethod)
            put(COL_WALK_TIME1, walkTime1)
            put(COL_WALK_TIME2, walkTime2)
            put(COL_PEE_STATUS, peeStatus)
            put(COL_POOP_STATUS, poopStatus)
            put(COL_SPIRIT_STATUS, spiritStatus)
            put(COL_APPEARANCE_STATUS, appearanceStatus)
        }
        db.insertWithOnConflict(TABLE_DAILY_CARE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

}