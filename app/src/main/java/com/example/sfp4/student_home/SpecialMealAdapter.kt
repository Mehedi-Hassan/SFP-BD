package com.example.sfp4.student_home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.sfp4.R
import com.example.sfp4.food_menu.Meal

class SpecialMealAdapter(context: Context, mealList: ArrayList<Meal>) : ArrayAdapter<Meal>(context, 0, mealList) {
    private var layoutInflater = LayoutInflater.from(context)
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    private fun initView(position: Int, convertView: View?, parent: ViewGroup): View {
        var newConvertView: View?

        if(convertView == null){
            newConvertView = LayoutInflater.from(context).inflate(
                R.layout.meal_card_special_layout, parent, false
            )
        }
        else{
            newConvertView = convertView
        }

        val tvAvailableMealName = newConvertView!!.findViewById<TextView>(R.id.tvAvailableMealName)
        val tvAvailableKcal = newConvertView!!.findViewById<TextView>(R.id.tvAvailableKcal)
        val tvAvailableCost = newConvertView!!.findViewById<TextView>(R.id.tvAvailableCost)

        val meal = getItem(position)

        tvAvailableMealName.text = getMealtext(meal)
        tvAvailableKcal.text = String.format("%.2f", meal!!.totalKcal) + " kcal"
        tvAvailableCost.text = String.format("%.2f", meal!!.totalCost) + " Tk"

        return newConvertView
    }

    private fun getMealtext(curMeal: Meal?) : String{
        var mealText = ""
        var addedOneFood = false
        for(food in curMeal!!.items){
            if(addedOneFood) mealText += " and "
            mealText += food!!.name
            addedOneFood = true
        }
        return mealText
    }
}