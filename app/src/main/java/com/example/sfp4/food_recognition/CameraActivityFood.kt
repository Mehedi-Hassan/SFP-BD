package com.example.sfp4.food_recognition

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.sfp4.PrefConfig
import com.example.sfp4.face_recognition.BoundingBoxOverlay
import com.example.sfp4.R
import com.example.sfp4.food_menu.FoodMenu
import com.google.common.util.concurrent.ListenableFuture
import org.tensorflow.lite.task.vision.detector.Detection
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CameraActivityFood : AppCompatActivity(), ObjectDetectorHelper.DetectorListener {

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var previewView : PreviewView
    private lateinit var food_bbox_overlay : OverlayView

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private var cameraSelectorOption = CameraSelector.LENS_FACING_BACK
    private var imageAnalyser: ImageAnalysis? = null

    var target: String? = null

    var allPredicted = false
    var predictedFoods = mutableSetOf<String>()

    var counter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_food)

        setTitle("Detecting Foods")

        val bundle: Bundle? = intent.extras
        if(target == "faceAndFood"){
            val studentName = bundle!!.getString("studentName")!!.substring(5)
            getSupportActionBar()?.setSubtitle("for $studentName")
        }

        target = bundle!!.getString("target")


        predictedFoods.clear()


        previewView = findViewById(R.id.food_preview_view)
        // Necessary to keep the Overlay above the PreviewView so that the boxes are visible.
        food_bbox_overlay = findViewById<OverlayView>(R.id.food_bbox_overlay)
        food_bbox_overlay.setWillNotDraw(false)
        objectDetectorHelper = ObjectDetectorHelper(this, this)


        startCameraPreview()
    }

    override fun onResume() {
        super.onResume()
        startCameraPreview()
    }

    // Attach the camera stream to the PreviewView.
    private fun startCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance( this )
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindPreview() },
            ContextCompat.getMainExecutor(this) )
    }

    private fun bindPreview() {
        Log.e("CameraActivityFood counter", counter.toString())
        counter++

        val preview : Preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(Surface.ROTATION_0)
            .build()

        val cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing( cameraSelectorOption )
            .build()

        imageAnalyser =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(Surface.ROTATION_0)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor()){ image ->
                        if (!::bitmapBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }

                        detectObjects(image)
                    }
                }

        cameraProvider?.unbindAll()

        preview.setSurfaceProvider( previewView.surfaceProvider )
        cameraProvider?.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview , imageAnalyser )
    }

    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    override fun onResults(
        results: MutableList<Detection>?,
        imageHeight: Int,
        imageWidth: Int
    ) {

        if (results != null) {
            for(result in results){
                Log.e("foodOnResults", result.categories[0].label)
                predictedFoods.add(result.categories[0].label)
            }
        }

        food_bbox_overlay.setResults(
            results ?: LinkedList<Detection>(),
            imageHeight,
            imageWidth
        )
        food_bbox_overlay.invalidate()

        if(target=="faceAndFood"){
            allPredicted = true
            val expectedFoodsOnToday = PrefConfig(this, "foodToday").readTodayFood()

            for(food in expectedFoodsOnToday){
                Log.e("expectedFoodsOnToday", food)
                if(!predictedFoods.contains(food))
                    allPredicted = false
            }

            if(allPredicted){
                Handler(Looper.getMainLooper()).postDelayed({
                    Toast.makeText(this, "Recognized ${getMealName(expectedFoodsOnToday)}", Toast.LENGTH_SHORT).show()
                    this.finish()
                }, 1000)
            }
        }

    }

    fun getMealName( expectedFoodsOnToday: ArrayList<String>) : String{
        var mealName = expectedFoodsOnToday[0]
        if(expectedFoodsOnToday.size != 1){
            mealName += " and ${expectedFoodsOnToday[1]}"
        }
        return mealName
    }

    override fun onError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if(allPredicted || target=="onlyFood")
            super.onBackPressed()
        else
            Toast.makeText(this, "Can't exit until expected food is detected", Toast.LENGTH_LONG).show()
    }
}