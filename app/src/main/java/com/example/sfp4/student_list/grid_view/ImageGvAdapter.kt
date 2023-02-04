package com.example.sfp4.student_list.grid_view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.example.sfp4.R
import com.squareup.picasso.Picasso

class ImageGvAdapter(var context: Context, var arrayList: ArrayList<ImageItem>) : BaseAdapter(){
    override fun getItem(position: Int): ImageItem {
        return arrayList.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return arrayList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        var view: View = View.inflate(context, R.layout.grid_item, null)

        var gridImage: ImageView = view.findViewById(R.id.gridImage)
        var imageItem: ImageItem = arrayList.get(position)
        Picasso.get().load(imageItem.getUri()).into(gridImage)

        return view
    }
}