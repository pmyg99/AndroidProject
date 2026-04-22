package com.example.yolotest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val textPaint = Paint().apply {
        color = Color.GREEN
        textSize = 50f
    }
    private var detections: List<YOLOv8OnnxDetector.Detection> = emptyList()
    private var srcWidth = 640
    private var srcHeight = 640

    // 你的60个类别名称列表
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

    fun setDetections(detections: List<YOLOv8OnnxDetector.Detection>, srcWidth: Int, srcHeight: Int) {
        this.detections = detections
        this.srcWidth = srcWidth
        this.srcHeight = srcHeight
        postInvalidate() // 刷新视图
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (detections.isEmpty()) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        // 计算缩放比例，使原始图像完全适应视图
        val scaleX = viewWidth / srcWidth
        val scaleY = viewHeight / srcHeight

        for (det in detections) {
            val left = det.x1 * scaleX
            val top = det.y1 * scaleY
            val right = det.x2 * scaleX
            val bottom = det.y2 * scaleY

            canvas.drawRect(left, top, right, bottom, paint)
            val className = classNames.getOrElse(det.classId) { "未知" }
            val label = "$className ${String.format("%.1f", det.confidence * 100)}%"
            canvas.drawText(label, left, top - 10f, textPaint)
        }
    }
}