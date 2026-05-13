package com.example.yolotest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detector: YOLOv8OnnxDetector
    private var imageAnalysis: ImageAnalysis? = null
    private var lastFrameBitmap: Bitmap? = null
    private val isActive = AtomicBoolean(true)

    private val CAMERA_PERMISSION_REQUEST_CODE = 101
    private val REQUEST_MAIN = 101
    private val TAG = "CameraActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        val btnBack = findViewById<Button>(R.id.btn_back)
        val btnCapture = findViewById<Button>(R.id.btn_capture)

        detector = YOLOv8OnnxDetector(this)
        detector.setConfThreshold(0.25f)

        cameraExecutor = Executors.newSingleThreadExecutor()

        btnBack.setOnClickListener { finish() }
        btnCapture.setOnClickListener { takePhoto() }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "需要相机权限", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FrameAnalyzer())
                }
            try {
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "启动相机失败", e)
                Toast.makeText(this, "启动相机失败: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    inner class FrameAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(imageProxy: ImageProxy) {
            if (!isActive.get()) {
                imageProxy.close()
                return
            }
            val bitmap = imageProxyToBitmap(imageProxy)
            imageProxy.close()
            if (bitmap == null) return

            synchronized(this@CameraActivity) {
                lastFrameBitmap?.recycle()
                lastFrameBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            }

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true)
            val detections = detector.detect(resizedBitmap)
            val srcWidth = bitmap.width
            val srcHeight = bitmap.height
            val scaleX = srcWidth.toFloat() / 640f
            val scaleY = srcHeight.toFloat() / 640f
            val mappedDetections = detections.map {
                it.copy(
                    x1 = (it.x1 * scaleX).toInt(),
                    y1 = (it.y1 * scaleY).toInt(),
                    x2 = (it.x2 * scaleX).toInt(),
                    y2 = (it.y2 * scaleY).toInt()
                )
            }

            runOnUiThread {
                if (isActive.get()) {
                    overlayView.setDetections(mappedDetections, srcWidth, srcHeight)
                }
            }

            resizedBitmap.recycle()
        }

        @OptIn(ExperimentalGetImage::class)
        private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
            val image = imageProxy.image ?: return null
            val planes = image.planes
            if (planes.isEmpty()) return null
            val buffer = planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val width = imageProxy.width
            val height = imageProxy.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixelBuffer = IntArray(width * height)
            for (i in 0 until width * height) {
                val offset = i * 4
                val r = bytes[offset].toInt() and 0xFF
                val g = bytes[offset + 1].toInt() and 0xFF
                val b = bytes[offset + 2].toInt() and 0xFF
                val a = bytes[offset + 3].toInt() and 0xFF
                pixelBuffer[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            bitmap.setPixels(pixelBuffer, 0, width, 0, 0, width, height)
            val rotation = imageProxy.imageInfo.rotationDegrees
            return if (rotation != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
                bitmap.recycle()
                rotated
            } else {
                bitmap
            }
        }
    }

    private fun takePhoto() {
        val frame = synchronized(this) { lastFrameBitmap }
        if (frame == null || frame.isRecycled) {
            Toast.makeText(this, "暂无图像", Toast.LENGTH_SHORT).show()
            return
        }
        val photoFile = createTempImageFile()
        try {
            FileOutputStream(photoFile).use { out ->
                frame.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("image_path", photoFile.absolutePath)
            startActivityForResult(intent, REQUEST_MAIN)
        } catch (e: Exception) {
            Log.e(TAG, "保存图片失败", e)
            Toast.makeText(this, "保存图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(cacheDir, "JPEG_${timeStamp}_${System.currentTimeMillis()}.jpg")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MAIN && resultCode == RESULT_OK) {
            val species = data?.getStringExtra("detected_species")
            if (!species.isNullOrEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra("detected_species", species)
                setResult(RESULT_OK, resultIntent)
            }
            finish()
        }
    }

    override fun onDestroy() {
        isActive.set(false)
        imageAnalysis?.clearAnalyzer()
        cameraExecutor.shutdown()
        try {
            if (!cameraExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                cameraExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            cameraExecutor.shutdownNow()
        }
        detector.close()
        super.onDestroy()
        synchronized(this) {
            lastFrameBitmap?.recycle()
            lastFrameBitmap = null
        }
    }
}


