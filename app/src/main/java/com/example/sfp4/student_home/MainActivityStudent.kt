package com.example.sfp4.student_home

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import com.example.sfp4.R
import com.example.sfp4.authentication.SignInActivity
import com.example.sfp4.food_menu.Food
import com.example.sfp4.food_menu.FoodMenu
import com.example.sfp4.food_menu.Meal
import com.example.sfp4.food_menu.recycler_view.AdapterForAvailableMeals
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivityStudent : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var fireStore: FirebaseFirestore

    lateinit var spinner: Spinner
    var selectedSpecialMeals = ArrayList<Meal>()
    var availableMeals = ArrayList<Meal>()
    var userID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide()

        setContentView(R.layout.activity_main_student)

        firebaseAuth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()

        setEssentials()

        availableMeals = FoodMenu().getAvailableMeals()
        availableMeals.add(0, Meal(arrayListOf(Food("Nothing", 0.0, 0.0)),
                                0.0,
                                0.0))

        initializeUI()
    }

    override fun onResume() {
        super.onResume()
        setEssentials()
    }

    private fun setEssentials(){
        setUser()
        setDayAndDate()
    }

    private fun setUser(){
        userID = firebaseAuth.currentUser!!.uid
        val documentReference = fireStore.collection("users").document(userID)
        documentReference.get()
            .addOnSuccessListener { document ->
                if(document != null) {
                    val tvUserName: TextView = findViewById(R.id.tvUserName)
                    tvUserName.text = document.getString("name")
                }
            }
    }

    private fun setDayAndDate(){
        val tvCurDate: TextView = findViewById(R.id.tvCurDate)
        val curDate = SimpleDateFormat("EEE, MMM d, ''yy", Locale.getDefault())
            .format(System.currentTimeMillis())

        tvCurDate.text = curDate.toString()
    }

    fun initializeUI(){
        spinner = findViewById(R.id.spSelectSpecialMeal)

        val adapter = SpecialMealAdapter(this, availableMeals)

        spinner.setAdapter(adapter)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if(position!=0 && !selectedSpecialMeals.contains(availableMeals[position])){
                    selectedSpecialMeals.add(availableMeals[position])
                }
                Toast.makeText(applicationContext, "Selected ${getMealtext(availableMeals[position])}", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Toast.makeText(applicationContext, "Selected Nothing", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun getMealtext(curMeal: Meal) : String{
        var mealText = ""
        var addedOneFood = false
        for(food in curMeal.items){
            if(addedOneFood) mealText += " and "
            mealText += food!!.name
            addedOneFood = true
        }
        return mealText
    }

    fun studentLogOutClicked(view: View) {
        firebaseAuth.signOut()

        val tvUserName: TextView = findViewById(R.id.tvUserName)
        tvUserName.text = "User Name"

        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    fun getCurWeekAndYear() : String{
        val wk = SimpleDateFormat("YYYY-'W'ww", Locale.getDefault()).format(System.currentTimeMillis())
        return wk
    }

    fun btnSendPreferenceClicked(view: View) {
        val curWeek = getCurWeekAndYear()
        val prefReference = fireStore.collection("specialFoods").document(curWeek)

        val foodMenu = FoodMenu()
        val availableMeals = foodMenu.getAvailableMeals()

        prefReference.get()
            .addOnSuccessListener { document ->
                if(document != null){
                    Log.d("DocumentSnapshot", "DocumentSnapshot data: ${document.data}")
                    val selections = HashMap<String, List<String>>()

                    for(meal in availableMeals){
                        val mealText = getMealtext(meal)
                        var prevStudents = listOf<String>()


                        if(document.contains(mealText)){
                            prevStudents = document.get(getMealtext(meal)) as List<String>
                            if(selectedSpecialMeals.contains(meal) && !prevStudents.contains(userID)){
                                prevStudents += userID
                            }
                        }
                        else{
                            if(selectedSpecialMeals.contains(meal)){
                                prevStudents += userID
                            }
                        }
//                        prefReference.update(mealText, prevStudents)
                        if(prevStudents.isNotEmpty()){
                            selections.put(mealText, prevStudents)
                        }
                    }

                    prefReference.set(selections)
                        .addOnSuccessListener {
                            Toast.makeText(applicationContext, "Sent preferences to authority", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    fun btnSendFeedbackClicked(view: View) {
        val curDate = SimpleDateFormat("EEE, MMM d, ''yy", Locale.getDefault())
            .format(System.currentTimeMillis())
        val prefReference = fireStore.collection("feedbacks").document(curDate)

        prefReference.get()
            .addOnSuccessListener { document ->
                if(document != null){

                    Log.d("DocumentSnapshot", "DocumentSnapshot data: ${document.data}")

                    var prevFeedbacks = listOf<String>()
                    if(document.contains(userID)){
                        prevFeedbacks = document.get(userID) as List<String>
                    }

                    val edtFeedBackText = findViewById<EditText>(R.id.edtFeedBackText).text.toString().trim()

                    if(edtFeedBackText.isNotEmpty()){
                        prevFeedbacks += edtFeedBackText
                    }

                    val keys = document.data?.keys
                    var done = false

                    if (keys != null) {
                        for(key in keys){
                            if(key.toString() == userID){
                                done = true
                                prefReference.update(userID, prevFeedbacks)
                            }
                        }
                    }
                    if(!done){
                        prefReference.update(userID, prevFeedbacks)
                    }

//                    val feedback = hashMapOf( userID to prevFeedbacks)
//                    prefReference.set(feedback)

                    Toast.makeText(applicationContext, "Sent feedback to authority", Toast.LENGTH_SHORT).show()
                }
            }
    }
}