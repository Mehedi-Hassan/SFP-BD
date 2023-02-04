package com.example.sfp4.food_menu

data class Meal(
    val items: ArrayList<Food?>,
    val totalKcal: Double,
    val totalCost: Double
)
