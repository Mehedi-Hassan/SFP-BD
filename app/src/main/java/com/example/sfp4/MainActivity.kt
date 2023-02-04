package com.example.sfp4

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sfp4.authentication.SignInActivity
import com.example.sfp4.dashboard.DashboardActivity
import com.example.sfp4.face_and_food.FaceAndFoodActivity
import com.example.sfp4.face_recognition.CameraActivityFace
import com.example.sfp4.food_menu.FoodMenu
import com.example.sfp4.food_menu.FoodMenuActivity
import com.example.sfp4.food_menu.Meal
import com.example.sfp4.food_recognition.CameraActivityFood
import com.example.sfp4.student_list.StudentListActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"


    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf( Manifest.permission.CAMERA,
                                                Manifest.permission.READ_EXTERNAL_STORAGE )


    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var fireStore: FirebaseFirestore

    var totalBudget = 0.0
    var totalNumOfStudents = 0
    var numOfPresentStudents = 0
    var numOfStudentsGotFood = 0
    var todaysFood = ""
    var todaysFoodKcal = 0.0
    var todaysFoodCost = 0.0
    var dailyBudgetForEachStudent = 0.0
    var curDate = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide()

        setContentView(R.layout.activity_main)

        // Check for permissions.
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        firebaseAuth = FirebaseAuth.getInstance()
        fireStore = FirebaseFirestore.getInstance()

//        clearSavedPreferences()
        setEssentials()
    }

    private fun clearSavedPreferences(){
//        PrefConfig(this).clearPredictions()
//        PrefConfig(this, "faceAndFood").clearPredictions()
//        PrefConfig(this, "studentsWhoGotFood").clearPredictions()
//        PrefConfig(this, "foodToday").clearPredictions()
    }

    override fun onResume() {
        super.onResume()
        setEssentials()
    }

    private fun setEssentials(){
        setDayAndDate()
        setUser()

        setTotalNumOfStudents()
        setNumOfPresentStudents()
        setNumOfStudentsGotFood()

        setTodayFoodMenu()
    }

    private fun setDayAndDate(){
        val tvCurDate: TextView = findViewById(R.id.tvCurDate)
        val curDate = SimpleDateFormat("EEE, MMM d, ''yy", Locale.getDefault())
            .format(System.currentTimeMillis())

        tvCurDate.text = curDate.toString()
    }

    private fun setUser(){
        val userID = firebaseAuth.currentUser!!.uid
        val documentReference = fireStore.collection("users").document(userID)
        documentReference.get()
            .addOnSuccessListener { document ->
                if(document != null) {
                    val tvUserName: TextView = findViewById(R.id.tvUserName)
                    tvUserName.text = document.getString("name")
                    getBudget()
                }
            }
    }

    private fun getBudget(){
        val budgetRef = fireStore.collection("budget").document("remaining")

        val budgetInTk : TextView = findViewById(R.id.budgetInTk)

        budgetRef.get()
            .addOnSuccessListener { document ->
                if(document != null){
                    totalBudget = document.getDouble("curBudget")!!
                    budgetInTk.text = totalBudget.toString()
//                    setSpecial()
                    setTodayFoodMenu()
                }
            }

        budgetInTk.text = totalBudget.toString()
    }

    private fun addBudget(addingAmount: Int){
        val budgetRef = fireStore.collection("budget").document("remaining")
        budgetRef.update("curBudget", FieldValue.increment(addingAmount.toDouble()))
    }

    private fun setTodayFoodMenu(){
        curDate = SimpleDateFormat("EEEE", Locale.getDefault())
            .format(System.currentTimeMillis()).toString()

        val foodMenu = FoodMenu()
        val availableMeals = foodMenu.getAvailableMeals()

        var todayMeal: Meal

        dailyBudgetForEachStudent = 0.0
        if(numOfPresentStudents != 0 && curDate != "Friday"){
            dailyBudgetForEachStudent = getTodaysBudget()/numOfPresentStudents
        }

        if(curDate != "Thursday"){
            todayMeal = foodMenu.generateMenuForEachDay(dailyBudgetForEachStudent)
            setTodayMeal(todayMeal)
        }
        else{
            val curWeek = getCurWeekAndYear()
            val prefReference = FirebaseFirestore.getInstance().collection("specialFoods").document(curWeek)

            prefReference.get()
                .addOnSuccessListener { document ->
                    if(document != null){
                        val specialMeals = ArrayList<Pair<String, Int>>()

                        Log.d("DocumentSnapshot", "DocumentSnapshot data: ${document.data}")

                        for(meal in availableMeals){
                            val mealText = getMealtext(meal)
                            var prevStudents = listOf<String>()

                            if(document.contains(mealText)){
                                prevStudents = document.get(getMealtext(meal)) as List<String>
                                specialMeals.add(Pair(mealText, prevStudents.size))
                            }
                        }

                        todayMeal = foodMenu.generateSpecialMeal(specialMeals, dailyBudgetForEachStudent)
                        setTodayMeal(todayMeal)
                    }
                }
        }
    }

    private fun setTodayMeal(todayMeal: Meal){
        val tvTodayFoodMenu: TextView = findViewById(R.id.tvTodayFoodMenu)

        PrefConfig(this, "foodToday").clearPredictions()
        PrefConfig(this, "foodToday").writeTodayFood(todayMeal.items)

        todaysFood = getMealtext(todayMeal)
        todaysFoodKcal = todayMeal.totalKcal
        todaysFoodCost = todayMeal.totalCost

        tvTodayFoodMenu.text = "Today's Food: $todaysFood"
    }



    private fun getCurWeekAndYear() : String{
        val wk = SimpleDateFormat("YYYY-'W'ww", Locale.getDefault()).format(System.currentTimeMillis())
        return wk
    }

