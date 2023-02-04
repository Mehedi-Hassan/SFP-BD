package com.example.sfp4.student_list.recycler_view

import android.app.AlertDialog
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Binder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import org.apache.commons.io.FileUtils
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.sfp4.R
import com.example.sfp4.databinding.ActivityMainBinding
import com.example.sfp4.student_list.grid_view.ImageGvAdapter
import com.example.sfp4.student_list.grid_view.ImageItem
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SingleStudentActivity : AppCompatActivity(){
    private lateinit var previewView : PreviewView
    private var imageCapture: ImageCapture? = null


    private lateinit var gridView: GridView
    private lateinit var arrayList: ArrayList<ImageItem>
    private lateinit var imageGvAdapter: ImageGvAdapter
    private lateinit var imageFolder : File
    private lateinit var outputDirectory : String

    private lateinit var savedUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_student)

        setTitle("Student Details")
        generateStudentInfo()

        val btnAddAFace: Button = findViewById(R.id.btnAddAFace)
        btnAddAFace.setOnClickListener{
            val view = View.inflate(this, R.layout.popup_camera_add_face, null)
            val builder = AlertDialog.Builder(this)
            builder.setView(view)

            val dialog = builder.create()
            dialog.show()

            previewView = dialog.findViewById(R.id.addFacePreview)
            startCamera()

            val btnCapture = dialog.findViewById<Button>(R.id.btnCapture)

            btnCapture.setOnClickListener{
                takePhoto()
                dialog.dismiss()
            }
        }



    }

    private fun generateStudentInfo(){
        val tvStudentName: TextView = findViewById(R.id.tvStudentName)
        val tvStudentRoll: TextView = findViewById(R.id.tvStudentRoll)

        val bundle: Bundle? = intent.extras
        val name = bundle!!.getString("name")
        val roll = bundle!!.getString("roll")

        tvStudentName.text = "Name: " + name
        tvStudentRoll.text = "Roll: " + roll

        outputDirectory = roll + " - " + name

        //--------------------------
        imageFolder = File(bundle!!.getString("imagesFolder"))

        Log.e("SingleStudentActivity", imageFolder.listFiles().size.toString())

        arrayList = ArrayList()
        for(image in imageFolder.listFiles()){
            arrayList.add(ImageItem(Uri.fromFile(image)))
            Log.e("SingleStudentActivity", image.name)
        }

        val gridView: GridView = findViewById(R.id.gridView)
        imageGvAdapter = ImageGvAdapter(applicationContext, arrayList!!)
        gridView?.adapter = imageGvAdapter
    }

    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider
            .getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { mPreview ->
                    mPreview.setSurfaceProvider(
                        previewView.surfaceProvider
                    )
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch (e: Exception) {
                Log.e(TAG, "Start camera failed: ", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/studentsFaces/" + outputDirectory)
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()


        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    generateStudentInfo()
                }
            }
        )


    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    companion object {
        private const val TAG = "SingleStudentActivity CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    }
}