package com.example.sfp4.student_list.recycler_view

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sfp4.R
import com.squareup.picasso.Picasso
import org.checkerframework.checker.optional.qual.Present

class StudentAdapter(val studentList: ArrayList<Student>, val listener: OnItemClickListener) : RecyclerView.Adapter<StudentAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val studentDp: ImageView = itemView.findViewById(R.id.studentDp)
        val studentName: TextView = itemView.findViewById(R.id.studentName)
        val studentRoll: TextView = itemView.findViewById(R.id.studentRoll)
        var isStudentPresent: TextView = itemView.findViewById(R.id.isStudentPresent)
    }

    interface OnItemClickListener {
        fun onItemClicked(student: Student)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.student_card_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val curStudent = studentList[position]
        holder.studentRoll.text = "Roll: " + curStudent.roll
        holder.studentName.text = curStudent.name

        if(curStudent.isPresent){
            holder.isStudentPresent.text = "Present"
            holder.isStudentPresent.setTextColor(Color.parseColor("#00C853"))
        }
        else{
            holder.isStudentPresent.text = "Absent"
            holder.isStudentPresent.setTextColor(Color.parseColor("#D50000"))
        }

        val imageSubDir = curStudent.imagesUri
        val contents = imageSubDir.listFiles()


        var studentDpUri: Uri
        if(contents.isNotEmpty()){
            studentDpUri = Uri.fromFile(contents[0])
            Picasso.get().load(studentDpUri).into(holder.studentDp)
        }
        else{
            Picasso.get().load(R.drawable.ic_profile).into(holder.studentDp)
        }

        holder.itemView.setOnClickListener{
            listener.onItemClicked(curStudent)
        }
    }

    override fun getItemCount(): Int {
        return studentList.size
    }


}