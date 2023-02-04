package com.example.sfp4.food_menu

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import com.example.sfp4.MainActivity
import com.example.sfp4.R
import com.example.sfp4.student_home.MainActivityStudent
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FoodMenu {

    val baseFoods = HashMap<String, Food>()
    val baseMeals = ArrayList<ArrayList<Food?>>()

    fun main(args: Array<String>) {}

    init {
        addBaseFoods()
        addBaseMeals()
    }

    private fun addBaseFoods(){
        // https://www.nutritionix.com/food/banana
        baseFoods.put("Banana", Food("Banana", 105.0, 7.0))

        // http://sfp.dpe.gov.bd/sites/default/files/files/sfp.dpe.gov.bd/page/849711e1_de2c_474d_89b3_937c13541c4e/2020-04-23-09-26-ab3681fce4ee529d0dc907d3f71869d0.pdf
        baseFoods.put("Biscuit", Food("Biscuit", 337.0, 10.0))

        // https://www.nutritionix.com/food/bread
        baseFoods.put("Bread", Food("Bread", 231.0, 7.0))

        // http://sfp.dpe.gov.bd/sites/default/files/files/sfp.dpe.gov.bd/page/849711e1_de2c_474d_89b3_937c13541c4e/2020-04-23-09-26-ab3681fce4ee529d0dc907d3f71869d0.pdf
        baseFoods.put("Egg", Food("Egg", 92.0, 9.0))

        // http://sfp.dpe.gov.bd/sites/default/files/files/sfp.dpe.gov.bd/page/849711e1_de2c_474d_89b3_937c13541c4e/2020-04-23-09-26-ab3681fce4ee529d0dc907d3f71869d0.pdf
        baseFoods.put("Khichuri", Food("Khichuri", 577.0, 34.0))

        // https://www.nutritionix.com/food/milk
        baseFoods.put("Milk", Food("Milk", 122.0, 20.0))

        // https://www.nutritionix.com/i/nutritionix/vegetable-curry-1-cup/56744c165c07ba8802092305
        baseFoods.put("Mixed Vegetable", Food("Mixed Vegetable", 188.0, 18.0))

        // https://www.nutritionix.com/food/paratha
        baseFoods.put("Paratha", Food("Paratha", 258.0, 10.0))

        // Dummy Food for friday
        baseFoods.put("Nothing", Food("Nothing", 0.0, 0.0))
    }

    private fun addBaseMeals(){
        baseMeals.add(arrayListOf(baseFoods["Khichuri"]))
        baseMeals.add(arrayListOf(baseFoods["Milk"], baseFoods["Bread"]))
        baseMeals.add(arrayListOf(baseFoods["Khichuri"], baseFoods["Egg"]))
        baseMeals.add(arrayListOf(baseFoods["Paratha"], baseFoods["Mixed Vegetable"]))
        baseMeals.add(arrayListOf(baseFoods["Paratha"], baseFoods["Egg"]))
        baseMeals.add(arrayListOf(baseFoods["Milk"], baseFoods["Biscuit"]))
        baseMeals.add(arrayListOf(baseFoods["Banana"], baseFoods["Biscuit"]))
        baseMeals.add(arrayListOf(baseFoods["Banana"], baseFoods["Bread"]))
        baseMeals.add(arrayListOf(baseFoods["Egg"], baseFoods["Bread"]))
        baseMeals.add(arrayListOf(baseFoods["Nothing"]))
    }

    fun getMealFromMealText(mealText: String) : Meal{
        var foods = arrayListOf<Food?>()
        var totalKcal = 0.0
        var totalCost = 0.0

        for(food in baseFoods){
            if(mealText.contains(food.key)){
                foods.add(food.value)
                totalKcal += food.value.kcal
                totalCost += food.value.cost
            }
        }

        return Meal(foods, totalKcal, totalCost)
    }

    fun getDummyMeal() : Meal{
        return Meal(arrayListOf(baseFoods["Nothing"]),
            baseFoods["Nothing"]!!.kcal,
            baseFoods["Nothing"]!!.cost)
    }
//    fun getDummyMeal() : Meal{
//        return Meal(arrayListOf(baseFoods["Khichuri"], baseFoods["Egg"]),
//            baseFoods["Khichuri"]!!.kcal + baseFoods["Egg"]!!.kcal,
//            baseFoods["Khichuri"]!!.cost + baseFoods["Egg"]!!.cost)
//    }

    fun checkBit(number: Int, position: Int) : Boolean{
        var num = number
        var pos = position
        while(pos > 0){
            num /= 2
            pos--
        }
        return (num%2 == 1)
    }

    fun generateMenuForEachDay(todaysBudget: Double) : Meal{
        var bestKcal = 0.0
        var bestMeal = getDummyMeal()

        for(meal in baseMeals){
            var mealCost = 0.0
            var mealKcal = 0.0

            for(food in meal){
                mealCost += food!!.cost
                mealKcal += food!!.kcal
            }

            if((mealCost <= todaysBudget) && (mealKcal >= bestKcal)){
                bestMeal = Meal(meal, mealKcal, mealCost)
                bestKcal = mealKcal
            }
        }

        return bestMeal
    }

    fun generateSpecialMeal(specialMealTexts : ArrayList<Pair<String, Int>>, todaysBudget: Double) : Meal{
        val specialMeals = ArrayList<Pair<Meal, Int>>()

        for(mealTextPair in specialMealTexts){
            specialMeals.add(Pair(getMealFromMealText(mealTextPair.first), mealTextPair.second))
        }

        specialMeals.sortByDescending { it.second }

        for(mealPair in specialMeals){
            val meal = mealPair.first

            if(meal.totalCost <= todaysBudget){
                return meal
            }
        }

        return generateMenuForEachDay(todaysBudget)
    }

    fun generateMenu(budget: Double, mealText: String) : ArrayList<Meal> {
        val specialMeal = getMealFromMealText(mealText)
        Log.e("budget", budget.toString())

        var thisWeeksBudget = budget

        thisWeeksBudget -= specialMeal.totalCost

        val numOfDay = 5
        var bestMeals: ArrayList<ArrayList<Food?>> = ArrayList<ArrayList<Food?>>()
        val size = baseMeals.size -1 // (-1 for skipping Friday)

        var bestKcal = 0.0

        for(mask in 1 until (1 shl size)){
            if(mask.countOneBits() != numOfDay)
                continue

            var mealCost = 0.0
            var mealKcal = 0.0

            var currentMeals = ArrayList<ArrayList<Food?>>()

            for(i in 0 until size){
                if(checkBit(mask, i)){
                    currentMeals.add(baseMeals[i])

                    for(food in baseMeals[i]){
                        mealCost += food!!.cost
                        mealKcal += food!!.kcal
                    }
                }
            }

            if((mealCost <= thisWeeksBudget) && (mealKcal >= bestKcal)){
                bestMeals = currentMeals
                bestKcal = mealKcal
            }
        }

        val bestMealsWithSpecial = getMeal(bestMeals)

        bestMealsWithSpecial.sortBy { it.totalKcal }
        if(bestMealsWithSpecial.contains(specialMeal)){
            val index = bestMealsWithSpecial.indexOf(specialMeal)
            Collections.swap(bestMealsWithSpecial, 2, index)
        }
        bestMealsWithSpecial.add(specialMeal)

        bestMealsWithSpecial.add(getDummyMeal())
        return bestMealsWithSpecial
    }

    fun getMeal(foodMenu: ArrayList<ArrayList<Food?>>) : ArrayList<Meal>{
        val meals = ArrayList<Meal>()

        for (meal in foodMenu){
            var totalCost = 0.0
            var totalKcal = 0.0

            for(food in meal){
                totalCost += food!!.cost
                totalKcal += food!!.kcal
            }

            meals.add(Meal(meal, totalKcal, totalCost))
        }

        return meals
    }

    fun getAvailableMeals() : ArrayList<Meal>{
        val baseMealsWithoutDummy = ArrayList<ArrayList<Food?>>()
        for(i in 0 until baseMeals.size-1){
            baseMealsWithoutDummy.add(baseMeals[i])
        }
        return getMeal(baseMealsWithoutDummy)
    }

}