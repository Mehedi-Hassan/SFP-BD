package com.example.sfp4

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import com.example.sfp4.face_recognition.Prediction
import com.example.sfp4.food_menu.Food
import org.tensorflow.lite.task.vision.detector.Detection
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PrefConfig(private val context : Context, var target: String? = "onlyFace") {
    private val FILENAME_FORMAT = "yyyy-MM-dd"
    private var curDate: String


    init {
        curDate = SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
            .format(System.currentTimeMillis())
        curDate = target + " " + curDate

        Log.e(context.toString()+ "PrefConfig", curDate)
    }

    fun writePredictions(predictions: ArrayList<Prediction>){
        val prefs: SharedPreferences = context.getSharedPreferences(curDate, MODE_PRIVATE)
        var savedSet = prefs.getStringSet(curDate, null)

        for(prediction in predictions){
            if (savedSet != null) {
                savedSet.add(prediction.label)
            }
            else{
                savedSet = mutableSetOf(prediction.label)
            }
        }


        val editor : SharedPreferences.Editor = prefs.edit()
        editor.clear()
        editor.putStringSet(curDate, savedSet)
        editor.apply()
    }

    fun readPredictions() : ArrayList<String>{
        val prefs: SharedPreferences = context.getSharedPreferences(curDate, MODE_PRIVATE)
        val savedSet = prefs.getStringSet(curDate, null)

        var predictions:ArrayList<String> = ArrayList()

        if (savedSet != null) {
            for(item in savedSet){
                predictions.add(item)
            }
        }
        return predictions
    }

    fun writeStudentsWhoGotFood(studentName: String){
        val prefs: SharedPreferences = context.getSharedPreferences(curDate, MODE_PRIVATE)
        var savedSet = prefs.getStringSet(curDate, null)

        if (savedSet != null) {
            savedSet.add(studentName)
        }
        else{
            savedSet = mutableSetOf(studentName)
        }

        val editor : SharedPreferences.Editor = prefs.edit()
        editor.clear()
        editor.putStringSet(curDate, savedSet)
        editor.apply()
    }

    fun readStudentsWhoGotFood() : ArrayList<String>{
        val prefs: SharedPreferences = context.getSharedPreferences(curDate, MODE_PRIVATE)
        val savedSet = prefs.getStringSet(curDate, null)

        var studentsWhoGotFood:ArrayList<String> = ArrayList()

        if (savedSet != null) {
            for(item in savedSet){
                studentsWhoGotFood.add(item)
            }
        }
        return studentsWhoGotFood
    }

    fun clearPredictions(){
        val prefs: SharedPreferences = context.getSharedPreferences(curDate, MODE_PRIVATE)

        val editor : SharedPreferences.Editor = prefs.edit()
        editor.clear()
        editor.apply()
    }

    fun writeTodayFood(meal: ArrayList<Food?>){
        val prefs: SharedPreferences = context.getSharedPreferences(curDate, MODE_PRIVATE)
        var savedSet = prefs.getStringSet(curDate, null)

        for(food in meal){
            if(savedSet != null){
                savedSet.add(food!!.name)
            }
            else{
                savedSet = mutableSetOf(food!!.name)
            }
        }

        val editor : SharedPreferences.Editor = prefs.edit()
        editor.clear()
        editor.putStringSet(curDate, savedSet)
        editor.apply()
    }

    fun readTodayFood() : ArrayList<String>{
        val prefs: SharedPreferences = context.getSharedPreferences(curDate, MODE_PRIVATE)
        val savedSet = prefs.getStringSet(curDate, null)

        var todayFoods = ArrayList<String>()

        if (savedSet != null) {
            for(item in savedSet){
                todayFoods.add(item)
            }
        }
        return todayFoods
    }

    fun writeFoodPredictions(predictions: MutableList<Detection>?){
        val foodCurDate = "food " + curDate
        val prefs: SharedPreferences = context.getSharedPreferences(foodCurDate, MODE_PRIVATE)
        var savedSet = prefs.getStringSet(foodCurDate, null)

        if (predictions != null) {
            for(prediction in predictions){
                if (savedSet != null) {
                    savedSet.add(prediction.categories[0].label)
                }
                else{
                    savedSet = mutableSetOf(prediction.categories[0].label)
                }
            }
        }

        val editor : SharedPreferences.Editor = prefs.edit()
        editor.clear()
        editor.putStringSet(curDate, savedSet)
        editor.apply()
    }

    fun readFoodPredictions() : ArrayList<String>{
        val foodCurDate = "food " + curDate
        val prefs: SharedPreferences = context.getSharedPreferences(foodCurDate, MODE_PRIVATE)
        val savedSet = prefs.getStringSet(foodCurDate, null)

        var predictions:ArrayList<String> = ArrayList()

        if (savedSet != null) {
            for(item in savedSet){
                predictions.add(item)
            }
        }
        return predictions
    }
}