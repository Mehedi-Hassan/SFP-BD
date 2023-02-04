package com.example.sfp4.student_list.grid_view

import android.net.Uri

class ImageItem {
    private var uri: Uri

    constructor(uri: Uri){
        this.uri = uri
    }

    fun getUri() : Uri {
        return uri
    }
}