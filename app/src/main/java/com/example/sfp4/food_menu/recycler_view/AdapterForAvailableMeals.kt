package com.example.sfp4.food_menu.recycler_view

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sfp4.R
import com.example.sfp4.food_menu.Meal
import com.squareup.picasso.Picasso

class AdapterForAvailableMeals(val mealList: ArrayList<Meal>) : RecyclerView.Adapter<AdapterForAvailableMeals.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAvailableMealName: TextView = itemView.findViewById(R.id.tvAvailableMealName)
        val tvAvailableKcal: TextView = itemView.findViewById(R.id.tvAvailableKcal)
        val tvAvailableCost: TextView = itemView.findViewById(R.id.tvAvailableCost)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.meal_card_available_layout, parent, false)
        return ViewHolder(view)
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val curMeal = mealList[position]

        holder.tvAvailableMealName.text = getMealtext(curMeal)
        holder.tvAvailableKcal.text = String.format("%.2f", curMeal!!.totalKcal) + " kcal"
        holder.tvAvailableCost.text = String.format("%.2f", curMeal!!.totalCost) + " Tk"

    }

    override fun getItemCount(): Int {
        return mealList.size
    }
}