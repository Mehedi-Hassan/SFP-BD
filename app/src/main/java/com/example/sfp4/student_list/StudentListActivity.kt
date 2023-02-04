package com.example.sfp4.student_list

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sfp4.PrefConfig
import com.example.sfp4.R
import com.example.sfp4.student_list.recycler_view.SingleStudentActivity
import com.example.sfp4.student_list.recycler_view.Student
import com.example.sfp4.student_list.recycler_view.StudentAdapter
import java.io.File

class StudentListActivity : AppCompatActivity(), StudentAdapter.OnItemClickListener {
    val TAG = "StudentListActivity"
    val rootDir = "/Pictures/studentsFaces"
    lateinit var studentFaceDir : File

    private lateinit var recyclerView: RecyclerView

    private var imageLabelPairs = ArrayList<Pair<File,String>>()
    private var studentList = ArrayList<Student>()
    private val prefConfig = PrefConfig(this)
    private var savedPredictions = ArrayList<String>()

    var numOfPresentStudents = 0
    var totalStudents = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_list)

        setTitle("Students' List")

        savedPredictions = prefConfig.readPredictions()

        studentFaceDir = File( Environment.getExternalStorageDirectory().absolutePath + rootDir)
        if(!studentFaceDir.exists() && !studentFaceDir.isDirectory()){
            studentFaceDir.mkdirs()
        }

        scanStorageForImages( studentFaceDir)
        recyclerView = findViewById(R.id.rv)
        showData()
    }

    private fun scanStorageForImages( imagesDir : File ) {
        imageLabelPairs.clear()
        val imageSubDirs = imagesDir.listFiles()
        if ( imageSubDirs != null ) {
            // List all the images in the "images" dir. Create a Hashmap of <Path,Bitmap> from them.
            for ( imageSubDir in imagesDir.listFiles() ) {
                Log.e( "Image Processing"  , "Reading directory -> ${imageSubDir.name}" )

                imageLabelPairs.add( Pair(imageSubDir , imageSubDir.name ))
            }

            studentList.clear()
            totalStudents = 0
            numOfPresentStudents = 0

            for (i in 0 until imageLabelPairs.size){
                scanImage( i )
                Log.d(TAG, studentList[i].name)
                totalStudents++
            }

            studentList.sortBy { it.roll }
        }
    }

    private fun scanImage(counter: Int) {
        val sample = imageLabelPairs[ counter ]

        // 01 - Mehedi
        val rollName = sample.second
        studentList.add(Student(rollName.substring(0, 2), rollName.substring(5), savedPredictions.contains(rollName) ,sample.first))
    }

    private fun showData(){
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = StudentAdapter(studentList, this)
        recyclerView.adapter = adapter
    }

    fun btnAddStudentClicked(view: View){
        val view = View.inflate(this, R.layout.popup_add_student, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(view)

        val dialog = builder.create()
        dialog.show()

        val edtStudentName = dialog.findViewById<EditText>(R.id.edtStudentName)
        val edtStudentRoll = dialog.findViewById<EditText>(R.id.edtStudentRoll)

        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialog.findViewById<Button>(R.id.btnSave)

        btnCancel.setOnClickListener{
            dialog.dismiss()
        }

        btnSave.setOnClickListener{
            var name = edtStudentName.text.toString()
            var roll = edtStudentRoll.text.toString()
            if(roll.length == 1){
                roll = "0" + roll
            }

            Log.e(TAG, "btnsave" + roll + name)

            if(name.isNotEmpty() && roll.isNotEmpty()){
                var newDir = File( Environment.getExternalStorageDirectory().absolutePath + rootDir
                        + '/' + roll + " - " + name)

                if(!newDir.exists() && !newDir.isDirectory()){
                    newDir.mkdirs()

                    Log.e(TAG, "Created directory")
                    scanStorageForImages( studentFaceDir)
                    showData()
                }
            }
            dialog.dismiss()
        }
    }


    override fun onItemClicked(student: Student) {
        val intent = Intent(this, SingleStudentActivity::class.java)
        intent.putExtra("name", student.name)
        intent.putExtra("roll", student.roll)
        intent.putExtra("imagesFolder", student.imagesUri.toString())
        startActivity(intent)
    }
}