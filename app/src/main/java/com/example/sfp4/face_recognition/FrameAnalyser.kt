package com.example.sfp4.face_recognition

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat.startActivity
import com.example.sfp4.PrefConfig
import com.example.sfp4.food_menu.FoodMenu
import com.example.sfp4.food_recognition.CameraActivityFood
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

// Analyser class to process frames and produce detections.
class FrameAnalyser(private var context: Context, private var boundingBoxOverlay: BoundingBoxOverlay, private var target: String?) : ImageAnalysis.Analyzer {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode( FaceDetectorOptions.PERFORMANCE_MODE_FAST )
        .build()
    private val detector = FaceDetection.getClient(realTimeOpts)
    private val model = FaceNetModel( context )
    private val nameScoreHashmap = HashMap<String,ArrayList<Float>>()
    private var subject = FloatArray( model.embeddingDim )
    private val prefConfig = PrefConfig(context, target)

    // Used to determine whether the incoming frame should be dropped or processed.
    private var isProcessing = false

    // Store the face embeddings in a ( String , FloatArray ) ArrayList.
    // Where String -> name of the person and FloatArray -> Embedding of the face.
    var faceList = ArrayList<Pair<String,FloatArray>>()
    var studentsWhoGotFood = ArrayList<String>()

    // Use any one of the two metrics, "cosine" or "l2"
    private val metricToBeUsed = "cosine"

    val studentDetectionCount = HashMap<String, Int>()
    val detectionCountThreshold = 2
    val similarityThreshold = 0.6

    @ExperimentalGetImage
    override fun analyze( image: ImageProxy) {
        // Rotated bitmap for the FaceNet model
        val frameBitmap = BitmapUtils.rotateBitmap( BitmapUtils.imageToBitmap( image.image!! ) ,
            image.imageInfo.rotationDegrees.toFloat() )

        // Configure frameHeight and frameWidth for output2overlay transformation matrix.
        if ( !boundingBoxOverlay.areDimsInit ) {
            boundingBoxOverlay.frameHeight = frameBitmap.height
            boundingBoxOverlay.frameWidth = frameBitmap.width
        }

        // If the previous frame is still being processed, then skip this frame
        if ( isProcessing || faceList.size == 0 ) {
            image.close()
            return
        }
        else {
            isProcessing = true
            val inputImage = InputImage.fromMediaImage( image.image!! , image.imageInfo.rotationDegrees )
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    CoroutineScope( Dispatchers.Main ).launch {
                        runModel( faces , frameBitmap )
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("App", e.message!!)
                }
                .addOnCompleteListener {
                    image.close()
                }
        }
    }

