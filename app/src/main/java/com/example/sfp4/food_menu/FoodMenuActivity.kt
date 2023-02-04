package com.example.sfp4.food_menu

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sfp4.R
import com.example.sfp4.food_menu.recycler_view.AdapterForAvailableMeals
import kotlin.collections.ArrayList

class FoodMenuActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide()

        setContentView(R.layout.activity_food_menu)

        val foodMenu = FoodMenu()

        val bundle: Bundle? = intent.extras
        val budget = bundle!!.getDouble("budget")
        val numOfPresentStudents = bundle!!.getInt("numOfPresentStudents")
        val dailyBudgetForEachStudent = bundle!!.getDouble("dailyBudgetForEachStudent")
        val curDate = bundle!!.getString("curDate").toString()
        val todayMeal = foodMenu.getMealFromMealText(bundle!!.getString("todayMeal").toString())

//        val tvWeeklyBudgetForEachStudent = findViewById<TextView>(R.id.tvWeeklyBudgetForEachStudent)
//        tvWeeklyBudgetForEachStudent.text = "Weekly Budget For Each Student: ${String.format("%.2f", budget)} Tk"



//        val thisWeekMeals = foodMenu.generateMenu(budget, curSpecial)

        val availableMeals = foodMenu.getAvailableMeals()
        availableMeals.sortBy { it.totalKcal }
        availableMeals.reverse()

        showAvailableMeals(availableMeals)
//        showSelectedMeals(thisWeekMeals)

        showSelectedMeal(budget, numOfPresentStudents, dailyBudgetForEachStudent, todayMeal, curDate)

    }

    private fun showAvailableMeals(availableMeals: ArrayList<Meal>){
        val recyclerView: RecyclerView = findViewById(R.id.rvAvailableMeals)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = AdapterForAvailableMeals(availableMeals)
        recyclerView.adapter = adapter
    }

//    private fun showSelectedMeals(thisWeekMeals: ArrayList<Meal>){
//        val recyclerView: RecyclerView = findViewById(R.id.rvSelectedMeals)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        val adapter = AdapterForSelectedMeals(thisWeekMeals)
//        recyclerView.adapter = adapter
//    }

    private fun showSelectedMeal( budget: Double,
                                  numOfPresentStudents: Int,
                                  dailyBudgetForEachStudent: Double,
                                  todayMeal: Meal,
                                  curDate: String){

        val tvDailyBudget : TextView = findViewById(R.id.tvDailyBudget)
        tvDailyBudget.text = "Daily Budget: ${String.format("%.2f", budget)} Tk"

        val tvNumOfPresentStudents : TextView = findViewById(R.id.tvNumOfPresentStudents)
        tvNumOfPresentStudents.text = "Number of present students: $numOfPresentStudents"

        val tvDailyBudgetForEachStudent : TextView = findViewById(R.id.tvDailyBudgetForEachStudent)
        tvDailyBudgetForEachStudent.text = "Budget For Each Student: ${String.format("%.2f", dailyBudgetForEachStudent)} Tk"

        val tvSelectedDay : TextView = findViewById(R.id.tvSelectedDay)
        tvSelectedDay.text = curDate

        val tvSelectedMeal : TextView = findViewById(R.id.tvSelectedMeal)
        tvSelectedMeal.text = getMealtext(todayMeal)

        val tvSelectedKcal : TextView = findViewById(R.id.tvSelectedKcal)
        tvSelectedKcal.text = "${String.format("%.2f", todayMeal.totalKcal)} kcal"

        val tvSelectedCost : TextView = findViewById(R.id.tvSelectedCost)
        tvSelectedCost.text = "${String.format("%.2f", todayMeal.totalCost)} Tk"
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


}