package com.example.sfp4.face_and_food.recycler_view

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sfp4.R
import com.example.sfp4.student_list.recycler_view.Student
import com.squareup.picasso.Picasso

class StudentAdapterForFood(val studentList: ArrayList<StudentForFood>) : RecyclerView.Adapter<StudentAdapterForFood.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gotFoodStudentDp: ImageView = itemView.findViewById(R.id.gotFoodStudentDp)
        val gotFoodStudentName: TextView = itemView.findViewById(R.id.gotFoodStudentName)
        val gotFoodStudentRoll: TextView = itemView.findViewById(R.id.gotFoodStudentRoll)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.student_got_food_card_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val curStudent = studentList[position]
        holder.gotFoodStudentRoll.text = "Roll: " + curStudent.roll
        holder.gotFoodStudentName.text = curStudent.name

        val imageSubDir = curStudent.imagesUri
        val contents = imageSubDir.listFiles()


        var studentDpUri: Uri
        if(contents.isNotEmpty()){
            studentDpUri = Uri.fromFile(contents[0])
            Picasso.get().load(studentDpUri).into(holder.gotFoodStudentDp)
        }
        else{
            Picasso.get().load(R.drawable.ic_profile).into(holder.gotFoodStudentDp)
        }
    }

    override fun getItemCount(): Int {
        return studentList.size
    }
}