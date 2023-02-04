package com.example.sfp4.student_list.recycler_view

import java.io.File

data class Student(
    val roll: String,
    val name: String,
    var isPresent: Boolean,
    val imagesUri: File
)
