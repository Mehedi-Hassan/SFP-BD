package com.example.sfp4.dashboard

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.sfp4.MainActivity
import com.example.sfp4.PrefConfig
import com.example.sfp4.R
import com.example.sfp4.food_menu.FoodMenu
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class DashboardActivity : AppCompatActivity() {

    private lateinit var fireStore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        setTitle("Today's Dashboard")

        val bundle: Bundle? = intent.extras

        val totalNumOfStudents = bundle!!.getInt("totalNumOfStudents")
        val tvDashboardTotalStudentsNumber: TextView = findViewById(R.id.tvDashboardTotalStudentsNumber)
        tvDashboardTotalStudentsNumber.text = totalNumOfStudents.toString()


        val numOfPresentStudents = bundle!!.getInt("numOfPresentStudents")
        val tvDashboardPresentStudentsNumber: TextView = findViewById(R.id.tvDashboardPresentStudentsNumber)
        tvDashboardPresentStudentsNumber.text = numOfPresentStudents.toString()


        val todaysFood = bundle!!.getString("todaysFood")
        val tvDashboardTodayFood : TextView = findViewById(R.id.tvDashboardTodayFood)
        tvDashboardTodayFood.text = todaysFood


        val todaysFoodKcal = bundle!!.getDouble("todaysFoodKcal")
        val tvFoodCalorie: TextView = findViewById(R.id.tvFoodCalorie)
        tvFoodCalorie.text = "$todaysFoodKcal kcal"


        val todaysFoodCost = bundle!!.getDouble("todaysFoodCost")
        val tvFoodCost: TextView = findViewById(R.id.tvFoodCost)
        tvFoodCost.text = "${String.format("%.2f", todaysFoodCost)} Tk"


        val numOfStudentsGotFood = bundle!!.getInt("numOfStudentsGotFood")
        val tvNumberOfStudentsGotFood: TextView = findViewById(R.id.tvNumberOfStudentsGotFood)
        tvNumberOfStudentsGotFood.text = numOfStudentsGotFood.toString()

        val btnSendReport : Button = findViewById(R.id.btnSendReport)
        btnSendReport.setOnClickListener {
            val curDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(System.currentTimeMillis())

            fireStore = FirebaseFirestore.getInstance()
            val documentReference: DocumentReference = fireStore.collection("reports").document(curDate)

            var prevNumOfSTudentsGotFood = listOf<String>()
            var increasedStudents = 0

            documentReference.get()
                .addOnSuccessListener { document ->
                    if(document != null){

                        if(document.contains("Students who got meal")){
                            prevNumOfSTudentsGotFood = document.get("Students who got meal") as List<String>
                        }

                        val studentsWhoGotFood = PrefConfig(this, "studentsWhoGotFood").readStudentsWhoGotFood()
                        studentsWhoGotFood.sortBy { it }

                        for(student in studentsWhoGotFood){
                            if(!prevNumOfSTudentsGotFood.contains(student)){
                                prevNumOfSTudentsGotFood += student
                                increasedStudents++
                            }
                        }

                        val report = hashMapOf(
                            "Total students" to totalNumOfStudents,
                            "Present students" to numOfPresentStudents,
                            "Students who got meal" to prevNumOfSTudentsGotFood,
                            "Served meal" to todaysFood,
                            "Meal kcal" to todaysFoodKcal,
                            "Meal cost per student" to todaysFoodCost
                        )

                        documentReference.set(report).addOnSuccessListener {
                            Log.e("DashboardActivity", "Report sent to authority")
                            addBudget(-todaysFoodCost*increasedStudents)
                        }


                    }
                }

        }
    }

    private fun addBudget(addingAmount: Double){
        val budgetRef = fireStore.collection("budget").document("remaining")
        budgetRef.update("curBudget", FieldValue.increment(addingAmount))
    }
}