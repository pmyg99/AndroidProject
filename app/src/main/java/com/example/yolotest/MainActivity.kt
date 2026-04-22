package com.example.yolotest

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var detector: YOLOv8OnnxDetector
    private lateinit var outputImage: ImageView

    private val classNames = listOf(
        "非洲野犬", "阿彭策尔山犬", "伯恩山犬", "边境牧羊犬", "佛兰德牧牛犬", "布拉班特格里芬犬", "布列塔尼猎犬",
        "卡迪根威尔士柯基", "杜宾犬", "英国塞特犬", "英国激飞猎犬", "恩特布赫山地犬", "爱斯基摩犬", "法国斗牛犬",
        "德国牧羊犬", "戈登塞特犬", "大丹犬", "大白熊犬", "大瑞士山地犬", "爱尔兰雪达犬", "爱尔兰水猎犬",
        "莱昂伯格犬", "墨西哥无毛犬", "纽芬兰犬", "古英国牧羊犬", "彭布罗克威尔士柯基", "博美犬", "罗威纳犬",
        "圣伯纳犬", "萨摩耶犬", "喜乐蒂牧羊犬", "西伯利亚哈士奇", "萨塞克斯猎犬", "藏獒", "威尔士激飞猎犬",
        "猴面梗", "巴仙吉犬", "拳师犬", "布里牧犬", "斗牛獒", "松狮犬", "克伦伯猎犬", "可卡犬", "柯利牧羊犬",
        "亚洲豺犬", "澳洲野犬", "比利时格罗安达牧羊犬", "荷兰毛狮犬", "澳大利亚卡尔比犬", "可蒙犬", "库瓦兹犬",
        "阿拉斯加雪橇犬", "比利时玛利诺犬", "迷你宾莎犬", "迷你贵宾犬", "巴哥犬", "比利时史基伯犬", "标准贵宾犬",
        "玩具贵宾犬", "匈牙利维兹拉犬"
    )

    private var currentImagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        outputImage = findViewById(R.id.outputImage)

        detector = YOLOv8OnnxDetector(this)
        detector.setConfThreshold(0.25f)

        val imagePath = intent.getStringExtra("image_path")
        if (imagePath != null) {
            currentImagePath = imagePath
            val bitmap = loadImageWithCorrectOrientation(imagePath)
            if (bitmap != null) {
                // 自动进行检测
                performDetection(bitmap)
            } else {
                Toast.makeText(this, "无法加载图片", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "未传入图片路径", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun performDetection(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.IO) {
            val detections = detector.detect(bitmap)
            withContext(Dispatchers.Main) {
                // 绘制检测结果
                val outputBitmap = drawDetections(bitmap, detections)
                outputImage.setImageBitmap(outputBitmap)

                // 根据检测到的目标数量决定是否返回品种
                if (detections.size == 1) {
                    val topDetection = detections.first()
                    val detectedSpecies = if (topDetection.classId in classNames.indices) {
                        classNames[topDetection.classId]
                    } else {
                        "未知品种"
                    }
                    // 自动返回品种并关闭界面
                    val resultIntent = Intent()
                    resultIntent.putExtra("detected_species", detectedSpecies)
                    setResult(RESULT_OK, resultIntent)
                    // 删除临时文件
                    currentImagePath?.let { path ->
                        File(path).takeIf { it.exists() }?.delete()
                    }
                    finish()
                } else {
                    // 没有目标或多个目标，提示后延迟关闭
                    val msg = if (detections.isEmpty()) "未检测到任何宠物" else "检测到多个目标，请手动选择品种"
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    // 3秒后自动关闭，或者可以保留界面让用户手动返回
                    outputImage.postDelayed({
                        currentImagePath?.let { path ->
                            File(path).takeIf { it.exists() }?.delete()
                        }
                        finish()
                    }, 2000)
                }
            }
        }
    }

    private fun drawDetections(bitmap: Bitmap, detections: List<YOLOv8OnnxDetector.Detection>): Bitmap {
        val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val paint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val textPaint = Paint().apply {
            color = Color.GREEN
            textSize = 40f
        }
        for (det in detections) {
            val left = det.x1.toFloat()
            val top = det.y1.toFloat()
            val right = det.x2.toFloat()
            val bottom = det.y2.toFloat()
            canvas.drawRect(left, top, right, bottom, paint)
            val className = if (det.classId in classNames.indices) classNames[det.classId] else "未知"
            val label = "$className ${String.format("%.2f", det.confidence)}"
            canvas.drawText(label, left, top - 10f, textPaint)
        }
        return mutable
    }

    private fun loadImageWithCorrectOrientation(filePath: String): Bitmap? {
        var bitmap = BitmapFactory.decodeFile(filePath) ?: return null
        try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            return rotated
        } catch (e: Exception) {
            return bitmap
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 确保临时文件被删除
        currentImagePath?.let { path ->
            File(path).takeIf { it.exists() }?.delete()
        }
        detector.close()
    }
}