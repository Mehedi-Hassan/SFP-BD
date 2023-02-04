package com.example.sfp4.face_recognition

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.DocumentsContract
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleOwner
import com.example.sfp4.R
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.launch
import java.io.*
import java.util.concurrent.Executors

class CameraActivityFace : AppCompatActivity() {
    private var target : String? = null

    private lateinit var previewView : PreviewView
    private lateinit var frameAnalyser  : FrameAnalyser
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private var cameraSelectorOption = CameraSelector.LENS_FACING_BACK

    private val studentImages = ArrayList<Pair<String, Bitmap>>()
    private val faceEmbeddings = ArrayList<Pair<String,FloatArray>>()
    private val rootDir: String = "/Pictures/studentsFaces"
    private lateinit var studentFaceDir : File

    private lateinit var realTimeOpts : FaceDetectorOptions
    private lateinit var detector : FaceDetector
    private lateinit var faceNetModel : FaceNetModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_face)

        setTitle("Recognizing Faces")

        val bundle: Bundle? = intent.extras
        target = bundle!!.getString("target")

        previewView = findViewById(R.id.face_preview_view)
        // Necessary to keep the Overlay above the PreviewView so that the boxes are visible.
        val boundingBoxOverlay = findViewById<BoundingBoxOverlay>( R.id.bbox_overlay )
        boundingBoxOverlay.setWillNotDraw( false )
        boundingBoxOverlay.setZOrderOnTop( true )

        frameAnalyser = FrameAnalyser( this , boundingBoxOverlay, target)
        faceNetModel = FaceNetModel(this)

        realTimeOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode( FaceDetectorOptions.PERFORMANCE_MODE_FAST )
            .build()
        detector = FaceDetection.getClient( realTimeOpts )

        // Directory for reading faces
        studentFaceDir = File( Environment.getExternalStorageDirectory().absolutePath + rootDir)
        if(!studentFaceDir.exists() && !studentFaceDir.isDirectory()){
            studentFaceDir.mkdirs()
        }

        scanStorageForImages( studentFaceDir)
        frameAnalyser.faceList = faceEmbeddings

        startCameraPreview()
    }

//    override fun onResume() {
//        super.onResume()
//        startCameraPreview()
//    }

    // Attach the camera stream to the PreviewView.
    private fun startCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance( this )
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider) },
            ContextCompat.getMainExecutor(this) )
    }

    private fun bindPreview(cameraProvider : ProcessCameraProvider) {
        val preview : Preview = Preview.Builder().build()
        val cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing( cameraSelectorOption )
            .build()
        preview.setSurfaceProvider( previewView.surfaceProvider )
        val imageFrameAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//            .setTargetResolution(Size( 480, 640 ) )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageFrameAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), frameAnalyser )
        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview , imageFrameAnalysis  )
    }

    //------------------------------------

    // read faces from storage
    private fun scanStorageForImages( imagesDir : File ) {
        val imageSubDirs = imagesDir.listFiles()
        if ( imageSubDirs != null ) {
            // List all the images in the "images" dir. Create a Hashmap of <Path,Bitmap> from them.
            for ( imageSubDir in imagesDir.listFiles() ) {
                Log.e( "Image Processing"  , "Reading directory -> ${imageSubDir.name}" )

                for ( image in imageSubDir.listFiles() ) {
                    Log.e( "Image Processing"  , "Reading file --> ${image.name}" )
//                    studentImages.add(Pair(imageSubDir.name, getFixedBitmap(Uri.fromFile(image))))
                    scanImage(imageSubDir.name, getFixedBitmap(Uri.fromFile(image)))
                }
            }

//            for (image in studentImages){
//                scanImage( image.first, image.second )
//                Log.d("scanStorageForImages", image.first)
//            }
        }
    }

    // Crop faces and produce embeddings ( using FaceNet ) from given image.
    // Store the embedding in faceEmbeddings
    private fun scanImage( name : String , image : Bitmap) {
        val inputImage = InputImage.fromByteArray(
            BitmapUtils.bitmapToNV21ByteArray( image ) ,
            image.width,
            image.height,
            0,
            InputImage.IMAGE_FORMAT_NV21
        )
        detector.process( inputImage )
            .addOnSuccessListener { faces ->
                if ( faces.size != 0 ) {
                    var embedding = faceNetModel.getFaceEmbedding(image, faces[0].boundingBox)
                    faceEmbeddings.add( Pair( name , embedding ) )
                }
            }
            .addOnFailureListener { e ->
                Log.e( "scanImage" , "detector failed" )
            }
    }

    // Get the image as a Bitmap from given Uri and fix the rotation using the Exif interface
    // Source -> https://stackoverflow.com/questions/14066038/why-does-an-image-captured-using-camera-intent-gets-rotated-on-some-devices-on-a
    private fun getFixedBitmap( imageFileUri : Uri) : Bitmap {
        var imageBitmap = BitmapUtils.getBitmapFromUri( contentResolver , imageFileUri )

        val exifInterface = ExifInterface( contentResolver.openInputStream( imageFileUri )!! )
        imageBitmap =
            when (exifInterface.getAttributeInt( ExifInterface.TAG_ORIENTATION ,
                ExifInterface.ORIENTATION_UNDEFINED )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> BitmapUtils.rotateBitmap( imageBitmap , 90f )
                ExifInterface.ORIENTATION_ROTATE_180 -> BitmapUtils.rotateBitmap( imageBitmap , 180f )
                ExifInterface.ORIENTATION_ROTATE_270 -> BitmapUtils.rotateBitmap( imageBitmap , 270f )
                else -> imageBitmap
            }

        return imageBitmap
//        return BitmapUtils.getResizedBitmap(imageBitmap, 500000)
    }

}