    private suspend fun runModel(faces : List<Face>, cameraFrameBitmap : Bitmap){
        val prevPredictions = prefConfig.readPredictions()

        withContext( Dispatchers.Default ) {
            val predictions = ArrayList<Prediction>()
            for (face in faces) {
                try {
                    // Crop the frame using face.boundingBox.
                    // Convert the cropped Bitmap to a ByteBuffer.
                    // Finally, feed the ByteBuffer to the FaceNet model.
                    subject = model.getFaceEmbedding(
                        cameraFrameBitmap ,
                        face.boundingBox )

                    // Perform clustering ( grouping )
                    // Store the clusters in a HashMap. Here, the key would represent the 'name'
                    // of that cluster and ArrayList<Float> would represent the collection of all
                    // L2 norms/ cosine distances.
                    for ( i in 0 until faceList.size ) {
                        // If this cluster ( i.e an ArrayList with a specific key ) does not exist,
                        // initialize a new one.
                        if ( nameScoreHashmap[ faceList[ i ].first ] == null ) {
                            // Compute the L2 norm and then append it to the ArrayList.
                            val p = ArrayList<Float>()
                            if ( metricToBeUsed == "cosine" ) {
                                p.add( cosineSimilarity( subject , faceList[ i ].second ) )
                            }
                            else {
                                p.add( L2Norm( subject , faceList[ i ].second ) )
                            }
                            nameScoreHashmap[ faceList[ i ].first ] = p
                        }
                        // If this cluster exists, append the L2 norm/cosine score to it.
                        else {
                            if ( metricToBeUsed == "cosine" ) {
                                nameScoreHashmap[ faceList[ i ].first ]?.add( cosineSimilarity( subject , faceList[ i ].second ) )
                            }
                            else {
                                nameScoreHashmap[ faceList[ i ].first ]?.add( L2Norm( subject , faceList[ i ].second ) )
                            }
                        }
                    }

                    // Compute the average of all scores norms for each cluster.
                    val avgScores = nameScoreHashmap.values.map{ scores -> scores.toFloatArray().average() }
//                    Logger.log( "Average score for each user : $nameScoreHashmap" )

                    val names = nameScoreHashmap.keys.toTypedArray()
                    nameScoreHashmap.clear()

                    // Calculate the minimum L2 distance from the stored average L2 norms.
                    var bestScoreUserName: String
                    var score = 0.0

                    if ( metricToBeUsed == "cosine" ) {
                        // In case of cosine similarity, choose the highest value.
                            // because we need min angle or max cos(theta)
                        score = avgScores.maxOrNull()!!

                        bestScoreUserName = names[ avgScores.indexOf( avgScores.maxOrNull()!! ) ]
                    }
                    else {
                        // In case of L2 norm, choose the lowest value.
                            score = avgScores.minOrNull()!!
                        bestScoreUserName = names[ avgScores.indexOf( avgScores.minOrNull()!! ) ]
                    }
                    Log.e("bestScore", score.toString() + bestScoreUserName)
//                    Logger.log( "Person identified as $bestScoreUserName" )


                    if(score >= similarityThreshold){
                        var currentCount = studentDetectionCount[bestScoreUserName]
                        if(currentCount == null) currentCount = 1
                        else currentCount++

                        studentDetectionCount[bestScoreUserName] = currentCount

                        if(currentCount >= detectionCountThreshold){
                            predictions.add(
                                Prediction(
                                    face.boundingBox,
                                    bestScoreUserName
                                )
                            )
                        }
                    }

                }
                catch ( e : Exception ) {
                    // If any exception occurs with this box and continue with the next boxes.
                    Log.e( "Model" , "Exception in FrameAnalyser : ${e.message}" )
                    continue
                }
            }
            withContext( Dispatchers.Main ) {
                prefConfig.writePredictions(predictions)

                // Clear the BoundingBoxOverlay and set the new results ( boxes ) to be displayed.
                boundingBoxOverlay.boundingBoxes = predictions
                boundingBoxOverlay.invalidate()

                isProcessing = false
            }
        }

        val curPredictions = prefConfig.readPredictions()

        Log.e("runModel $target", prevPredictions.size.toString()+ " " + curPredictions.size.toString())
        if(target == "faceAndFood" && prevPredictions != curPredictions){
            val presentStudents = PrefConfig(context).readPredictions()

            for(studentName in curPredictions){
                if(!prevPredictions.contains(studentName) && presentStudents.contains(studentName)){
                    Log.e(target, studentName)

                    Toast.makeText(context, "Recognized ${studentName.substring(5)}. Show Served Meal Now", Toast.LENGTH_SHORT).show()

                    Handler(Looper.getMainLooper()).postDelayed({
                        val intentFood =  Intent(context, CameraActivityFood::class.java)
                        intentFood.putExtra("target", target)
                        intentFood.putExtra("studentName", studentName)
                        context.startActivity(intentFood)

                        Log.e(target,"Im back")

                        Log.e("studentsWhoGotFood", studentName)
                        studentsWhoGotFood.add(studentName)

                        PrefConfig(context, "studentsWhoGotFood").writeStudentsWhoGotFood(studentName)
                    }, 0)
                }
            }
            (context as Activity).finish()
        }


        Log.e("frameAnalyser", "finished")
    }


    // Compute the L2 norm of ( x2 - x1 )
    private fun L2Norm( x1 : FloatArray, x2 : FloatArray ) : Float {
        var sum = 0.0f
        val mag1 = sqrt( x1.map{ xi -> xi.pow( 2 ) }.sum() )
        val mag2 = sqrt( x2.map{ xi -> xi.pow( 2 ) }.sum() )
        for( i in x1.indices ) {
            sum += ( (x1[i] / mag1) - (x2[i] / mag2) ).pow( 2 )
        }
        return sqrt( sum )
    }


    // Compute the cosine of the angle between x1 and x2.
    private fun cosineSimilarity( x1 : FloatArray , x2 : FloatArray ) : Float {
        var dotProduct = 0.0f
        var mag1 = 0.0f
        var mag2 = 0.0f
        var sum = 0.0f
        for (i in x1.indices) {
            dotProduct += (x1[i] * x2[i])
            mag1 += x1[i].toDouble().pow(2.0).toFloat()
            mag2 += x2[i].toDouble().pow(2.0).toFloat()
            sum += (x1[i] - x2[i]).pow(2)
        }
        mag1 = sqrt(mag1)
        mag2 = sqrt(mag2)
        return dotProduct / (mag1 * mag2)
    }

}