//    fun getThisWeeksBudget() : Double{
//        var thisWeeksBudget = totalBudget/totalNumOfStudents
//        thisWeeksBudget /= numOfWeekdaysRemaining()
//        thisWeeksBudget *= 6
//        return thisWeeksBudget
//    }

    fun getTodaysBudget() : Double{
        var todaysBudget = totalBudget / numOfWeekdaysRemaining()
        return todaysBudget
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

    private fun setTotalNumOfStudents(){
        val rootDir = "/Pictures/studentsFaces"
        var studentFaceDir = File( Environment.getExternalStorageDirectory().absolutePath + rootDir)
        if(!studentFaceDir.exists() && !studentFaceDir.isDirectory()){
            studentFaceDir.mkdirs()
        }

        val imageSubDirs = studentFaceDir.listFiles()
        totalNumOfStudents = imageSubDirs.size
    }

    private fun setNumOfPresentStudents(){
        numOfPresentStudents = PrefConfig(this).readPredictions().size
    }

    private fun setNumOfStudentsGotFood(){
        numOfStudentsGotFood = PrefConfig(this, "studentsWhoGotFood").readStudentsWhoGotFood().size
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission( baseContext, it ) == PackageManager.PERMISSION_GRANTED
    }

    fun btnCameraForFaceClicked(view: View){
        val intent = Intent(this, CameraActivityFace::class.java)
        intent.putExtra("target", "onlyFace")
        startActivity(intent)
    }

    fun btnCameraForFoodClicked(view: View){
        val intent = Intent(this, CameraActivityFood::class.java)
        intent.putExtra("target", "onlyFood")
        startActivity(intent)
    }

    fun btnStudentListClicked(view: View){
        val intent = Intent(this, StudentListActivity::class.java)
        startActivity(intent)
    }

    fun btnFaceAndFoodClicked(view: View){
        val intent = Intent(this, FaceAndFoodActivity::class.java)
        startActivity(intent)
    }

    fun btnFoodMenuClicked(view: View){
        val intent = Intent(this, FoodMenuActivity::class.java)
        intent.putExtra("budget", getTodaysBudget())
        intent.putExtra("numOfPresentStudents", numOfPresentStudents)
        intent.putExtra("dailyBudgetForEachStudent", dailyBudgetForEachStudent)
        intent.putExtra("curDate", curDate)
        intent.putExtra("todayMeal", todaysFood)
        startActivity(intent)
    }

    fun tvDashboardClicked(view: View) {
        val intent = Intent(this, DashboardActivity::class.java)

        intent.putExtra("totalNumOfStudents", totalNumOfStudents)
        intent.putExtra("numOfPresentStudents", numOfPresentStudents)
        intent.putExtra("todaysFood", todaysFood)
        intent.putExtra("todaysFoodKcal", todaysFoodKcal)
        intent.putExtra("todaysFoodCost", todaysFoodCost)
        intent.putExtra("numOfStudentsGotFood", numOfStudentsGotFood)

        startActivity(intent)
    }



    fun logOutClicked(view: View) {
        firebaseAuth.signOut()

        val tvUserName: TextView = findViewById(R.id.tvUserName)
        tvUserName.text = "User Name"

        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    fun budgetClicked(view: View) {
        val view = View.inflate(this, R.layout.popup_budget, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(view)

        val dialog = builder.create()
        dialog.show()

        val tvRemainingBudget: TextView = dialog.findViewById(R.id.tvRemainingBudget)
        val edtMoneyAmount: EditText = dialog.findViewById(R.id.edtMoneyAmount)

        val btnBudgetCancel = dialog.findViewById<Button>(R.id.btnBudgetCancel)
        val btnBudgetAdd = dialog.findViewById<Button>(R.id.btnBudgetAdd)

        tvRemainingBudget.text = "Remaining Budget: $totalBudget Taka"

        btnBudgetCancel.setOnClickListener{
            dialog.dismiss()
        }

        btnBudgetAdd.setOnClickListener{
            val addingAmount = edtMoneyAmount.text.toString().toInt()
            addBudget(addingAmount)
            getBudget()
            dialog.dismiss()
        }
    }

    private fun numOfWeekdaysRemaining() : Int{
        val calendar = Calendar.getInstance()
        val curMonth = calendar.get(Calendar.MONTH)

        var numOfWeekDays = 0
        while(calendar.get(Calendar.MONTH) == curMonth){
            calendar.add(Calendar.DATE, 1)
            if(calendar.get(Calendar.DAY_OF_WEEK) != 7) numOfWeekDays++
        }

        return numOfWeekDays
    }




}





















