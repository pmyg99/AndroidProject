package com.example.yolotest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import ai.onnxruntime.*
import java.nio.FloatBuffer
import ai.onnxruntime.OnnxTensor

class YOLOv8OnnxDetector(private val context: Context, modelFileName: String = "best.onnx") {
    private var session: OrtSession? = null
    private val environment = OrtEnvironment.getEnvironment()
    private val inputSize = 320
    private val numClasses = 60
    private var confThreshold = 0.3f
    private val iouThreshold = 0.45f

    init {
        loadModel(modelFileName)
    }

    private fun loadModel(modelFileName: String) {
        try {
            val modelBytes = context.assets.open(modelFileName).readBytes()
            val options = OrtSession.SessionOptions().apply {
                // 移除 addNnapi() 或者注释掉
                // addNnapi()   // 这行导致错误，暂时注释
                setInterOpNumThreads(4)  // 设置 CPU 线程数
            }
            session = environment.createSession(modelBytes, options)
            Log.d("YOLO", "Model loaded")
        } catch (e: Exception) {
            throw RuntimeException("Failed to load model: ${e.message}")
        }
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        Log.d("YOLO", "Detection started")
        val inputTensor = preprocess(bitmap)
        Log.d("YOLO", "Preprocessing done")
        val inputName = session?.inputNames?.firstOrNull() ?: run {
            Log.e("YOLO", "No input name found")
            return emptyList()
        }
        Log.d("YOLO", "Input name: $inputName")
        return try {
            val output = session?.run(mapOf(inputName to inputTensor))
            Log.d("YOLO", "Inference done, output=$output")
            if (output == null) {
                Log.e("YOLO", "Output is null")
                return emptyList()
            }
            val outputValue = output.get(0)?.value
            Log.d("YOLO", "Output value type: ${outputValue?.javaClass?.simpleName}")

            // 处理可能的三维数组类型 float[][][]
            val outputArray: FloatArray = when (outputValue) {
                is Array<*> -> {
                    // 假设是三维数组 [1][64][8400]
                    val arr = outputValue as Array<Array<FloatArray>>
                    val batch = arr[0] as Array<FloatArray>
                    val channels = batch.size
                    val predictions = if (channels > 0) batch[0].size else 0
                    Log.d("YOLO", "Detected 3D array shape: [${arr.size}][$channels][$predictions]")
                    val flat = FloatArray(channels * predictions)
                    for (c in 0 until channels) {
                        val row = batch[c]
                        for (p in 0 until predictions) {
                            flat[c * predictions + p] = row[p]
                        }
                    }
                    flat
                }
                is OnnxTensor -> {
                    outputValue.floatBuffer.array()
                }
                else -> {
                    Log.e("YOLO", "Unsupported output type: ${outputValue?.javaClass}")
                    return emptyList()
                }
            }

            Log.d("YOLO", "Output array size: ${outputArray.size}, min: ${outputArray.min()}, max: ${outputArray.max()}")
            val detections = postprocess(outputArray, bitmap.width, bitmap.height)
            Log.d("YOLO", "Postprocess done, found ${detections.size} objects")
            output.close()
            detections
        } catch (e: Exception) {
            Log.e("YOLO", "Detection failed", e)
            emptyList()
        } finally {
            inputTensor.close()
        }
    }

    private fun preprocess(bitmap: Bitmap): OnnxTensor {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val data = FloatArray(1 * 3 * inputSize * inputSize)
        var idx = 0
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = resized.getPixel(x, y)
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                data[idx] = r
                data[idx + inputSize * inputSize] = g
                data[idx + 2 * inputSize * inputSize] = b
                idx++
            }
        }
        val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
        return OnnxTensor.createTensor(environment, FloatBuffer.wrap(data), shape)
    }

    private fun postprocess(output: FloatArray, origW: Int, origH: Int): List<Detection> {
        val stride = numClasses + 4  // 64
        val numPredictions = output.size / stride   // 动态计算预测框数量
        val predictions = Array(numPredictions) { i ->
            FloatArray(stride) { j -> output[j * numPredictions + i] }
        }
        val detections = mutableListOf<Detection>()
        for (pred in predictions) {
            // 解码边界框
            var x1 = pred[0] - pred[2] / 2
            var y1 = pred[1] - pred[3] / 2
            var x2 = pred[0] + pred[2] / 2
            var y2 = pred[1] + pred[3] / 2
            x1 = x1.coerceIn(0f, inputSize.toFloat())
            y1 = y1.coerceIn(0f, inputSize.toFloat())
            x2 = x2.coerceIn(0f, inputSize.toFloat())
            y2 = y2.coerceIn(0f, inputSize.toFloat())
            if (x1 >= x2 || y1 >= y2) continue

            // 类别概率（模型输出已经是 sigmoid 后的值）
            val probs = pred.sliceArray(4 until stride)
            var maxScore = -1f
            var classId = -1
            for (i in probs.indices) {
                if (probs[i] > maxScore) {
                    maxScore = probs[i]
                    classId = i
                }
            }
            if (maxScore < confThreshold) continue

            val scaleX = origW.toFloat() / inputSize
            val scaleY = origH.toFloat() / inputSize
            detections.add(
                Detection(
                    (x1 * scaleX).toInt(),
                    (y1 * scaleY).toInt(),
                    (x2 * scaleX).toInt(),
                    (y2 * scaleY).toInt(),
                    maxScore,
                    classId
                )
            )
        }
        return nms(detections)
    }

    private fun nms(detections: List<Detection>): List<Detection> {
        val result = mutableListOf<Detection>()
        val sorted = detections.sortedByDescending { it.confidence }
        for (det in sorted) {
            if (result.none { iou(det, it) > iouThreshold }) {
                result.add(det)
            }
        }
        return result
    }

    private fun iou(a: Detection, b: Detection): Float {
        val x1 = maxOf(a.x1, b.x1).toFloat()
        val y1 = maxOf(a.y1, b.y1).toFloat()
        val x2 = minOf(a.x2, b.x2).toFloat()
        val y2 = minOf(a.y2, b.y2).toFloat()
        val inter = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
        val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)
        return inter / (areaA + areaB - inter)
    }

    fun setConfThreshold(threshold: Float) {
        confThreshold = threshold
    }

    fun close() {
        session?.close()
        environment.close()
    }

    data class Detection(val x1: Int, val y1: Int, val x2: Int, val y2: Int, val confidence: Float, val classId: Int)
}