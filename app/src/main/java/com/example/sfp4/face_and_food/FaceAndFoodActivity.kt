package com.example.sfp4.face_and_food

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sfp4.PrefConfig
import com.example.sfp4.R
import com.example.sfp4.face_and_food.recycler_view.StudentAdapterForFood
import com.example.sfp4.face_and_food.recycler_view.StudentForFood
import com.example.sfp4.face_recognition.CameraActivityFace
import com.example.sfp4.food_menu.Food
import com.example.sfp4.student_list.recycler_view.Student
import java.io.File

class FaceAndFoodActivity : AppCompatActivity() {
    val TAG = "FaceAndFoodActivity"
    val rootDir: String = "/Pictures/studentsFaces"
    lateinit var studentFaceDir : File

    private var studentsWhoGotFood = ArrayList<String>()
    private var studentList = ArrayList<StudentForFood>()
    private var imageLabelPairs = ArrayList<Pair<File,String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_and_food)

        setTitle("Students Who Got Food")

        studentFaceDir = File( Environment.getExternalStorageDirectory().absolutePath + rootDir)
        if(!studentFaceDir.exists() && !studentFaceDir.isDirectory()){
            studentFaceDir.mkdirs()
        }
        scanStorageForImages( studentFaceDir)

        setStudentsWhoGotFood()
    }

    private fun scanStorageForImages( imagesDir : File) {
        imageLabelPairs.clear()
        val imageSubDirs = imagesDir.listFiles()

        if ( imageSubDirs != null ) {
            // List all the images in the "images" dir. Create a Hashmap of <Path,Bitmap> from them.
            for ( imageSubDir in imagesDir.listFiles() ) {
                Log.e( "Image Processing"  , "Reading directory -> ${imageSubDir.name}" )
                imageLabelPairs.add(Pair(imageSubDir , imageSubDir.name))
            }
        }
    }

    override fun onResume() {
        super.onResume()

        setStudentsWhoGotFood()
    }

    private fun setStudentsWhoGotFood(){
        val prefConfig = PrefConfig(this, "studentsWhoGotFood")
        studentsWhoGotFood = prefConfig.readStudentsWhoGotFood()

        Log.e(TAG + "studentsWhoGotFood size: ", studentsWhoGotFood.size.toString())

        generateStudents()
        showData()
    }

    private fun generateStudents(){
        studentList.clear()

        for(sample in imageLabelPairs){
            val rollName = sample.second
            if(studentsWhoGotFood.contains(rollName)){
                studentList.add(StudentForFood(rollName.substring(0, 2), rollName.substring(5),sample.first))
            }
        }

        studentList.sortBy { it.roll }

        for(student in studentList){
            Log.e("generateStudents", student.name)
        }
    }

    private fun showData(){
        val recyclerView: RecyclerView = findViewById(R.id.faceAndFoodRv)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = StudentAdapterForFood(studentList)
        recyclerView.adapter = adapter
    }

    fun btnStartDetectingClicked(view: View){
        val target = "faceAndFood"

        val intentFace = Intent(this, CameraActivityFace::class.java)
        intentFace.putExtra("target", target)
        startActivity(intentFace)
    }
}